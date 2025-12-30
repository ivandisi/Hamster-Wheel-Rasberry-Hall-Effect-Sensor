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
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailyLengthViewModel : ViewModel() {

    private val _dataList = mutableStateOf<List<Bars>>(emptyList())
    private var _totalLength = mutableStateOf<Float>(0.0F)
    private var _avgTrips = mutableStateOf<Int>(0)
    val dataList: State<List<Bars>> = _dataList
    private var totalTrip = 0

    init {
        _dataList.value = listOf(
            Bars(
                "N/A",
                listOf(
                    Bars.Data(label = "Length", value = 0.0, color = SolidColor(Color.Gray))
                    )
            )
        )
        fetchDataFromApi()
    }

    fun fetchDataFromApi() {
        viewModelScope.launch {
            val result = loadData()
            if (result != null && result.isSuccessful) {
                val data: DataByYear? = result.body()
                if (data != null) {
                    _totalLength.value = 0.0F
                    _avgTrips.value = 0
                    totalTrip = 0

                    val mydata = arrayListOf<Bars>()

                    for ((index, item) in data.withIndex()) {
                        _totalLength.value += (item.length.toFloat() / 100000)

                        mydata.add(
                            Bars(
                                Constants.Support.HOURS.get(item.hour.toInt()),
                                listOf(
                                        Bars.Data(
                                        label = "Length in meter",
                                        value = item.length.toDouble() / 100,
                                        color = SolidColor(Color.Blue)
                                    ),
                                ))
                        )
                    }

                    _dataList.value = mydata

                }

            }
        }
    }

    suspend fun loadData(): Response<DataByYear>? {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formattedDate = today.format(formatter)

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