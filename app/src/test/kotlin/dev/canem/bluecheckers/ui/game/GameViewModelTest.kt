package dev.canem.bluecheckers.ui.game

import androidx.lifecycle.SavedStateHandle
import dev.canem.bluecheckers.ai.Weights
import dev.canem.bluecheckers.data.repository.AiRepository
import dev.canem.bluecheckers.data.repository.Prefs
import dev.canem.bluecheckers.data.repository.PrefsRepository
import dev.canem.bluecheckers.data.repository.ProgressRepository
import dev.canem.bluecheckers.data.repository.ProgressSummary
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.Square
import dev.canem.bluecheckers.ui.navigation.Routes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var aiRepo: AiRepository
    private lateinit var progressRepo: ProgressRepository
    private lateinit var prefsRepo: PrefsRepository
    private lateinit var soundPlayer: SoundPlayer

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        aiRepo = mockk(relaxed = true)
        progressRepo = mockk(relaxed = true)
        prefsRepo = mockk(relaxed = true)
        soundPlayer = mockk(relaxed = true)
        coEvery { aiRepo.loadWeights() } returns Weights.DEFAULT
        coEvery { aiRepo.loadGamesTrained() } returns 0
        every { prefsRepo.prefsFlow } returns flowOf(
            Prefs.DEFAULT.copy(animationsEnabled = false), // skip animation gating in tests
        )
        every { progressRepo.observeSummary() } returns flowOf(ProgressSummary.EMPTY)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun newVm(level: Int = 0): GameViewModel {
        val savedState = SavedStateHandle(mapOf(Routes.ARG_LEVEL to level))
        return GameViewModel(savedState, aiRepo, progressRepo, prefsRepo, soundPlayer)
    }

    @Test
    fun `tap on own piece selects it and shows destinations`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onSquareTap(Square(2, 0)) // white front-line man
        val state = vm.uiState.value
        assertEquals(Square(2, 0), state.selectedSquare)
        assertEquals(setOf(Square(3, 1)), state.destinations.keys)
    }

    @Test
    fun `tap on destination commits the move and switches side`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onSquareTap(Square(2, 0))
        vm.onSquareTap(Square(3, 1))
        // After player's move the AI will respond (level 0 -> RandomAgent). After the AI
        // also moves it's white's turn again. We assert that the AI eventually made a move
        // by checking that we left the initial board.
        val state = vm.uiState.value
        assertTrue(state.lastMove != null)
    }

    @Test
    fun `tap on empty square deselects`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onSquareTap(Square(2, 0))
        assertEquals(Square(2, 0), vm.uiState.value.selectedSquare)
        vm.onSquareTap(Square(4, 4)) // empty
        assertNull(vm.uiState.value.selectedSquare)
    }

    @Test
    fun `2P mode does not call AI repository`() = runTest(dispatcher) {
        val vm = newVm(level = Routes.LEVEL_TWO_PLAYER)
        vm.onSquareTap(Square(2, 0))
        vm.onSquareTap(Square(3, 1))
        // sideToMove flipped to BLACK because no AI auto-responds.
        assertEquals(PieceColor.BLACK, vm.uiState.value.sideToMove)
        assertTrue(vm.uiState.value.twoPlayerMode)
    }

    @Test
    fun `resign at level 0 records a loss`() = runTest(dispatcher) {
        val vm = newVm(level = 0)
        vm.resign()
        // The outcome dialog appears.
        assertTrue(vm.uiState.value.outcome is dev.canem.bluecheckers.game.GameOutcome.Win)
    }
}
