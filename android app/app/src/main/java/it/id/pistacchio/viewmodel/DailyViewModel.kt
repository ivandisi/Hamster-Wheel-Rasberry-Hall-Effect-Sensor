package it.id.pistacchio.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ehsannarmani.compose_charts.models.Bars
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import it.id.pistacchio.Constants
import it.id.pistacchio.net.MyApi
import it.id.pistacchio.net.model.DataByYear
import it.id.pistacchio.net.model.SpeedModel
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailyViewModel : ViewModel() {

    private val _dataList = mutableStateOf<List<Bars>>(emptyList())
    private var _totalLength = mutableStateOf<Float>(0.0F)
    private var _avgTrips = mutableStateOf<Int>(0)
    private var _mostIntenseHour = mutableStateOf<Int>(0)
    private var _intMostIntenseHour = mutableStateOf<Int>(0)
    private var _speed = mutableStateOf<SpeedModel>(SpeedModel())
    val dataList: State<List<Bars>> = _dataList
    var totalLength: State<Float> = _totalLength
    var avgTrips: State<Int> = _avgTrips
    var intMostIntenseHour: State<Int> = _intMostIntenseHour
    var mostIntenseHour: State<Int> = _mostIntenseHour
    var speed: State<SpeedModel> = _speed
    private var totalTrip = 0

    init {
        _dataList.value = listOf(
            Bars(
                "N/A",
                listOf(
                    Bars.Data(label = "Trips", value = 0.0, color = SolidColor(Color.Gray)),
                    )
            )
        )
        fetchDataFromApi()
    }

    fun fetchDataFromApi() {
        viewModelScope.launch {
            val result = loadData()
            if (result.isSuccessful) {
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

            val speed = loadSpeed()
            if (speed.isSuccessful && speed.body() != null) {
                _speed.value = speed.body()!!
            }
        }
    }

    suspend fun loadSpeed(): Response<SpeedModel> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formattedDate = today.format(formatter)

        val service = MyApi.instance
        val speed = service.getSpeedByDay(formattedDate)
        return speed
    }

    suspend fun loadData(): Response<DataByYear> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formattedDate = today.format(formatter)

        val service = MyApi.instance
        val byDay = service.getByDay(formattedDate)
        return byDay
    }

}