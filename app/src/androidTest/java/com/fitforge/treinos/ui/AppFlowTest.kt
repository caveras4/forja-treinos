package com.fitforge.treinos.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fitforge.treinos.MainActivity
import com.fitforge.treinos.TreinosApplication
import com.fitforge.treinos.domain.WorkoutDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val container get() = (rule.activity.application as TreinosApplication).container
    private val suffix = System.currentTimeMillis().toString().takeLast(6)

    @Before
    fun waitForApp() {
        rule.waitUntil(15_000) { container.ready.value }
        rule.waitUntilAtLeastOneExists(hasTestTag("fab_criar"), 10_000)
    }

    private fun createWorkout(name: String, vararg ids: String): Long = runBlocking {
        var draft = WorkoutDraft(name = name)
        ids.forEach { draft = draft.toggle(it) }
        container.workoutRepository.saveDraft(draft, null)
    }

    /** O treino criado pelo repositório pode levar um instante para chegar na lista. */
    private fun scrollHomeTo(name: String): SemanticsNodeInteraction {
        rule.waitUntilAtLeastOneExists(hasTestTag("lista_treinos"), 10_000)
        rule.waitUntil(10_000) {
            runCatching {
                rule.onNodeWithTag("lista_treinos").performScrollToNode(hasTestTag("treino-$name"))
            }.isSuccess
        }
        return rule.onNodeWithTag("treino-$name")
    }

    private fun scrollFormTo(tag: String) {
        rule.onNodeWithTag("lista_form").performScrollToNode(hasTestTag(tag))
    }

    private fun searchAndExpand(name: String, id: String) {
        rule.onNodeWithTag("busca").performTextClearance()
        rule.onNodeWithTag("busca").performTextInput(name)
        rule.waitForIdle()
        rule.onNodeWithTag("nome-$id").performClick()
        rule.waitForIdle()
    }

    @Test
    fun criarTreino_listaExpansivelVerdeConcluirEConfigurar() {
        val name = "Treino UI $suffix"
        rule.onNodeWithTag("fab_criar").performClick()

        // Concluir só habilita com seleção.
        rule.onNodeWithTag("concluir").assertIsNotEnabled()

        // Tocar no nome expande e mostra a mídia e o botão de adicionar.
        searchAndExpand("Supino reto com barra", "supino-reto-barra")
        rule.onNodeWithContentDescription("Demonstração: Supino reto com barra").assertIsDisplayed()
        rule.onNodeWithText("Adicionar ao treino").performClick()
        rule.onNodeWithContentDescription("Supino reto com barra, adicionado ao treino").assertExists()
        rule.onNodeWithTag("concluir").assertIsEnabled()

        // Tocar no nome de novo recolhe.
        rule.onNodeWithTag("nome-supino-reto-barra").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Remover do treino").assertDoesNotExist()

        searchAndExpand("Leg press", "leg-press")
        rule.onNodeWithText("Adicionar ao treino").performClick()

        rule.onNodeWithTag("concluir").performClick()

        // Passo 2: só os selecionados, com séries/reps/carga.
        rule.onNodeWithTag("item-supino-reto-barra").assertExists()
        rule.onNodeWithTag("item-leg-press").assertExists()
        rule.onNodeWithText("Peito e Quadríceps").assertExists() // foco sugerido
        rule.onNodeWithTag("campo_nome").performTextInput(name)
        rule.onNodeWithTag("carga-leg-press").performTextInput("100")
        rule.onNodeWithTag("salvar").performClick()

        val card = scrollHomeTo(name)
        card.assertExists()
        rule.onNode(hasText("Peito e Quadríceps · 2 exercícios")).assertExists()
    }

    @Test
    fun salvarSemNomeMostraErro() {
        rule.onNodeWithTag("fab_criar").performClick()
        searchAndExpand("Prancha", "prancha")
        rule.onNodeWithText("Adicionar ao treino").performClick()
        rule.onNodeWithTag("concluir").performClick()
        rule.onNodeWithTag("salvar").performClick()
        rule.onNodeWithText("Informe o nome do treino.").assertExists()
    }

    @Test
    fun voltarComSelecaoPedeConfirmacaoDeDescarte() {
        rule.onNodeWithTag("fab_criar").performClick()
        searchAndExpand("Prancha", "prancha")
        rule.onNodeWithText("Adicionar ao treino").performClick()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        rule.onNodeWithText("Descartar").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("fab_criar"), 5_000)
    }

    @Test
    fun iniciarTreino_mostraDadosMarcaFeitoEAlteraCarga() {
        val name = "Sessao $suffix"
        createWorkout(name, "leg-press", "prancha")
        scrollHomeTo(name).performClick()

        rule.waitUntilAtLeastOneExists(hasTestTag("sessao-leg-press"), 5_000)
        rule.onNodeWithTag("sessao-leg-press").performClick()
        rule.onNodeWithContentDescription("Demonstração: Leg press").assertIsDisplayed()
        rule.onNodeWithText("Séries").assertExists()
        rule.onNodeWithText("Repetições").assertExists()

        // Digitar a carga substitui a anterior.
        rule.onNodeWithTag("carga-sessao-leg-press").performClick()
        rule.onNodeWithTag("campo_carga_dialogo").performTextClearance()
        rule.onNodeWithTag("campo_carga_dialogo").performTextInput("85")
        rule.onNodeWithText("Salvar").performClick()
        rule.waitUntilAtLeastOneExists(hasText("85 kg"), 5_000)

        rule.onNodeWithContentDescription("Aumentar carga").performClick()
        rule.waitUntilAtLeastOneExists(hasText("86 kg"), 5_000)

        rule.onNodeWithContentDescription("Marcar Leg press como feito").performClick()
        rule.onNodeWithText("1 de 2 feitos").assertExists()
        rule.onNodeWithContentDescription("Marcar Prancha como feito").performClick()
        rule.onNodeWithText("Treino concluído!").assertExists()

        // A carga nova ficou salva no banco.
        val weight = runBlocking {
            container.database.workoutExerciseDao().getAll().first { it.exerciseId == "leg-press" && it.weightKg == 86.0 }
        }
        assertEquals(86.0, weight.weightKg!!, 0.0)
    }

    @Test
    fun editarTreino_removeReordenaComToqueLongoESalva() {
        val name = "Edicao $suffix"
        val id = createWorkout(name, "puxada-frontal-aberta", "remada-baixa", "rosca-martelo")
        scrollHomeTo(name).performTouchInput { longClick() }
        rule.onNodeWithContentDescription("Editar $name").performClick()

        rule.waitUntilAtLeastOneExists(hasTestTag("lista_form"), 5_000)
        scrollFormTo("item-rosca-martelo")
        rule.onNodeWithContentDescription("Remover Rosca martelo").performClick()
        rule.onNodeWithTag("item-rosca-martelo").assertDoesNotExist()

        // Toque longo na alça e arrasta para cima.
        scrollFormTo("item-puxada-frontal-aberta")
        val top = rule.onNodeWithTag("item-puxada-frontal-aberta").fetchSemanticsNode().boundsInRoot.top
        rule.onNodeWithTag("alca-remada-baixa").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            repeat(20) {
                moveBy(Offset(0f, -40f))
                advanceEventTime(30)
            }
            up()
        }
        rule.waitForIdle()
        val newTop = rule.onNodeWithTag("item-remada-baixa").fetchSemanticsNode().boundsInRoot.top
        assertTrue("remada-baixa deveria estar acima ($newTop < $top)", newTop <= top + 1f)

        rule.onNodeWithTag("salvar").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("fab_criar"), 5_000)

        val saved = runBlocking { container.workoutRepository.loadDraft(id)!! }
        assertEquals(listOf("remada-baixa", "puxada-frontal-aberta"), saved.items.map { it.exerciseId })
    }

    @Test
    fun editarSemSalvarNaoAlteraNada() {
        val name = "Sem salvar $suffix"
        val id = createWorkout(name, "leg-press", "prancha")
        scrollHomeTo(name).performTouchInput { longClick() }
        rule.onNodeWithContentDescription("Editar $name").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("lista_form"), 5_000)
        scrollFormTo("item-prancha")
        rule.onNodeWithContentDescription("Remover Prancha").performClick()
        rule.onNodeWithContentDescription("Voltar").performClick()
        rule.onNodeWithText("Descartar").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("fab_criar"), 5_000)

        val saved = runBlocking { container.workoutRepository.loadDraft(id)!! }
        assertEquals(2, saved.items.size)
    }
}
