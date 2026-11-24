package com.fitforge.treinos.domain.transfer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Os treinos de exemplo em docs/ precisam importar sem nenhum exercício desconhecido.
 * Se o catálogo mudar e um id sumir, este teste avisa.
 */
class ExampleWorkoutsTest {

    // Testes unitários rodam com o diretório do módulo (app/) como pasta atual.
    private val root = File("..").canonicalFile

    private val catalogIds: Set<String> by lazy {
        val text = File(root, "app/src/main/assets/exercises.json").readText()
        Json.parseToJsonElement(text).jsonObject["exercicios"]!!.jsonArray
            .map { it.jsonObject["id"]!!.jsonPrimitive.content }
            .toSet()
    }

    private fun examples(): List<File> =
        (File(root, "docs/treinos").listFiles { f -> f.extension == "json" }.orEmpty().toList() +
            File(root, "docs/exemplo-treino.json")).sortedBy { it.name }

    @Test
    fun `todos os treinos de exemplo importam completos`() {
        val files = examples()
        assertEquals(7, files.size)
        for (file in files) {
            val result = WorkoutImportParser.parse(file.readText(), file.length(), catalogIds, emptyList())
            assertTrue("${file.name}: $result", result is ImportResult.Parsed)
            result as ImportResult.Parsed
            assertTrue("${file.name}: desconhecidos ${result.unknownIds}", result.unknownIds.isEmpty())
            assertEquals(file.name, result.totalInFile, result.draft.items.size)
        }
    }
}
