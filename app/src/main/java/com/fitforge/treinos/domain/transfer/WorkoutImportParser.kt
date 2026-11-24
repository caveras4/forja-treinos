package com.fitforge.treinos.domain.transfer

import com.fitforge.treinos.domain.DraftItem
import com.fitforge.treinos.domain.WeightFormat
import com.fitforge.treinos.domain.WorkoutDraft
import com.fitforge.treinos.domain.WorkoutRules
import com.fitforge.treinos.domain.uniqueName
import kotlinx.serialization.SerializationException

sealed interface ImportResult {
    data class Error(val message: String) : ImportResult

    /**
     * Treino pronto para revisão. [unknownIds] lista os exercícios do arquivo que não existem
     * neste aparelho; eles ficam fora do [draft].
     */
    data class Parsed(
        val draft: WorkoutDraft,
        val totalInFile: Int,
        val unknownIds: List<String>
    ) : ImportResult
}

private val VALID_COLORS = setOf("emerald", "cobalt", "amber", "crimson")

/** Valida um treino (do JSON de treino ou de dentro do backup). Devolve a mensagem de erro, ou null. */
internal fun validateWorkoutJson(treino: WorkoutJson): String? {
    WorkoutRules.validateName(treino.nome)?.let { return it }
    WorkoutRules.validateFocus(treino.foco)?.let { return it }
    WorkoutRules.validateExerciseCount(treino.exercicios.size)?.let { return it }
    val ids = HashSet<String>()
    treino.exercicios.forEachIndexed { index, ex ->
        val pos = "Exercício ${index + 1} (${ex.id})"
        if (ex.id.isBlank()) return "Exercício ${index + 1}: id vazio."
        if (!ids.add(ex.id)) return "$pos aparece mais de uma vez."
        if (ex.series !in WorkoutRules.SETS_RANGE) {
            return "$pos: séries devem estar entre ${WorkoutRules.SETS_RANGE.first} e ${WorkoutRules.SETS_RANGE.last}."
        }
        if (ex.repeticoes !in WorkoutRules.REPS_RANGE) {
            return "$pos: repetições devem estar entre ${WorkoutRules.REPS_RANGE.first} e ${WorkoutRules.REPS_RANGE.last}."
        }
        if (ex.cargaKg != null && !WorkoutRules.isValidWeight(ex.cargaKg)) {
            return "$pos: carga deve estar entre 0 e ${WorkoutRules.WEIGHT_MAX.toInt()} kg."
        }
    }
    return null
}

internal fun WorkoutJson.toDraft(knownIds: Set<String>): Pair<WorkoutDraft, List<String>> {
    val (known, unknown) = exercicios.partition { it.id in knownIds }
    val draft = WorkoutDraft(
        name = nome.trim(),
        focus = foco.trim(),
        colorKey = cor.takeIf { it in VALID_COLORS } ?: "emerald",
        items = known.map {
            DraftItem(
                exerciseId = it.id,
                sets = it.series,
                reps = it.repeticoes,
                weightText = WeightFormat.toInput(it.cargaKg?.takeIf { kg -> kg > 0 })
            )
        }
    )
    return draft to unknown.map { it.id }
}

object WorkoutImportParser {

    /**
     * @param byteSize tamanho real do arquivo, conferido antes de ler o conteúdo.
     * @param knownIds ids de exercícios existentes no aparelho (catálogo + personalizados).
     * @param existingNames nomes dos treinos já cadastrados, para evitar duplicata.
     */
    fun parse(
        text: String,
        byteSize: Long,
        knownIds: Set<String>,
        existingNames: Collection<String>
    ): ImportResult {
        if (byteSize > TransferFormat.WORKOUT_MAX_BYTES) {
            return ImportResult.Error("Arquivo muito grande. O limite é 256 KB.")
        }
        val file = try {
            TransferFormat.json.decodeFromString(WorkoutFile.serializer(), text)
        } catch (e: SerializationException) {
            return ImportResult.Error("O arquivo não é um treino válido: campos ausentes ou JSON malformado.")
        } catch (e: IllegalArgumentException) {
            return ImportResult.Error("O arquivo não é um treino válido: campos ausentes ou JSON malformado.")
        }
        if (file.formato != TransferFormat.WORKOUT_FORMAT) {
            return ImportResult.Error("Formato \"${file.formato}\" não reconhecido. Esperado: \"${TransferFormat.WORKOUT_FORMAT}\".")
        }
        if (file.versao != TransferFormat.VERSION) {
            return ImportResult.Error("Versão ${file.versao} não suportada. Este app lê a versão ${TransferFormat.VERSION}.")
        }
        validateWorkoutJson(file.treino)?.let { return ImportResult.Error(it) }

        val (draft, unknown) = file.treino.toDraft(knownIds)
        if (draft.items.isEmpty()) {
            return ImportResult.Error("Nenhum exercício do arquivo existe neste aparelho.")
        }
        return ImportResult.Parsed(
            draft = draft.copy(name = uniqueName(draft.name, existingNames)),
            totalInFile = file.treino.exercicios.size,
            unknownIds = unknown
        )
    }

    fun export(draftName: String, focus: String, colorKey: String, items: List<WorkoutExerciseJson>): String =
        TransferFormat.json.encodeToString(
            WorkoutFile.serializer(),
            WorkoutFile(
                formato = TransferFormat.WORKOUT_FORMAT,
                versao = TransferFormat.VERSION,
                treino = WorkoutJson(nome = draftName, foco = focus, cor = colorKey, exercicios = items)
            )
        )
}
