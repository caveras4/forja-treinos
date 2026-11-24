"""Gera o catálogo de exercícios embutido no APK.

Lê `selecao.csv` (curadoria em PT-BR), baixa as 2 fotos de cada exercício do
free-exercise-db (https://github.com/yuhonas/free-exercise-db, licença Unlicense)
e gera um WebP animado por exercício, alternando as duas fotos.

Saída:
  app/src/main/assets/exercises/<id>.webp
  app/src/main/assets/exercises.json

Roda só no desenvolvimento; o app nunca acessa a rede.

Uso:
  py -m venv tools/catalogo/.venv
  tools/catalogo/.venv/Scripts/pip install pillow
  tools/catalogo/.venv/Scripts/python tools/catalogo/gerar_catalogo.py
"""

from __future__ import annotations

import csv
import io
import json
import sys
import urllib.request
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
PASTA_TOOL = Path(__file__).resolve().parent
CACHE = PASTA_TOOL / ".cache"
ASSETS = RAIZ / "app" / "src" / "main" / "assets"
SAIDA_MIDIA = ASSETS / "exercises"
BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main"

LADO_MAX = 480
QUALIDADE = 70
DURACAO_QUADRO_MS = 900

GRUPOS_VALIDOS = [
    "Peito", "Costas", "Ombros", "Bíceps", "Tríceps", "Antebraço",
    "Quadríceps", "Posterior", "Glúteos", "Panturrilha", "Abdômen",
]


def baixar(url: str, destino: Path) -> bytes:
    if destino.exists():
        return destino.read_bytes()
    destino.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": "forja-treinos-catalogo"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        dados = resp.read()
    destino.write_bytes(dados)
    return dados


def preparar_quadro(dados: bytes) -> Image.Image:
    img = Image.open(io.BytesIO(dados)).convert("RGB")
    img.thumbnail((LADO_MAX, LADO_MAX), Image.LANCZOS)
    return img


def main() -> int:
    banco = json.loads(
        baixar(f"{BASE_URL}/dist/exercises.json", CACHE / "exercises.json").decode("utf-8")
    )
    por_id = {e["id"]: e for e in banco}

    with open(PASTA_TOOL / "selecao.csv", encoding="utf-8", newline="") as f:
        linhas = list(csv.DictReader(f))

    erros = []
    vistos = set()
    for linha in linhas:
        if linha["id_origem"] not in por_id:
            erros.append(f"id_origem inexistente: {linha['id_origem']}")
        if linha["grupo"] not in GRUPOS_VALIDOS:
            erros.append(f"grupo inválido em {linha['id']}: {linha['grupo']}")
        if linha["id"] in vistos:
            erros.append(f"id duplicado: {linha['id']}")
        vistos.add(linha["id"])
    if erros:
        print("\n".join(erros), file=sys.stderr)
        return 1

    SAIDA_MIDIA.mkdir(parents=True, exist_ok=True)
    catalogo = []
    total = 0
    for i, linha in enumerate(linhas, start=1):
        origem = por_id[linha["id_origem"]]
        quadros = []
        for caminho in origem["images"][:2]:
            dados = baixar(f"{BASE_URL}/exercises/{caminho}", CACHE / "img" / caminho)
            quadros.append(preparar_quadro(dados))

        # Os quadros precisam ter o mesmo tamanho para animar sem "pulo".
        largura = min(q.width for q in quadros)
        altura = min(q.height for q in quadros)
        quadros = [q.resize((largura, altura), Image.LANCZOS) for q in quadros]

        destino = SAIDA_MIDIA / f"{linha['id']}.webp"
        quadros[0].save(
            destino,
            format="WEBP",
            save_all=True,
            append_images=quadros[1:],
            duration=DURACAO_QUADRO_MS,
            loop=0,
            quality=QUALIDADE,
            method=6,
        )
        total += destino.stat().st_size
        catalogo.append({
            "id": linha["id"],
            "nome": linha["nome_pt"],
            "grupo": linha["grupo"],
            "midia": f"exercises/{linha['id']}.webp",
        })
        print(f"[{i}/{len(linhas)}] {linha['id']}", flush=True)

    (ASSETS / "exercises.json").write_text(
        json.dumps({"fonte": "free-exercise-db (Unlicense)", "exercicios": catalogo},
                   ensure_ascii=False, indent=1),
        encoding="utf-8",
    )
    print(f"\n{len(catalogo)} exercícios, mídia total {total / 1024 / 1024:.1f} MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
