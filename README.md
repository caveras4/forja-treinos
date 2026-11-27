# Forja Treinos

App Android de academia **100% offline**. Não usa internet e não tem a permissão `INTERNET`.
Você monta seus treinos a partir de um catálogo com 175 exercícios animados, registra a carga
atual de cada um e acompanha o treino do dia.

## Funcionalidades

- **Criar treino.** A lista de exercícios vem separada por grupo muscular, com busca e filtro.
  - Tocar no nome expande o card com a animação e o botão "Adicionar ao treino".
  - O exercício adicionado fica verde. Tocar de novo no nome recolhe o card.
  - **Concluir** só habilita com pelo menos um exercício selecionado.
  - Depois de concluir, você define séries, repetições e carga (opcional), além do nome, do foco e da cor do treino.
- **Iniciar treino.** O app mostra a lista de exercícios. Tocar em um mostra a animação, as séries,
  as repetições e a carga. Dá para marcar o exercício como feito e alterar a carga com `−`/`+` ou digitando.
  **Não há histórico:** a carga nova substitui a anterior.
- **Treino em andamento.** Tocar no card só abre o treino para ver. Para começar, toque no **▶** do card
  na Home ou em **Iniciar** dentro do treino (no lugar onde fica o Finalizar). Os quadrados de marcar feito só
  aparecem depois de iniciar, e o progresso é salvo a cada marcação.
  Voltar para a Home ou fechar o app não perde nada. Só existe **um treino em andamento por vez**:
  ao iniciar outro, o app pergunta se você quer **Continuar treino** (o atual) ou **Encerrar e iniciar novo**.
  Na Home, o treino em andamento aparece numa faixa fixa no topo, de ponta a ponta da tela, com borda
  animada, ponto pulsando e barra de progresso. O card dele ocupa a linha inteira, com borda verde pulsando,
  etiqueta "EM ANDAMENTO" e barra de progresso.
  **Finalizar** encerra o treino. Se faltar exercício, um aviso no mesmo estilo do de troca mostra o
  progresso e oferece **Continuar treino** ou **Finalizar mesmo assim**. As cargas continuam salvas.
- **Editar treino** (segure o card na Home):
  - reordenar com toque longo e arrastar;
  - remover e adicionar exercícios;
  - mudar séries, reps e carga;
  - renomear e trocar a cor.
  Nada é gravado até tocar em **Salvar**, e sair sem salvar pede confirmação.
- **Exercício personalizado.** Quando um exercício não está no catálogo, você cria o seu com imagem
  ou GIF próprio (GIF, WebP, PNG ou JPG, **até 2 MB e até 720 px**). O formato é validado pelo
  conteúdo do arquivo, não pela extensão.
- **Importar/exportar treino em JSON** (formato abaixo).
- **Backup completo em `.zip`**, com os treinos, os exercícios personalizados e as imagens deles.
- **Aparência:** tema claro, escuro ou automático, e 4 paletas de cor. Layout adaptado para tablet.

## Formato JSON de treino (v1)

```json
{
  "formato": "forja-treino",
  "versao": 1,
  "treino": {
    "nome": "Treino A",
    "foco": "Peito e Tríceps",
    "cor": "emerald",
    "exercicios": [
      { "id": "supino-reto-barra", "series": 4, "repeticoes": 10, "cargaKg": 40 },
      { "id": "triceps-corda", "series": 3, "repeticoes": 12 }
    ]
  }
}
```

| Campo | Regra |
|---|---|
| `formato` / `versao` | precisam ser `"forja-treino"` e `1` |
| `nome` | de 2 a 48 caracteres. Se já existir um treino com o mesmo nome, ganha o sufixo " (2)" |
| `foco` | opcional, até 80 caracteres |
| `cor` | `emerald`, `cobalt`, `amber` ou `crimson`. Outro valor vira `emerald` |
| `exercicios` | de 1 a 30, sem repetir `id` |
| `id` | id do catálogo (veja `app/src/main/assets/exercises.json`). Um id desconhecido é listado e o usuário decide se importa sem ele |
| `series` / `repeticoes` | de 1 a 10 / de 1 a 100 |
| `cargaKg` | opcional, de 0 a 500 |

O arquivo pode ter no máximo 256 KB. Há um exemplo completo em [`docs/exemplo-treino.json`](docs/exemplo-treino.json).

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · Navigation Compose · kotlinx.serialization ·
Coil (GIF/WebP animado, só arquivos locais) · [Reorderable](https://github.com/Calvin-LL/Reorderable).

A arquitetura é MVVM, com injeção de dependência manual (`AppContainer`):

```
ui/        telas Compose + ViewModels (home, editor, session)
domain/    regras puras, sem Android: rascunho do treino, validações, JSON e backup
data/      Room (entities, DAOs, migration), repositories, mídia própria, backup .zip
```

## Como rodar

Pré-requisitos: JDK 17 ou superior e Android SDK 35 (pelo Android Studio ou pelas command-line tools).

```bash
./gradlew assembleDebug            # APK em app/build/outputs/apk/debug/
./gradlew testDebugUnitTest        # testes de regra (JVM)
./gradlew connectedDebugAndroidTest  # banco, migration, catálogo e telas (precisa de emulador/aparelho)
```

Para usar no celular, prefira o APK de release (`./gradlew assembleRelease`, em
`app/build/outputs/apk/release/`): o Compose em debug é bem mais lento. O release é assinado com a chave
de debug da máquina, então instala por cima do APK de debug sem perder os dados (desde que os dois
tenham sido gerados no mesmo computador). Para publicar numa loja, troque por uma keystore própria.

## Catálogo de exercícios

As imagens e a lista base vêm do [free-exercise-db](https://github.com/yuhonas/free-exercise-db),
em domínio público (Unlicense). A curadoria em PT-BR fica em `tools/catalogo/selecao.csv`.
O script junta as 2 fotos de cada exercício num WebP animado. Ele roda só no desenvolvimento,
porque o app nunca baixa nada.

```bash
py -m venv tools/catalogo/.venv
tools/catalogo/.venv/Scripts/pip install pillow
tools/catalogo/.venv/Scripts/python tools/catalogo/gerar_catalogo.py
```

## Origem

A base foi gerada no Google AI Studio. O protótipo web original está em `docs/prototipo-web/`,
como referência visual.
