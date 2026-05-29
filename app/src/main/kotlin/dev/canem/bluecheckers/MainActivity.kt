package dev.canem.bluecheckers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.canem.bluecheckers.ui.navigation.BlueNavGraph
import dev.canem.bluecheckers.ui.theme.BlueCheckersTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlueCheckersTheme {
                BlueNavGraph()
            }
        }
    }
}
