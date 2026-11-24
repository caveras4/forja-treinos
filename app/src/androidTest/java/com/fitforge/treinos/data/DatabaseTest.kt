package com.fitforge.treinos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.local.ExerciseEntity
import com.fitforge.treinos.data.media.CustomMediaStorage
import com.fitforge.treinos.data.repository.CreateExerciseResult
import com.fitforge.treinos.data.repository.LocalExerciseRepository
import com.fitforge.treinos.data.repository.LocalWorkoutRepository
import com.fitforge.treinos.domain.WorkoutDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var workouts: LocalWorkoutRepository
    private lateinit var exercises: LocalExerciseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        workouts = LocalWorkoutRepository(db)
        exercises = LocalExerciseRepository(db, context.assets, CustomMediaStorage(context))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun catalogoSincronizaComTodosOsExercicios() = runTest {
        exercises.syncCatalog()
        assertEquals(175, db.exerciseDao().countCatalog())
        // Rodar de novo não duplica.
        exercises.syncCatalog()
        assertEquals(175, db.exerciseDao().countCatalog())
    }

    @Test
    fun salvarRascunhoGravaOrdemSeriesRepsECarga() = runTest {
        exercises.syncCatalog()
        val draft = WorkoutDraft(name = "Treino Teste", colorKey = "cobalt")
            .toggle("leg-press").toggle("supino-reto-barra")
            .setWeightText("leg-press", "120,5").setReps("supino-reto-barra", 8)
        val id = workouts.saveDraft(draft, null)

        val items = workouts.observeExercises(id).first()
        assertEquals(listOf("leg-press", "supino-reto-barra"), items.map { it.exercise.id })
        assertEquals(120.5, items[0].item.weightKg!!, 0.0)
        assertNull(items[1].item.weightKg)
        assertEquals(8, items[1].item.reps)

        val summary = workouts.observeSummaries().first().single()
        assertEquals(2, summary.exerciseCount)
    }

    @Test
    fun editarSubstituiItensENaoDuplicaTreino() = runTest {
        exercises.syncCatalog()
        val id = workouts.saveDraft(WorkoutDraft(name = "T1").toggle("leg-press").toggle("prancha"), null)
        val loaded = workouts.loadDraft(id)!!
        val edited = loaded.move("prancha", "leg-press").remove("leg-press").toggle("face-pull").copy(name = "T1 editado")
        assertEquals(id, workouts.saveDraft(edited, id))

        assertEquals(1, workouts.observeSummaries().first().size)
        assertEquals("T1 editado", workouts.loadDraft(id)!!.name)
        assertEquals(listOf("prancha", "face-pull"), workouts.loadDraft(id)!!.items.map { it.exerciseId })
    }

    @Test
    fun cargaNovaSubstituiAAnterior() = runTest {
        exercises.syncCatalog()
        val id = workouts.saveDraft(WorkoutDraft(name = "T").toggle("leg-press").setWeightText("leg-press", "100"), null)
        val item = workouts.observeExercises(id).first().single().item
        workouts.updateWeight(item.id, 110.0)
        assertEquals(110.0, workouts.observeExercises(id).first().single().item.weightKg!!, 0.0)
    }

    @Test
    fun excluirTreinoApagaItensEmCascata() = runTest {
        exercises.syncCatalog()
        val id = workouts.saveDraft(WorkoutDraft(name = "T").toggle("leg-press"), null)
        workouts.deleteWorkout(db.workoutDao().getById(id)!!)
        assertTrue(db.workoutExerciseDao().getAll().isEmpty())
    }

    @Test
    fun excluirPersonalizadoRemoveDosTreinos() = runTest {
        exercises.syncCatalog()
        val created = exercises.createCustom("Meu exercício", "Peito", null) as CreateExerciseResult.Ok
        val id = workouts.saveDraft(WorkoutDraft(name = "T").toggle(created.exercise.id).toggle("leg-press"), null)
        assertEquals(1, exercises.countWorkoutsUsing(created.exercise.id))

        exercises.deleteCustom(created.exercise)
        assertEquals(listOf("leg-press"), workouts.loadDraft(id)!!.items.map { it.exerciseId })
    }

    @Test
    fun nomeDePersonalizadoRepetidoERecusado() = runTest {
        exercises.syncCatalog()
        assertTrue(exercises.createCustom("leg press", "Quadríceps", null) is CreateExerciseResult.Invalid)
        assertTrue(exercises.createCustom("X", "Peito", null) is CreateExerciseResult.Invalid)
    }

    @Test
    fun catalogoNaoPodeSerExcluidoComoPersonalizado() = runTest {
        exercises.syncCatalog()
        exercises.deleteCustom(ExerciseEntity("leg-press", "Leg press", "Quadríceps", null, isCustom = true))
        assertEquals(175, db.exerciseDao().countCatalog())
    }
}
