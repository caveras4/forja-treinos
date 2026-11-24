package com.fitforge.treinos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutRulesTest {

    @Test
    fun `nome precisa ter entre 2 e 48 caracteres depois do trim`() {
        assertNotNull(WorkoutRules.validateName("   "))
        assertNotNull(WorkoutRules.validateName(" A "))
        assertNull(WorkoutRules.validateName(" AB "))
        assertNull(WorkoutRules.validateName("x".repeat(48)))
        assertNotNull(WorkoutRules.validateName("x".repeat(49)))
    }

    @Test
    fun `quantidade de exercicios entre 1 e 30`() {
        assertNotNull(WorkoutRules.validateExerciseCount(0))
        assertNull(WorkoutRules.validateExerciseCount(1))
        assertNull(WorkoutRules.validateExerciseCount(30))
        assertNotNull(WorkoutRules.validateExerciseCount(31))
    }

    @Test
    fun `carga aceita virgula e ponto e vazio vira sem carga`() {
        assertEquals(WeightInput.Value(12.5), WeightFormat.parse("12,5"))
        assertEquals(WeightInput.Value(12.5), WeightFormat.parse(" 12.5 "))
        assertEquals(WeightInput.Empty, WeightFormat.parse(""))
        assertEquals(WeightInput.Invalid, WeightFormat.parse("abc"))
        assertEquals(WeightInput.Invalid, WeightFormat.parse("-1"))
        assertEquals(WeightInput.Invalid, WeightFormat.parse("500,5"))
        assertEquals(WeightInput.Value(500.0), WeightFormat.parse("500"))
    }

    @Test
    fun `exibicao da carga`() {
        assertEquals("40 kg", WeightFormat.display(40.0))
        assertEquals("12,5 kg", WeightFormat.display(12.5))
        assertEquals("Sem carga", WeightFormat.display(null))
        assertEquals("Sem carga", WeightFormat.display(0.0))
        assertEquals("12,5", WeightFormat.toInput(12.5))
        assertEquals("", WeightFormat.toInput(null))
    }

    @Test
    fun `passo de carga nao fica negativo e zero vira sem carga`() {
        assertEquals(1.0, WeightFormat.step(null, 1.0))
        assertNull(WeightFormat.step(1.0, -1.0))
        assertNull(WeightFormat.step(0.5, -1.0))
        assertEquals(500.0, WeightFormat.step(500.0, 1.0))
        assertEquals(41.0, WeightFormat.step(40.0, 1.0))
    }

    @Test
    fun `sugestao de foco junta grupos distintos na ordem`() {
        assertEquals("", MuscleGroups.suggestFocus(emptyList()))
        assertEquals("Peito", MuscleGroups.suggestFocus(listOf("Peito", "Peito")))
        assertEquals("Peito e Tríceps", MuscleGroups.suggestFocus(listOf("Peito", "Tríceps", "Peito")))
        assertEquals("Costas, Bíceps e Abdômen", MuscleGroups.suggestFocus(listOf("Costas", "Bíceps", "Abdômen")))
    }

    @Test
    fun `nome duplicado ganha sufixo numerico`() {
        assertEquals("Treino A", uniqueName("Treino A", listOf("Treino B")))
        assertEquals("Treino A (2)", uniqueName("Treino A", listOf("treino a")))
        assertEquals("Treino A (3)", uniqueName("Treino A", listOf("Treino A", "Treino A (2)")))
        val long = "x".repeat(48)
        val result = uniqueName(long, listOf(long))
        assertEquals(48, result.length)
        assert(result.endsWith(" (2)"))
    }
}
