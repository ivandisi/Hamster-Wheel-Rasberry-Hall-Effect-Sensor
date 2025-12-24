package it.id.pistacchio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import it.id.pistacchio.ui.theme.MyApplicationTheme
import it.id.pistacchio.viewmodel.DailyLengthViewModel
import it.id.pistacchio.viewmodel.DailyViewModel
import it.id.pistacchio.viewmodel.YearViewModel

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
    YEAR("Year", Icons.Default.DateRange)
}

@Composable
fun YearRowChart(modifier: Modifier = Modifier, viewModel: YearViewModel = viewModel()) {

    val dataList by viewModel.dataList

    RowChart(
        modifier = modifier,
        data = dataList,
        barProperties = BarProperties(
            spacing = 3.dp
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
    )
}

@Composable
fun DailyRowChart(modifier: Modifier = Modifier, viewModel: DailyViewModel = viewModel()) {
    val dataList by viewModel.dataList

    RowChart(
        modifier = modifier,
        data = dataList,
        barProperties = BarProperties(
            spacing = 3.dp
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
    )
}

@Composable
fun DailyLengtgRowChart(modifier: Modifier = Modifier, viewModel: DailyLengthViewModel = viewModel()) {
    val dataList by viewModel.dataList

    RowChart(
        modifier = modifier,
        data = dataList,
        barProperties = BarProperties(
            spacing = 3.dp
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
    )
}

@Composable
fun HomeView( viewModel: DailyViewModel = viewModel(), lengthViewModel: DailyLengthViewModel = viewModel(), yearViewModel: YearViewModel = viewModel() ) {
    Column {
        Spacer(modifier = Modifier.size(45.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .size(420.dp,205.dp)
                .padding(8.dp, 8.dp)
        ) {
            Row {
                IconButton(
                    onClick = {
                        viewModel.fetchDataFromApi()
                        lengthViewModel.fetchDataFromApi()
                        yearViewModel.fetchDataFromApi()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = stringResource(id = R.string.refresh),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(28.dp)
                    )
                }
                Text(
                    text = "Hello I'm Pistacchio !",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(12.dp)
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary,
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp,8.dp)

        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = "Today length runned:",
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )
                Text(
                    text = "${viewModel.totalLength.value} km",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = "Avg trips/hour (when wheel is used):",
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )
                Text(
                    text = "${viewModel.avgTrips.value} trips/H",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = "Most intense hour of gym:",
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )

                Text(
                    text = "${viewModel.mostIntenseHour.value} trips at ${
                        Constants.Support.HOURS.get(
                            viewModel.intMostIntenseHour.value
                        )
                    }",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )
                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = "Speed of the day:",
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )

                Text(
                    text = "${viewModel.speed.value.speed}${viewModel.speed.value.speedKM}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )
                Text(
                    text = "with ${viewModel.speed.value.deltaT}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(12.dp, 0.dp)
                        .wrapContentHeight()
                )
            }
        }

    }
}

@Composable
fun DailyView() {
    Column {
        Spacer(modifier = Modifier.size(45.dp))
        Text(
            text = "Today I'm doing ...",
            modifier = Modifier
                .padding(8.dp)
        )
        DailyRowChart(
            modifier = Modifier
                .padding(16.dp, 0.dp)

        )
    }
}

@Composable
fun DailyLengthView() {
    Column {
        Spacer(modifier = Modifier.size(45.dp))
        Text(
            text = "Today I'm running for ...",
            modifier = Modifier
                .padding(8.dp)
        )
        DailyLengtgRowChart(
            modifier = Modifier
                .padding(16.dp, 0.dp)

        )
    }
}
@Composable
fun YearView() {
    Column {
        Spacer(modifier = Modifier.size(45.dp))
        Text(
            text = "This year I'm doing ...",
            modifier = Modifier
                .padding(8.dp)
        )
        YearRowChart(
            modifier = Modifier
                .padding(8.dp)

        )
    }
}

