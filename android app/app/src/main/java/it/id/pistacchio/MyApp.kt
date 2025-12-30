package it.id.pistacchio

import android.app.Application
import it.id.pistacchio.net.MyApi

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MyApi.init(this)
    }
}