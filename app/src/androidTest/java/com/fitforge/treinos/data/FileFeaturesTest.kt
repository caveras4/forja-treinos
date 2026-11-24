package com.fitforge.treinos.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitforge.treinos.data.backup.BackupManager
import com.fitforge.treinos.data.backup.BackupResult
import com.fitforge.treinos.data.files.TextFiles
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.media.CustomMediaStorage
import com.fitforge.treinos.data.media.MediaImportResult
import com.fitforge.treinos.data.repository.CreateExerciseResult
import com.fitforge.treinos.data.repository.LocalExerciseRepository
import com.fitforge.treinos.data.repository.LocalWorkoutRepository
import com.fitforge.treinos.domain.WorkoutDraft
import com.fitforge.treinos.ui.editor.EditorStep
import com.fitforge.treinos.ui.editor.EditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class FileFeaturesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var workouts: LocalWorkoutRepository
    private lateinit var exercises: LocalExerciseRepository
    private lateinit var media: CustomMediaStorage
    private lateinit var dir: File

    @Before
    fun setUp() = runBlocking<Unit> {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        media = CustomMediaStorage(context)
        workouts = LocalWorkoutRepository(db)
        exercises = LocalExerciseRepository(db, context.assets, media)
        exercises.syncCatalog()
        dir = File(context.cacheDir, "testes-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    private fun file(name: String, bytes: ByteArray): Uri =
        Uri.fromFile(File(dir, name).apply { writeBytes(bytes) })

    private fun png(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    // ---------- Mídia personalizada ----------

    @Test
    fun midiaValidaECopiadaParaOArmazenamentoDoApp() = runBlocking<Unit> {
        val result = media.import(file("ok.png", png(320, 240)))
        assertTrue(result is MediaImportResult.Ok)
        val copied = media.file((result as MediaImportResult.Ok).fileName)
        assertTrue(copied.exists())
        assertTrue(copied.name.endsWith(".png"))
        copied.delete()
    }

    @Test
    fun midiaGrandeDemaisERecusada() = runBlocking<Unit> {
        val result = media.import(file("grande.gif", ByteArray(2 * 1024 * 1024 + 10) { 'G'.code.toByte() }))
        assertTrue(result is MediaImportResult.Rejected)
        assertTrue((result as MediaImportResult.Rejected).message.contains("2 MB"))
    }

    @Test
    fun midiaComDimensaoAcimaDe720ERecusada() = runBlocking<Unit> {
        val result = media.import(file("enorme.png", png(1080, 400)))
        assertTrue((result as MediaImportResult.Rejected).message.contains("720"))
    }

    @Test
    fun arquivoQueNaoEImagemERecusadoMesmoComExtensaoGif() = runBlocking<Unit> {
        val result = media.import(file("falso.gif", "isto é texto".toByteArray()))
        assertTrue((result as MediaImportResult.Rejected).message.contains("Formato"))
    }

    // ---------- Importar JSON pelo ViewModel ----------

    private fun editor() = EditorViewModel(SavedStateHandle(), workouts, exercises, media, TextFiles(context.contentResolver))

    private suspend fun waitCatalog(vm: EditorViewModel) = withTimeout(5_000) {
        vm.uiState.first { it.exercises.isNotEmpty() && !it.loading }
    }

    @Test
    fun importarJsonValidoVaiDiretoParaOFormulario() = runBlocking<Unit> {
        val json = """{"formato":"forja-treino","versao":1,"treino":{"nome":"Importado","exercicios":[
            {"id":"leg-press","series":4,"repeticoes":10,"cargaKg":150},
            {"id":"prancha","series":3,"repeticoes":1}]}}"""
        val vm = editor()
        waitCatalog(vm)
        vm.onImportFile(file("treino.json", json.toByteArray()))
        val state = withTimeout(5_000) { vm.uiState.first { it.step == EditorStep.FORM } }
        assertEquals("Importado", state.draft.name)
        assertEquals(listOf("leg-press", "prancha"), state.draft.items.map { it.exerciseId })
        assertEquals("150", state.draft.items[0].weightText)
    }

    @Test
    fun importarJsonComIdDesconhecidoPedeConfirmacao() = runBlocking<Unit> {
        val json = """{"formato":"forja-treino","versao":1,"treino":{"nome":"Parcial","exercicios":[
            {"id":"leg-press","series":4,"repeticoes":10},
            {"id":"nao-existe","series":3,"repeticoes":10}]}}"""
        val vm = editor()
        waitCatalog(vm)
        vm.onImportFile(file("parcial.json", json.toByteArray()))
        val state = withTimeout(5_000) { vm.uiState.first { it.importPrompt != null } }
        assertEquals(listOf("nao-existe"), state.importPrompt!!.unknownIds)
        vm.onConfirmImport()
        assertEquals(1, vm.uiState.value.draft.items.size)
        assertEquals(EditorStep.FORM, vm.uiState.value.step)
    }

    @Test
    fun importarJsonGrandeDemaisMostraErro() = runBlocking<Unit> {
        val vm = editor()
        waitCatalog(vm)
        vm.onImportFile(file("grande.json", ByteArray(300 * 1024) { ' '.code.toByte() }))
        val state = withTimeout(5_000) { vm.uiState.first { it.message != null } }
        assertTrue(state.message!!.contains("256 KB"))
        assertEquals(EditorStep.PICKER, state.step)
    }

    // ---------- Backup .zip ----------

    @Test
    fun backupExportaERestauraTreinosPersonalizadosEMidia() = runBlocking<Unit> {
        val mediaName = (media.import(file("m.png", png(200, 200))) as MediaImportResult.Ok).fileName
        val custom = (exercises.createCustom("Meu supino", "Peito", mediaName) as CreateExerciseResult.Ok).exercise
        workouts.saveDraft(
            WorkoutDraft(name = "Backup A").toggle("leg-press").toggle(custom.id).setWeightText("leg-press", "90"),
            null
        )
        val manager = BackupManager(context.contentResolver, db, workouts, media)
        val zip = Uri.fromFile(File(dir, "backup.zip"))
        assertTrue(manager.export(zip) is BackupResult.Exported)

        // Apaga tudo e cria outra coisa, para ver a substituição.
        db.workoutDao().deleteAll()
        exercises.deleteCustom(custom)
        workouts.saveDraft(WorkoutDraft(name = "Vai sumir").toggle("prancha"), null)

        val result = manager.import(zip)
        assertTrue(result.toString(), result is BackupResult.Imported)
        val restored = db.workoutDao().getAll()
        assertEquals(listOf("Backup A"), restored.map { it.name })
        val draft = workouts.loadDraft(restored.single().id)!!
        assertEquals(listOf("leg-press", custom.id), draft.items.map { it.exerciseId })
        assertEquals("90", draft.items[0].weightText)
        assertTrue(media.file(mediaName).exists())
    }

    @Test
    fun backupComEntradaMaliciosaNaoEscreveForaDaPasta() = runBlocking<Unit> {
        val zipFile = File(dir, "malicioso.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(
                """{"formato":"forja-backup","versao":1,"exerciciosPersonalizados":[],"treinos":[]}""".toByteArray()
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("midia/../../shared_prefs/x.png"))
            zip.write(png(10, 10))
            zip.closeEntry()
        }
        val manager = BackupManager(context.contentResolver, db, workouts, media)
        manager.import(Uri.fromFile(zipFile))
        assertTrue(!File(context.filesDir.parentFile, "shared_prefs/x.png").exists())
    }

    @Test
    fun arquivoQueNaoEZipFalhaSemApagarDados() = runBlocking<Unit> {
        workouts.saveDraft(WorkoutDraft(name = "Fica").toggle("prancha"), null)
        val manager = BackupManager(context.contentResolver, db, workouts, media)
        val result = manager.import(file("nao-e-zip.zip", "texto qualquer".toByteArray()))
        assertTrue(result is BackupResult.Failed)
        assertEquals(listOf("Fica"), db.workoutDao().getAll().map { it.name })
    }
}
