package com.fitforge.treinos.domain

/** Um exercício dentro do rascunho do treino. A carga fica como texto até salvar. */
data class DraftItem(
    val exerciseId: String,
    val sets: Int = WorkoutRules.DEFAULT_SETS,
    val reps: Int = WorkoutRules.DEFAULT_REPS,
    val weightText: String = ""
)

/**
 * Rascunho de um treino em criação ou edição. Nada vai para o banco até o usuário salvar.
 * Todas as operações devolvem uma cópia nova, então o estado da tela é imutável.
 */
data class WorkoutDraft(
    val name: String = "",
    val focus: String = "",
    val colorKey: String = "emerald",
    val items: List<DraftItem> = emptyList()
) {
    val selectedIds: Set<String> get() = items.mapTo(LinkedHashSet()) { it.exerciseId }

    fun isSelected(exerciseId: String): Boolean = items.any { it.exerciseId == exerciseId }

    /** Marca ou desmarca o exercício. Quem é marcado vai para o fim, então a ordem segue a seleção. */
    fun toggle(exerciseId: String): WorkoutDraft =
        if (isSelected(exerciseId)) remove(exerciseId)
        else copy(items = items + DraftItem(exerciseId))

    fun remove(exerciseId: String): WorkoutDraft =
        copy(items = items.filterNot { it.exerciseId == exerciseId })

    /** Move o item [fromId] para a posição atual de [toId] (arrastar e soltar). */
    fun move(fromId: String, toId: String): WorkoutDraft {
        val from = items.indexOfFirst { it.exerciseId == fromId }
        val to = items.indexOfFirst { it.exerciseId == toId }
        if (from < 0 || to < 0 || from == to) return this
        val list = items.toMutableList()
        list.add(to, list.removeAt(from))
        return copy(items = list)
    }

    fun update(exerciseId: String, transform: (DraftItem) -> DraftItem): WorkoutDraft =
        copy(items = items.map { if (it.exerciseId == exerciseId) transform(it) else it })

    fun setSets(exerciseId: String, sets: Int): WorkoutDraft =
        update(exerciseId) { it.copy(sets = sets.coerceIn(WorkoutRules.SETS_RANGE)) }

    fun setReps(exerciseId: String, reps: Int): WorkoutDraft =
        update(exerciseId) { it.copy(reps = reps.coerceIn(WorkoutRules.REPS_RANGE)) }

    fun setWeightText(exerciseId: String, text: String): WorkoutDraft =
        update(exerciseId) { it.copy(weightText = text) }

    /** Remove exercícios que deixaram de existir (ex.: personalizado excluído). */
    fun retainExisting(existingIds: Set<String>): WorkoutDraft =
        copy(items = items.filter { it.exerciseId in existingIds })

    /** Valida tudo de uma vez. Lista vazia = pode salvar. */
    fun validate(): DraftErrors {
        val itemErrors = items
            .filter { WeightFormat.parse(it.weightText) == WeightInput.Invalid }
            .associate { it.exerciseId to "Carga inválida (0 a ${WorkoutRules.WEIGHT_MAX.toInt()} kg)." }
        return DraftErrors(
            name = WorkoutRules.validateName(name),
            focus = WorkoutRules.validateFocus(focus),
            exercises = WorkoutRules.validateExerciseCount(items.size),
            items = itemErrors
        )
    }
}

data class DraftErrors(
    val name: String? = null,
    val focus: String? = null,
    val exercises: String? = null,
    val items: Map<String, String> = emptyMap()
) {
    val isEmpty: Boolean get() = name == null && focus == null && exercises == null && items.isEmpty()
}
