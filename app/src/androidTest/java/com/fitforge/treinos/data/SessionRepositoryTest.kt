package com.fitforge.treinos.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitforge.treinos.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {

    private lateinit var file: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        file = File(context.cacheDir, "sessao-${System.nanoTime()}.preferences_pb")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    private fun newRepository(s: CoroutineScope = scope) =
        SessionRepository(PreferenceDataStoreFactory.create(scope = s) { file })

    @Test
    fun progressoSobreviveAoAppSerFechado() = runBlocking<Unit> {
        val first = newRepository()
        first.start(10)
        first.toggleDone(10, "leg-press")
        first.toggleDone(10, "prancha")
        scope.cancel() // "fecha o app": o DataStore em memória morre

        // Um DataStore novo lendo o mesmo arquivo = app aberto de novo.
        val reopened = newRepository(CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scope = it })
        val session = reopened.current()!!
        assertEquals(10L, session.workoutId)
        assertEquals(setOf("leg-press", "prancha"), session.doneExerciseIds)
    }

    @Test
    fun soUmTreinoPorVez() = runBlocking<Unit> {
        val repo = newRepository()
        repo.start(1)
        repo.toggleDone(1, "leg-press")
        repo.start(2)
        val session = repo.current()!!
        assertEquals(2L, session.workoutId)
        assertEquals(emptySet<String>(), session.doneExerciseIds)
    }

    @Test
    fun startIfIdleNaoSubstituiTreinoAtivo() = runBlocking<Unit> {
        val repo = newRepository()
        repo.start(1)
        repo.startIfIdle(2)
        assertEquals(1L, repo.current()!!.workoutId)
    }

    @Test
    fun marcacaoDeOutroTreinoEIgnorada() = runBlocking<Unit> {
        val repo = newRepository()
        repo.start(1)
        repo.toggleDone(2, "leg-press")
        assertEquals(emptySet<String>(), repo.current()!!.doneExerciseIds)
    }

    @Test
    fun finalizarEFinishIf() = runBlocking<Unit> {
        val repo = newRepository()
        repo.start(1)
        repo.finishIf(2)
        assertEquals(1L, repo.current()!!.workoutId)
        repo.finishIf(1)
        assertNull(repo.current())
        repo.start(3)
        repo.finish()
        assertNull(repo.current())
    }
}
