package com.fitforge.treinos.ui.theme

enum class AppPalette(
    val key: String,
    val label: String
) {
    EMERALD("emerald", "Esmeralda"),
    COBALT("cobalt", "Cobalto"),
    AMBER("amber", "Âmbar"),
    CRIMSON("crimson", "Carmesim");

    companion object {
        fun fromKey(key: String): AppPalette = entries.firstOrNull { it.key == key } ?: EMERALD
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("Automático"),
    LIGHT("Claro"),
    DARK("Escuro")
}
