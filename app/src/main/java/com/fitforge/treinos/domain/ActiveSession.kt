package com.fitforge.treinos.domain

/**
 * Treino em andamento. Só existe um por vez e ele sobrevive a sair da tela e fechar o app.
 *
 * Os exercícios feitos são guardados pelo id do exercício (ex.: "leg-press"), não pelo id da
 * linha no banco: editar o treino recria as linhas, mas o exercício continua o mesmo.
 */
data class ActiveSession(
    val workoutId: Long,
    val doneExerciseIds: Set<String> = emptySet(),
    val startedAt: Long = 0L
) {
    fun isDone(exerciseId: String): Boolean = exerciseId in doneExerciseIds

    fun toggle(exerciseId: String): ActiveSession =
        copy(doneExerciseIds = if (isDone(exerciseId)) doneExerciseIds - exerciseId else doneExerciseIds + exerciseId)

    /** Descarta marcações de exercícios que saíram do treino. */
    fun retain(exerciseIds: Collection<String>): ActiveSession =
        copy(doneExerciseIds = doneExerciseIds.intersect(exerciseIds.toSet()))

    fun doneCount(exerciseIds: Collection<String>): Int = exerciseIds.count { it in doneExerciseIds }
}

/** O que fazer quando o usuário toca num treino na Home. */
sealed interface StartDecision {
    /** Nenhum treino em andamento: começa este. */
    data object StartNew : StartDecision

    /** É o próprio treino em andamento: só continua. */
    data object Resume : StartDecision

    /** Outro treino está em andamento: perguntar ao usuário. */
    data class Conflict(val activeWorkoutId: Long) : StartDecision
}

fun decideStart(active: ActiveSession?, targetWorkoutId: Long): StartDecision = when {
    active == null -> StartDecision.StartNew
    active.workoutId == targetWorkoutId -> StartDecision.Resume
    else -> StartDecision.Conflict(active.workoutId)
}
