package dev.canem.bluecheckers.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.canem.bluecheckers.R

private data class RulesSection(val title: String, val body: String)

private val SECTIONS = listOf(
    RulesSection(
        title = "Board & setup",
        body = "8×8 board. Play happens only on the 32 dark squares. White moves first. " +
            "The long dark diagonal sits on each player's left, so a dark square is in " +
            "your bottom-left corner. 12 men per side fill the three rows closest to each player.",
    ),
    RulesSection(
        title = "Men (pedras)",
        body = "Move one square diagonally forward. Capture by jumping an adjacent enemy " +
            "onto the empty square beyond — forward or backward. Captures chain: you must keep " +
            "going while another capture is available with the same piece. The chain may turn " +
            "between jumps. No piece may be jumped twice in one chain.",
    ),
    RulesSection(
        title = "Kings (damas) — flying",
        body = "Move any number of empty squares diagonally, any direction. Capture at a " +
            "distance: jump exactly one enemy with any number of empty squares before and " +
            "after, landing on any empty square beyond the captured piece. Chain may change " +
            "direction between jumps.",
    ),
    RulesSection(
        title = "Mandatory capture & lei da maioria",
        body = "If any capture is available, you must capture. Among available capture " +
            "sequences, you must pick one that takes the maximum number of pieces. Brazilian " +
            "rules use the quantity rule only — kings and men count the same.",
    ),
    RulesSection(
        title = "Capture timing",
        body = "Captured pieces stay on the board as blockers until your move ends. They " +
            "cannot be jumped a second time, and a king cannot pass through or land on a " +
            "square holding a not-yet-removed captured piece.",
    ),
    RulesSection(
        title = "Promotion",
        body = "A man becomes a king when it stops on the opponent's back rank. A man that " +
            "merely passes through the back rank during a multi-capture does NOT promote — it " +
            "continues under man-movement rules. A newly minted king may not capture on the " +
            "same turn it was promoted.",
    ),
    RulesSection(
        title = "Win / draw",
        body = "Win by capturing all of the opponent's pieces, or by leaving them with no " +
            "legal move. Draw on three-fold repetition or after 20 consecutive king-only " +
            "plies with no capture and no man movement (CBD default; FMJD-64 tournament " +
            "counters available in Settings).",
    ),
)

@Composable
fun RulesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.rules_title), style = MaterialTheme.typography.headlineSmall)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (s in SECTIONS) {
                Text(s.title, style = MaterialTheme.typography.titleMedium)
                Text(s.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}
