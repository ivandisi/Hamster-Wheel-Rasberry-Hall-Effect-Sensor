package it.id.pistacchio.net

import it.id.pistacchio.net.model.DataByYear
import it.id.pistacchio.net.model.DataModel
import it.id.pistacchio.net.model.SpeedModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GetService {

    @GET("/getByDay")
    suspend fun getByDay(@Query("day") day: String): Response<DataByYear>
    @GET("/getMaxSpeed")
    suspend fun getSpeedByDay(@Query("day") day: String): Response<SpeedModel>
    @GET("/getByYear")
    suspend fun getByYear(@Query("year") year: String): Response<DataByYear>
}