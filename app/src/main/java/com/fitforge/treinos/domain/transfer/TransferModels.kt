package com.fitforge.treinos.domain.transfer

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Arquivo de compartilhamento de um treino (formato "forja-treino", versão 1). */
@Serializable
data class WorkoutFile(
    val formato: String,
    val versao: Int,
    val treino: WorkoutJson
)

@Serializable
data class WorkoutJson(
    val nome: String,
    val foco: String = "",
    val cor: String = "emerald",
    val exercicios: List<WorkoutExerciseJson>
)

@Serializable
data class WorkoutExerciseJson(
    val id: String,
    val series: Int,
    val repeticoes: Int,
    val cargaKg: Double? = null
)

/** Conteúdo do backup.json dentro do .zip de backup completo. */
@Serializable
data class BackupFile(
    val formato: String,
    val versao: Int,
    val exerciciosPersonalizados: List<CustomExerciseJson> = emptyList(),
    val treinos: List<WorkoutJson> = emptyList()
)

@Serializable
data class CustomExerciseJson(
    val id: String,
    val nome: String,
    val grupo: String,
    /** Nome do arquivo dentro de "midia/" no zip, ou null se o exercício não tem imagem. */
    val midia: String? = null
)

object TransferFormat {
    const val WORKOUT_FORMAT = "forja-treino"
    const val BACKUP_FORMAT = "forja-backup"
    const val VERSION = 1
    const val WORKOUT_MAX_BYTES = 256L * 1024
    const val BACKUP_JSON_MAX_BYTES = 5L * 1024 * 1024
    const val BACKUP_JSON_ENTRY = "backup.json"
    const val BACKUP_MEDIA_DIR = "midia/"
    const val BACKUP_MAX_WORKOUTS = 200
    const val BACKUP_MAX_CUSTOM = 500

    /** Nomes de mídia aceitos no zip: sem barra, sem "..", sem caminho absoluto. */
    val SAFE_MEDIA_NAME = Regex("^[A-Za-z0-9_-]{1,80}\\.(gif|webp|png|jpg)$")

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }
}
