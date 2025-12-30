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

class YearViewModel : ViewModel() {

    private val _dataList = mutableStateOf<List<Bars>>(emptyList())
    val dataList: State<List<Bars>> = _dataList

    init {
        _dataList.value = listOf(
            Bars(
                "N/A",
                listOf(Bars.Data(label = "Trips", value = 0.0, color = SolidColor(Color.Gray)))
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

                    val mydata = arrayListOf<Bars>()

                    for ((index, item) in data.withIndex()) {
                        mydata.add(
                            Bars(
                                Constants.Support.MONTHS.get(item.month.toInt()-1),
                                listOf(Bars.Data(
                                    label = "Trips",
                                    value = item.trips.toDouble(),
                                    color = SolidColor(Color.Red)
                                )
                                ))
                        )
                    }
                    _dataList.value = mydata
                }

            }
        }
    }

    suspend fun loadData(): Response<DataByYear>? {
        val service = MyApi.instance

        try {
            val byYear = service.getByYear(LocalDate.now().year.toString())
            return byYear

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

}