package it.id.pistacchio.view.generic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import it.id.pistacchio.Constants
import it.id.pistacchio.viewmodel.DailyLengthViewModel
import it.id.pistacchio.viewmodel.DailyViewModel
import it.id.pistacchio.viewmodel.YearViewModel
import java.util.Locale

@Composable
fun RecapView(viewModel: DailyViewModel = viewModel()) {
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
                text = "Today distance run:",
                modifier = Modifier
                    .padding(12.dp, 0.dp)
                    .wrapContentHeight()
            )
            Text(
                text = "${viewModel.totalLength.value.let {
                    String.format(Locale.getDefault(), "%.2f", it)
                }} km",
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
                text = "${viewModel.avgTrips.value} trips/h",
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
                text = "${viewModel.speed.value.speed} ${viewModel.speed.value.speedKM}",
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
fun DailyLengthRowChart(modifier: Modifier = Modifier, viewModel: DailyLengthViewModel = viewModel()) {
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
fun Header() {
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(35.dp)
        .background(Color.Gray))
}
