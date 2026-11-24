package com.fitforge.treinos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveSessionTest {

    @Test
    fun `sem treino em andamento comeca um novo`() {
        assertEquals(StartDecision.StartNew, decideStart(null, 7))
    }

    @Test
    fun `tocar no proprio treino em andamento continua`() {
        assertEquals(StartDecision.Resume, decideStart(ActiveSession(7), 7))
    }

    @Test
    fun `tocar em outro treino gera conflito com o ativo`() {
        assertEquals(StartDecision.Conflict(7), decideStart(ActiveSession(7), 9))
    }

    @Test
    fun `marcar e desmarcar feito`() {
        var s = ActiveSession(1)
        s = s.toggle("leg-press").toggle("prancha")
        assertTrue(s.isDone("leg-press"))
        s = s.toggle("leg-press")
        assertFalse(s.isDone("leg-press"))
        assertEquals(setOf("prancha"), s.doneExerciseIds)
    }

    @Test
    fun `editar o treino descarta marcacoes de exercicios removidos`() {
        val s = ActiveSession(1, setOf("leg-press", "prancha", "face-pull"))
        assertEquals(setOf("prancha"), s.retain(listOf("prancha", "remada-baixa")).doneExerciseIds)
    }

    @Test
    fun `contagem considera so exercicios que ainda estao no treino`() {
        val s = ActiveSession(1, setOf("leg-press", "removido"))
        assertEquals(1, s.doneCount(listOf("leg-press", "prancha")))
    }
}
