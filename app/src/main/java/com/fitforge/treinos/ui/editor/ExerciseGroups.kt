package com.fitforge.treinos.ui.editor

import com.fitforge.treinos.data.local.ExerciseEntity
import com.fitforge.treinos.domain.MuscleGroups
import java.text.Normalizer

data class ExerciseGroup(val name: String, val exercises: List<ExerciseEntity>)

/** Remove acentos e caixa: "Tríceps" e "triceps" casam na busca. */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

/**
 * Agrupa os exercícios por grupo muscular, na ordem de [MuscleGroups.ORDER], aplicando
 * o filtro de grupo e a busca por nome. Grupos sem resultado não aparecem.
 */
fun buildGroups(
    exercises: Collection<ExerciseEntity>,
    query: String,
    groupFilter: String?
): List<ExerciseGroup> {
    val q = normalizeForSearch(query)
    // Normaliza cada nome uma vez só (a ordenação compararia o mesmo nome várias vezes).
    return exercises
        .asSequence()
        .filter { groupFilter == null || it.muscleGroup == groupFilter }
        .map { it to normalizeForSearch(it.name) }
        .filter { (_, name) -> q.isEmpty() || name.contains(q) }
        .groupBy { (exercise, _) -> exercise.muscleGroup }
        .map { (group, list) -> ExerciseGroup(group, list.sortedBy { it.second }.map { it.first }) }
        .sortedBy { MuscleGroups.sortIndex(it.name) }
}
