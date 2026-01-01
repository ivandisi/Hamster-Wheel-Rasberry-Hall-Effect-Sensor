package it.id.pistacchio.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import it.id.pistacchio.view.generic.Header
import it.id.pistacchio.view.generic.RecapView
import it.id.pistacchio.viewmodel.DailySearchViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryView(viewModel: DailySearchViewModel = viewModel()) {
    val cs = rememberCalendarState()

    Box(modifier = Modifier.fillMaxSize()) {

        Column {
            Header()

            StaticCalendar(
                calendarState = cs,
                dayContent = { dayState ->
                    Box(
                        modifier = Modifier
                            .clickable {
                                viewModel.updateSelection(dayState.date)
                            }
                            .background(
                                if (dayState.date == viewModel.selectedDate.value)
                                    Color.Gray
                                else Color.Transparent
                            )
                            .padding(8.dp)
                    ) {
                        Text(dayState.date.dayOfMonth.toString())
                    }
                })
            Text(
                "Searching for: ${viewModel.selectedDate.value}",
                modifier = Modifier
                    .padding(10.dp)
            )
            RecapView(viewModel)
        }

        if (viewModel.isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}