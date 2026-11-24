package com.fitforge.treinos.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.fitforge.treinos.domain.ActiveSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Guarda o treino em andamento no DataStore. Cada alteração é gravada na hora, então
 * fechar o app (ou o Android matar o processo) não perde o progresso.
 */
class SessionRepository(private val dataStore: DataStore<Preferences>) {

    val active: Flow<ActiveSession?> = dataStore.data
        .map { prefs ->
            prefs[WORKOUT_ID]?.let { id ->
                ActiveSession(
                    workoutId = id,
                    doneExerciseIds = prefs[DONE].orEmpty(),
                    startedAt = prefs[STARTED_AT] ?: 0L
                )
            }
        }
        .distinctUntilChanged()

    suspend fun current(): ActiveSession? = active.first()

    /** Começa [workoutId] do zero, substituindo qualquer treino em andamento. */
    suspend fun start(workoutId: Long) {
        dataStore.edit {
            it[WORKOUT_ID] = workoutId
            it[DONE] = emptySet()
            it[STARTED_AT] = System.currentTimeMillis()
        }
    }

    /** Começa [workoutId] só se nenhum treino estiver em andamento. */
    suspend fun startIfIdle(workoutId: Long) {
        dataStore.edit { if (it[WORKOUT_ID] == null) {
            it[WORKOUT_ID] = workoutId
            it[DONE] = emptySet()
            it[STARTED_AT] = System.currentTimeMillis()
        } }
    }

    suspend fun toggleDone(workoutId: Long, exerciseId: String) = update(workoutId) { it.toggle(exerciseId) }

    suspend fun clearDone(workoutId: Long) = update(workoutId) { it.copy(doneExerciseIds = emptySet()) }

    /** Depois de editar o treino: tira as marcações de exercícios removidos. */
    suspend fun retain(workoutId: Long, exerciseIds: Collection<String>) = update(workoutId) { it.retain(exerciseIds) }

    suspend fun finish() {
        dataStore.edit { clear(it) }
    }

    /** Encerra a sessão se ela for deste treino (ex.: o treino foi excluído). */
    suspend fun finishIf(workoutId: Long) {
        dataStore.edit { if (it[WORKOUT_ID] == workoutId) clear(it) }
    }

    /** Só altera se a sessão ativa for de [workoutId]; ignora chamadas atrasadas de outra tela. */
    private suspend fun update(workoutId: Long, transform: (ActiveSession) -> ActiveSession) {
        dataStore.edit { prefs ->
            if (prefs[WORKOUT_ID] != workoutId) return@edit
            val current = ActiveSession(workoutId, prefs[DONE].orEmpty(), prefs[STARTED_AT] ?: 0L)
            prefs[DONE] = transform(current).doneExerciseIds
        }
    }

    private fun clear(prefs: MutablePreferences) {
        prefs.remove(WORKOUT_ID)
        prefs.remove(DONE)
        prefs.remove(STARTED_AT)
    }

    private companion object {
        val WORKOUT_ID = longPreferencesKey("session_workout_id")
        val DONE = stringSetPreferencesKey("session_done_exercises")
        val STARTED_AT = longPreferencesKey("session_started_at")
    }
}
