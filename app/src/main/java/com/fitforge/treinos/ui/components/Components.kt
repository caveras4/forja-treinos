package com.fitforge.treinos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.fitforge.treinos.TreinosApplication
import com.fitforge.treinos.data.local.ExerciseEntity
import com.fitforge.treinos.ui.theme.AppPalette
import java.io.File

/** Verde de "selecionado / feito", fixo em qualquer paleta, como o usuário pediu. */
object SuccessColors {
    @Composable
    fun container(): Color = if (isSystemInDarkThemeCompat()) Color(0xFF14532D) else Color(0xFFDCFCE7)

    @Composable
    fun onContainer(): Color = if (isSystemInDarkThemeCompat()) Color(0xFFBBF7D0) else Color(0xFF14532D)

    @Composable
    fun accent(): Color = if (isSystemInDarkThemeCompat()) Color(0xFF4ADE80) else Color(0xFF16A34A)

    @Composable
    private fun isSystemInDarkThemeCompat(): Boolean =
        MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
}

fun paletteSwatch(key: String): Color = when (AppPalette.fromKey(key)) {
    AppPalette.EMERALD -> Color(0xFF059669)
    AppPalette.COBALT -> Color(0xFF2563EB)
    AppPalette.AMBER -> Color(0xFFD97706)
    AppPalette.CRIMSON -> Color(0xFFE11D48)
}

/** Monta o modelo do Coil: arquivo de assets (catálogo) ou de filesDir (personalizado). */
@Composable
fun rememberMediaModel(exercise: ExerciseEntity): Any? {
    val context = LocalContext.current
    return remember(exercise.id, exercise.mediaPath) {
        val path = exercise.mediaPath ?: return@remember null
        if (exercise.isCustom) {
            val app = context.applicationContext as TreinosApplication
            app.container.mediaStorage.file(path)
        } else {
            "file:///android_asset/$path"
        }
    }
}

/** Animação do exercício, com espaço reservado enquanto carrega ou quando não há imagem. */
@Composable
fun ExerciseMedia(exercise: ExerciseEntity, modifier: Modifier = Modifier) {
    val model = rememberMediaModel(exercise)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = "Demonstração: ${exercise.name}" },
        contentAlignment = Alignment.Center
    ) {
        if (model == null || (model is File && !model.exists())) {
            MediaPlaceholder(text = "Sem imagem")
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                error = { MediaPlaceholder(text = "Imagem indisponível") }
            )
        }
    }
}

@Composable
private fun MediaPlaceholder(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp)
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Seletor numérico compacto: [-] valor [+]. */
@Composable
fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            FilledTonalIconButton(
                onClick = { onValueChange(value - 1) },
                enabled = value > range.first,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Diminuir $label")
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 32.dp)
                    .semantics { contentDescription = "$label: $value" }
            )
            FilledTonalIconButton(
                onClick = { onValueChange(value + 1) },
                enabled = value < range.last,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Aumentar $label")
            }
        }
    }
}

/** Bolinhas para escolher a cor do treino. */
@Composable
fun ColorPicker(selectedKey: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppPalette.entries.forEach { palette ->
            val selected = palette.key == selectedKey
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(paletteSwatch(palette.key))
                    .clickable { onSelect(palette.key) }
                    .semantics { contentDescription = "Cor ${palette.label}" + if (selected) " (selecionada)" else "" }
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Selo com a inicial do treino, na cor escolhida. */
@Composable
fun WorkoutBadge(name: String, colorKey: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(paletteSwatch(colorKey))
    ) {
        Text(
            text = badgeLetter(name),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/** "Treino A" → "A"; "Pernas" → "P". */
fun badgeLetter(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val word = if (words.size > 1 && words[0].equals("treino", ignoreCase = true)) words[1] else words.firstOrNull()
    return word?.take(1)?.uppercase() ?: "?"
}
