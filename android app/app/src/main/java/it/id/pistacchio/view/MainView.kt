package it.id.pistacchio.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import it.id.pistacchio.R
import it.id.pistacchio.viewmodel.DailyLengthViewModel
import it.id.pistacchio.viewmodel.DailySearchViewModel
import it.id.pistacchio.viewmodel.DailyViewModel
import it.id.pistacchio.viewmodel.YearViewModel
import java.time.LocalDate




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
        RecapView(viewModel)
    }
}


@Composable
fun HistoryView(viewModel: DailySearchViewModel = viewModel()) {
    val cs = rememberCalendarState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column {
        Spacer(modifier = Modifier.size(45.dp))
        StaticCalendar(
            calendarState =  cs,
            dayContent = { dayState ->
            Box(
                modifier = Modifier
                    .clickable {
                        selectedDate = dayState.date
                        viewModel.fetchDataFromApi("" + selectedDate?.year + selectedDate?.month?.value?.let {
                            String.format("%02d", it)
                        } + selectedDate?.dayOfMonth?.let {
                            String.format("%02d", it)
                        })
                    }
                    .background(
                        if (dayState.date == selectedDate)
                            Color.Gray
                        else Color.Transparent
                    )
                    .padding(8.dp)
            ) {
                Text(dayState.date.dayOfMonth.toString())
            }
        })
        Text("Searching for: "+selectedDate?.year + selectedDate?.month?.value?.let {
            String.format("%02d", it)
        } + selectedDate?.dayOfMonth?.let {
            String.format("%02d", it)
        },
            modifier = Modifier
                .padding(10.dp))
        RecapView(viewModel)
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
        DailyLengthRowChart(
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