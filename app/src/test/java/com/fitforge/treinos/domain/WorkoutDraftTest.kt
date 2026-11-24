package com.fitforge.treinos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDraftTest {

    private fun ids(draft: WorkoutDraft) = draft.items.map { it.exerciseId }

    @Test
    fun `marcar adiciona no fim e marcar de novo desmarca`() {
        var draft = WorkoutDraft()
        draft = draft.toggle("a").toggle("b").toggle("c")
        assertEquals(listOf("a", "b", "c"), ids(draft))
        draft = draft.toggle("b")
        assertEquals(listOf("a", "c"), ids(draft))
        assertFalse(draft.isSelected("b"))
        draft = draft.toggle("b")
        assertEquals(listOf("a", "c", "b"), ids(draft))
    }

    @Test
    fun `mesmo exercicio nunca entra duas vezes`() {
        val draft = WorkoutDraft().toggle("a").toggle("a").toggle("a")
        assertEquals(listOf("a"), ids(draft))
    }

    @Test
    fun `novo item vem com o padrao 3x12 e carga vazia`() {
        val item = WorkoutDraft().toggle("a").items.single()
        assertEquals(3, item.sets)
        assertEquals(12, item.reps)
        assertEquals("", item.weightText)
    }

    @Test
    fun `mover reordena como arrastar e soltar`() {
        val draft = WorkoutDraft().toggle("a").toggle("b").toggle("c").toggle("d")
        assertEquals(listOf("b", "c", "a", "d"), ids(draft.move("a", "c")))
        assertEquals(listOf("d", "a", "b", "c"), ids(draft.move("d", "a")))
        assertSame(draft, draft.move("a", "inexistente"))
        assertSame(draft, draft.move("a", "a"))
    }

    @Test
    fun `series e reps ficam dentro dos limites`() {
        var draft = WorkoutDraft().toggle("a")
        draft = draft.setSets("a", 0).setReps("a", 500)
        assertEquals(1, draft.items[0].sets)
        assertEquals(100, draft.items[0].reps)
        draft = draft.setSets("a", 11)
        assertEquals(10, draft.items[0].sets)
    }

    @Test
    fun `validacao aponta nome, lista vazia e carga invalida`() {
        val empty = WorkoutDraft(name = "")
        val errors = empty.validate()
        assertNotNull(errors.name)
        assertNotNull(errors.exercises)

        val badWeight = WorkoutDraft(name = "Treino").toggle("a").setWeightText("a", "abc")
        val e2 = badWeight.validate()
        assertTrue(e2.items.containsKey("a"))
        assertFalse(e2.isEmpty)

        val ok = WorkoutDraft(name = "Treino").toggle("a").setWeightText("a", "20,5")
        assertTrue(ok.validate().isEmpty)
    }

    @Test
    fun `retainExisting remove exercicios apagados`() {
        val draft = WorkoutDraft().toggle("a").toggle("custom-x").toggle("b")
        assertEquals(listOf("a", "b"), ids(draft.retainExisting(setOf("a", "b"))))
    }
}
