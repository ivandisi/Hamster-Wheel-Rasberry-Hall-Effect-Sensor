package it.id.pistacchio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import it.id.pistacchio.ui.theme.MyApplicationTheme
import it.id.pistacchio.view.DailyLengthView
import it.id.pistacchio.view.DailyView
import it.id.pistacchio.view.HistoryView
import it.id.pistacchio.view.HomeView
import it.id.pistacchio.view.YearView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> {
                    HomeView()
                }
                AppDestinations.TODAY -> {
                    DailyView()
                }
                AppDestinations.TODAY_LENGTH -> {
                    DailyLengthView()
                }
                AppDestinations.YEAR -> {
                    YearView()
                }
                AppDestinations.HISTORY -> {
                    HistoryView()
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Recap", Icons.Default.Home),
    TODAY("Today", Icons.Default.Refresh),
    TODAY_LENGTH("Length", Icons.Default.Info),
    HISTORY("History", Icons.Default.DateRange),
    YEAR("Year", Icons.Default.DateRange)
}

