package com.fitforge.treinos.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Limites usados na criação, na edição e na importação de treinos. */
object WorkoutRules {
    const val NAME_MIN = 2
    const val NAME_MAX = 48
    const val FOCUS_MAX = 80
    const val EXERCISES_MIN = 1
    const val EXERCISES_MAX = 30
    val SETS_RANGE = 1..10
    val REPS_RANGE = 1..100
    const val WEIGHT_MAX = 500.0
    const val DEFAULT_SETS = 3
    const val DEFAULT_REPS = 12
    const val EXERCISE_NAME_MIN = 2
    const val EXERCISE_NAME_MAX = 48

    fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Informe o nome do treino."
            trimmed.length < NAME_MIN -> "O nome deve ter pelo menos $NAME_MIN caracteres."
            trimmed.length > NAME_MAX -> "O nome deve ter no máximo $NAME_MAX caracteres."
            else -> null
        }
    }

    fun validateFocus(focus: String): String? =
        if (focus.trim().length > FOCUS_MAX) "O foco deve ter no máximo $FOCUS_MAX caracteres." else null

    fun validateExerciseCount(count: Int): String? = when {
        count < EXERCISES_MIN -> "Adicione pelo menos um exercício."
        count > EXERCISES_MAX -> "O treino pode ter no máximo $EXERCISES_MAX exercícios."
        else -> null
    }

    fun validateExerciseName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.length < EXERCISE_NAME_MIN -> "O nome deve ter pelo menos $EXERCISE_NAME_MIN caracteres."
            trimmed.length > EXERCISE_NAME_MAX -> "O nome deve ter no máximo $EXERCISE_NAME_MAX caracteres."
            else -> null
        }
    }

    fun isValidWeight(weight: Double): Boolean = !weight.isNaN() && weight in 0.0..WEIGHT_MAX
}

/** Resultado da leitura do campo de carga digitado pelo usuário. */
sealed interface WeightInput {
    /** Campo vazio: exercício sem carga (ex.: peso do corpo). */
    data object Empty : WeightInput
    data class Value(val kg: Double) : WeightInput
    data object Invalid : WeightInput
}

object WeightFormat {
    private val formatter = DecimalFormat("0.##", DecimalFormatSymbols(Locale("pt", "BR")))

    /** Aceita vírgula ou ponto como separador decimal. */
    fun parse(text: String): WeightInput {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty()) return WeightInput.Empty
        val value = normalized.toDoubleOrNull() ?: return WeightInput.Invalid
        return if (WorkoutRules.isValidWeight(value)) WeightInput.Value(value) else WeightInput.Invalid
    }

    /** Texto para o campo de edição: "40", "12,5" ou vazio. */
    fun toInput(kg: Double?): String = kg?.let { formatter.format(it) } ?: ""

    /** Texto para exibição: "40 kg", "12,5 kg" ou "Sem carga". */
    fun display(kg: Double?): String =
        if (kg == null || kg == 0.0) "Sem carga" else "${formatter.format(kg)} kg"

    /** Soma [delta] à carga atual, sem sair de 0..500. Zero vira "sem carga" (null). */
    fun step(current: Double?, delta: Double): Double? {
        val next = ((current ?: 0.0) + delta).coerceIn(0.0, WorkoutRules.WEIGHT_MAX)
        return if (next == 0.0) null else next
    }
}

/** Grupos musculares, na ordem em que aparecem na lista. */
object MuscleGroups {
    val ORDER = listOf(
        "Peito", "Costas", "Ombros", "Bíceps", "Tríceps", "Antebraço",
        "Quadríceps", "Posterior", "Glúteos", "Panturrilha", "Abdômen"
    )

    fun sortIndex(group: String): Int = ORDER.indexOf(group).let { if (it < 0) ORDER.size else it }

    /** Sugestão de foco a partir dos grupos dos exercícios, na ordem em que aparecem. */
    fun suggestFocus(groups: List<String>): String {
        val distinct = groups.distinct()
        return when (distinct.size) {
            0 -> ""
            1 -> distinct[0]
            else -> distinct.dropLast(1).joinToString(", ") + " e " + distinct.last()
        }.take(WorkoutRules.FOCUS_MAX)
    }
}

/** Gera "Treino A (2)", "Treino A (3)"... quando o nome já existe. */
fun uniqueName(name: String, existing: Collection<String>): String {
    val taken = existing.map { it.trim().lowercase() }.toSet()
    val base = name.trim()
    if (base.lowercase() !in taken) return base
    var n = 2
    while (true) {
        val suffix = " ($n)"
        val candidate = base.take(WorkoutRules.NAME_MAX - suffix.length) + suffix
        if (candidate.lowercase() !in taken) return candidate
        n++
    }
}
