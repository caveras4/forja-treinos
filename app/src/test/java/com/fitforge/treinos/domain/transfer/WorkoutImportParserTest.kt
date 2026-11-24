package com.fitforge.treinos.domain.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutImportParserTest {

    private val known = setOf("supino-reto-barra", "triceps-corda", "crucifixo-reto")

    private fun parse(json: String, existing: List<String> = emptyList()) =
        WorkoutImportParser.parse(json, json.length.toLong(), known, existing)

    private fun error(result: ImportResult): String {
        assertTrue("esperava erro, veio $result", result is ImportResult.Error)
        return (result as ImportResult.Error).message
    }

    private val valid = """
        {
          "formato": "forja-treino",
          "versao": 1,
          "treino": {
            "nome": "Treino A",
            "foco": "Peito e Tríceps",
            "cor": "cobalt",
            "exercicios": [
              { "id": "supino-reto-barra", "series": 4, "repeticoes": 10, "cargaKg": 40 },
              { "id": "triceps-corda", "series": 3, "repeticoes": 12 }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `arquivo valido vira rascunho completo`() {
        val result = parse(valid) as ImportResult.Parsed
        assertEquals("Treino A", result.draft.name)
        assertEquals("Peito e Tríceps", result.draft.focus)
        assertEquals("cobalt", result.draft.colorKey)
        assertEquals(listOf("supino-reto-barra", "triceps-corda"), result.draft.items.map { it.exerciseId })
        assertEquals("40", result.draft.items[0].weightText)
        assertEquals("", result.draft.items[1].weightText)
        assertEquals(4, result.draft.items[0].sets)
        assertTrue(result.unknownIds.isEmpty())
    }

    @Test
    fun `id desconhecido vai para o relatorio e fica fora do rascunho`() {
        val json = valid.replace("triceps-corda", "exercicio-que-nao-existe")
        val result = parse(json) as ImportResult.Parsed
        assertEquals(2, result.totalInFile)
        assertEquals(listOf("exercicio-que-nao-existe"), result.unknownIds)
        assertEquals(1, result.draft.items.size)
    }

    @Test
    fun `todos desconhecidos e erro`() {
        val json = valid.replace("supino-reto-barra", "x1").replace("triceps-corda", "x2")
        assertTrue(error(parse(json)).contains("Nenhum exercício"))
    }

    @Test
    fun `nome duplicado ganha sufixo`() {
        val result = parse(valid, existing = listOf("Treino A")) as ImportResult.Parsed
        assertEquals("Treino A (2)", result.draft.name)
    }

    @Test
    fun `formato e versao errados`() {
        assertTrue(error(parse(valid.replace("forja-treino", "outro-app"))).contains("não reconhecido"))
        assertTrue(error(parse(valid.replace("\"versao\": 1", "\"versao\": 2"))).contains("Versão 2"))
    }

    @Test
    fun `json malformado ou campo obrigatorio ausente`() {
        error(parse("{ isto não é json"))
        error(parse("""{"formato":"forja-treino","versao":1}"""))
        error(parse(""))
    }

    @Test
    fun `limites de series, repeticoes e carga`() {
        assertTrue(error(parse(valid.replace("\"series\": 4", "\"series\": 11"))).contains("séries"))
        assertTrue(error(parse(valid.replace("\"repeticoes\": 10", "\"repeticoes\": 0"))).contains("repetições"))
        assertTrue(error(parse(valid.replace("\"cargaKg\": 40", "\"cargaKg\": 501"))).contains("carga"))
        assertTrue(error(parse(valid.replace("\"cargaKg\": 40", "\"cargaKg\": -5"))).contains("carga"))
    }

    @Test
    fun `nome invalido e exercicio repetido`() {
        error(parse(valid.replace("\"Treino A\"", "\"A\"")))
        val dup = valid.replace("triceps-corda", "supino-reto-barra")
        assertTrue(error(parse(dup)).contains("mais de uma vez"))
    }

    @Test
    fun `lista vazia e lista com mais de 30`() {
        val empty = """{"formato":"forja-treino","versao":1,"treino":{"nome":"Treino","exercicios":[]}}"""
        error(parse(empty))
        val many = (1..31).joinToString(",") { """{"id":"e$it","series":3,"repeticoes":10}""" }
        error(parse("""{"formato":"forja-treino","versao":1,"treino":{"nome":"Treino","exercicios":[$many]}}"""))
    }

    @Test
    fun `arquivo acima de 256 KB e recusado antes de ler`() {
        val result = WorkoutImportParser.parse(valid, 256L * 1024 + 1, known, emptyList())
        assertTrue(error(result).contains("256 KB"))
    }

    @Test
    fun `cor desconhecida volta para o padrao e campos extras sao ignorados`() {
        val json = valid.replace("\"cobalt\"", "\"roxo\"").replace("\"versao\": 1,", "\"versao\": 1, \"extra\": true,")
        val result = parse(json) as ImportResult.Parsed
        assertEquals("emerald", result.draft.colorKey)
    }

    @Test
    fun `exportar e importar de volta preserva o treino`() {
        val json = WorkoutImportParser.export(
            "Treino X", "Peito", "amber",
            listOf(WorkoutExerciseJson("crucifixo-reto", 3, 15, 12.5))
        )
        val result = parse(json) as ImportResult.Parsed
        assertEquals("Treino X", result.draft.name)
        assertEquals("amber", result.draft.colorKey)
        assertEquals("12,5", result.draft.items.single().weightText)
        assertEquals(15, result.draft.items.single().reps)
    }
}
