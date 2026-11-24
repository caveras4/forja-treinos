package com.fitforge.treinos.data

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fitforge.treinos.domain.MuscleGroups
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogAssetsTest {

    @Test
    fun todoExercicioDoCatalogoTemMidiaValidaEGrupoConhecido() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val text = assets.open("exercises.json").bufferedReader().use { it.readText() }
        val list = Json.parseToJsonElement(text).jsonObject["exercicios"]!!.jsonArray
        assertEquals(174, list.size)

        val ids = HashSet<String>()
        val problems = mutableListOf<String>()
        for (element in list) {
            val obj = element.jsonObject
            val id = obj["id"]!!.jsonPrimitive.content
            val group = obj["grupo"]!!.jsonPrimitive.content
            val media = obj["midia"]!!.jsonPrimitive.content
            if (!ids.add(id)) problems += "id duplicado: $id"
            if (group !in MuscleGroups.ORDER) problems += "grupo desconhecido: $id/$group"
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            try {
                assets.open(media).use { BitmapFactory.decodeStream(it, null, options) }
                if (options.outWidth <= 0) problems += "mídia ilegível: $media"
            } catch (e: Exception) {
                problems += "mídia ausente: $media"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
