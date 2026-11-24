package com.fitforge.treinos.ui.editor

import com.fitforge.treinos.data.local.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseGroupsTest {

    private fun ex(id: String, name: String, group: String) = ExerciseEntity(id, name, group, null, false)

    private val all = listOf(
        ex("1", "Tríceps corda", "Tríceps"),
        ex("2", "Supino reto", "Peito"),
        ex("3", "Agachamento", "Quadríceps"),
        ex("4", "Crucifixo", "Peito"),
        ex("5", "Abdominal", "Abdômen")
    )

    @Test
    fun `grupos seguem a ordem oficial e exercicios ficam em ordem alfabetica`() {
        val groups = buildGroups(all, "", null)
        assertEquals(listOf("Peito", "Tríceps", "Quadríceps", "Abdômen"), groups.map { it.name })
        assertEquals(listOf("Crucifixo", "Supino reto"), groups[0].exercises.map { it.name })
    }

    @Test
    fun `busca ignora acento e caixa`() {
        val groups = buildGroups(all, "TRICEPS", null)
        assertEquals(listOf("Tríceps corda"), groups.flatMap { it.exercises }.map { it.name })
    }

    @Test
    fun `filtro por grupo e busca sem resultado`() {
        assertEquals(listOf("Peito"), buildGroups(all, "", "Peito").map { it.name })
        assertTrue(buildGroups(all, "zzz", null).isEmpty())
    }
}
