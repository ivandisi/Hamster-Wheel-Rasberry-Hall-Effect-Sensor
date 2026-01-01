package it.id.pistacchio.viewmodel

import android.R
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ehsannarmani.compose_charts.models.Bars
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import it.id.pistacchio.Constants
import it.id.pistacchio.net.MyApi
import it.id.pistacchio.net.NoInternetException
import it.id.pistacchio.net.model.DataByYear
import it.id.pistacchio.net.model.SpeedModel
import kotlinx.serialization.descriptors.PrimitiveKind
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailySearchViewModel : DailyViewModel() {

    var _selectedDate = mutableStateOf<LocalDate?>(null)
    val selectedDate : State<LocalDate?> = _selectedDate
    var _selectedDateLabel = mutableStateOf<String>("")
    val selectedDateLabel : State<String> = _selectedDateLabel
    val formatter = DateTimeFormatter.ofPattern("yyyy MM dd")
    private var totalTrip = 0

    init {

        _selectedDate.value = LocalDate.now()
        _selectedDateLabel.value = formatter.format(_selectedDate.value)

        _dataList.value = listOf(
            Bars(
                "N/A",
                listOf(
                    Bars.Data(label = "Trips", value = 0.0, color = SolidColor(Color.Gray)),
                    )
            )
        )
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formattedDate = today.format(formatter)

        fetchDataFromApi(formattedDate)
    }

    fun updateSelection(dayState: LocalDate) {
        _selectedDate.value = dayState
        _selectedDateLabel.value =
            "" + _selectedDate?.value + " " + _selectedDate.value?.month?.value?.let {
                String.format("%02d", it)
            } + " " + _selectedDate.value?.dayOfMonth?.let {
                String.format("%02d", it)
            }
        fetchDataFromApi(
            "" + _selectedDate.value?.year
                    + _selectedDate.value?.month?.value?.let {
                String.format("%02d", it)
            } + _selectedDate.value?.dayOfMonth?.let {
                String.format("%02d", it)
            })
    }

    fun fetchDataFromApi(data: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = loadData(data)
            if (result != null && result.isSuccessful) {
                val data: DataByYear? = result.body()
                if (data != null) {
                    _totalLength.value = 0.0F
                    _avgTrips.value = 0
                    totalTrip = 0
                    _mostIntenseHour.value = 0

                    val mydata = arrayListOf<Bars>()

                    for ((index, item) in data.withIndex()) {
                        _totalLength.value += (item.length.toFloat() / 100000)

                        if (item.trips.toInt() > 0) {
                            _avgTrips.value += item.trips.toInt()
                            totalTrip++
                        }

                        if (_mostIntenseHour.value < item.trips.toInt()){
                            _mostIntenseHour.value = item.trips.toInt()
                            _intMostIntenseHour.value = index
                        }

                        mydata.add(
                            Bars(
                                Constants.Support.HOURS.get(item.hour.toInt()),
                                listOf(
                                        Bars.Data(
                                        label = "Trips",
                                        value = item.trips.toDouble(),
                                        color = SolidColor(Color.Red)
                                    ),
                                ))
                        )
                    }

                    _dataList.value = mydata

                    if(totalTrip != 0)
                        _avgTrips.value /= totalTrip
                }

            }

            val speed = loadSpeed(data)
            if (speed != null && speed.isSuccessful && speed.body() != null) {
                _speed.value = speed.body()!!
            }

            _isLoading.value = false

        }
    }

    suspend fun loadSpeed(formattedDate: String): Response<SpeedModel>? {

        val service = MyApi.instance

        try {
            val speed = service.getSpeedByDay(formattedDate)
            return speed

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    suspend fun loadData(formattedDate: String): Response<DataByYear>? {

        val service = MyApi.instance

        try {
            val byDay = service.getByDay(formattedDate)
            return byDay

        } catch (e: Exception) {
           e.printStackTrace()
        }
        return null
    }

}