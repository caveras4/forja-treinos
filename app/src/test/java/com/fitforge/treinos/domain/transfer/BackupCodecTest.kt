package com.fitforge.treinos.domain.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val catalog = setOf("supino-reto-barra", "leg-press")

    private fun backup(
        customs: List<CustomExerciseJson> = emptyList(),
        treinos: List<WorkoutJson> = emptyList(),
        formato: String = TransferFormat.BACKUP_FORMAT
    ) = BackupCodec.encode(BackupFile(formato, 1, customs, treinos))

    private val custom = CustomExerciseJson("custom-1", "Meu exercício", "Peito", "custom-1.gif")
    private val treino = WorkoutJson(
        nome = "Treino A",
        exercicios = listOf(
            WorkoutExerciseJson("supino-reto-barra", 4, 10, 40.0),
            WorkoutExerciseJson("custom-1", 3, 12)
        )
    )

    @Test
    fun `backup valido com exercicio personalizado e midia`() {
        val result = BackupCodec.decode(backup(listOf(custom), listOf(treino)), catalog, setOf("custom-1.gif"))
        val content = (result as BackupDecodeResult.Ok).content
        assertEquals(1, content.customExercises.size)
        assertEquals(1, content.workouts.size)
        assertEquals(listOf("supino-reto-barra", "custom-1"), content.workouts[0].items.map { it.exerciseId })
        assertTrue(content.skippedExerciseIds.isEmpty())
    }

    @Test
    fun `midia citada mas ausente no zip e erro`() {
        val result = BackupCodec.decode(backup(listOf(custom), listOf(treino)), catalog, emptySet())
        assertTrue(result is BackupDecodeResult.Error)
    }

    @Test
    fun `nome de midia com caminho e recusado`() {
        val evil = custom.copy(midia = "../../databases/x.gif")
        val result = BackupCodec.decode(backup(listOf(evil)), catalog, setOf("../../databases/x.gif"))
        assertTrue(result is BackupDecodeResult.Error)
    }

    @Test
    fun `id personalizado sem prefixo ou igual ao catalogo e recusado`() {
        val r1 = BackupCodec.decode(backup(listOf(custom.copy(id = "leg-press", midia = null))), catalog, emptySet())
        assertTrue(r1 is BackupDecodeResult.Error)
        val r2 = BackupCodec.decode(backup(listOf(custom.copy(id = "abc", midia = null))), catalog, emptySet())
        assertTrue(r2 is BackupDecodeResult.Error)
    }

    @Test
    fun `grupo muscular desconhecido e recusado`() {
        val r = BackupCodec.decode(backup(listOf(custom.copy(grupo = "Pescoço", midia = null))), catalog, emptySet())
        assertTrue(r is BackupDecodeResult.Error)
    }

    @Test
    fun `exercicio desconhecido e ignorado e reportado`() {
        val t = treino.copy(exercicios = treino.exercicios + WorkoutExerciseJson("sumiu", 3, 10))
        val result = BackupCodec.decode(backup(listOf(custom), listOf(t)), catalog, setOf("custom-1.gif"))
        val content = (result as BackupDecodeResult.Ok).content
        assertEquals(listOf("sumiu"), content.skippedExerciseIds)
        assertEquals(2, content.workouts[0].items.size)
    }

    @Test
    fun `formato errado e treino invalido sao erro`() {
        assertTrue(BackupCodec.decode(backup(formato = "x"), catalog, emptySet()) is BackupDecodeResult.Error)
        val bad = treino.copy(exercicios = listOf(WorkoutExerciseJson("leg-press", 0, 10)))
        assertTrue(BackupCodec.decode(backup(treinos = listOf(bad)), catalog, emptySet()) is BackupDecodeResult.Error)
        assertTrue(BackupCodec.decode("lixo", catalog, emptySet()) is BackupDecodeResult.Error)
    }
}
