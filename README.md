# Hamster Wheel Hall Effect Sensor

POC of a simple solution based on Rasberry PI with a Hall Effect Sensor to retrive metric from wheel trips 

* Local Server with simple API SET
* Local Server DB 
* Hall Effect Sensor data input
* Android APP to read data

## General info

The doc should provide all information needed to run the project, but if not feel free to add a issue in github and I'll address any questions! :)

### POC

Project is based on Rasberry PI Board and AZ-Delivery KY-024

<p align="center" >
  <img src="https://www.az-delivery.de/cdn/shop/products/ky-024-linear-magnetic-hall-sensor-726599.jpg"
       alt="Go to the sensor"
       height="100">
  <img src="https://assets.raspberrypi.com/static/8f6eca535dbb23a21eb41a748050e3a0/33b96/16gb.webp"
       alt="Go to the board"
       height="100">
</p>

### HARDWARE LINKS

* AZ-Delivery KY-024 -> [LINK](https://www.az-delivery.de/it/products/hall-sensor-modul?srsltid=AfmBOorUrREHiLzq2KTRPAs1MlyRB_Pjx9o7F6O_OREGu73n7KSz_wx4)

* Rasberry PI Board -> [LINK](https://www.raspberrypi.com/)


## How to run

### To run the backend:

Before start set the configuration about your wheel and the Digital PIN used


```python
  # Length of wheel, 29cm diameter (91 circumference)
  tripLength = 91
  # Sensor PIN
  Digital_PIN = 22
  # High level filter, max 1 trip in 200ms on a 29cm diameter wheel (realistic max hamster speed)
  MAX_ONE_IN = 0.2
```

**tripLength** (express in cm) is the circumference of the wheel

**MAX_ONE_IN** (express in seconds) is the max ms where only 1 wheel revolution must be present, it is used as high level filter for irregular bouncing, in my case I extimate a max speed of my hamster around 10KM/h so it is physical impossible to have more than 1 revolution in 200ms


```bash
  sudo python3 ~/pistacchio.py
```

Requirements:

```bash
  sudo apt update
  sudo apt install python3-gpiozero
  pip install tinydb
  pip install gpiozero
```

Check requirements:

```bash
  python3 -c "import tinydb, gpiozero; print('OK')"
```


### To run the Android APP:

Import it in Android Studio and run it

Before start it, please be sure to address the right IP in the Constants.kt

```kotlin
  object GetAPI {
        const val URL: String = "http://192.168.1.99:8000//"
    }
```

External Library used for Android Project

 ```gradle
    implementation("io.github.ehsannarmani:compose-charts:0.1.10")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("io.github.boguszpawlowski.composecalendar:composecalendar:1.4.0")
    implementation("io.github.boguszpawlowski.composecalendar:kotlinx-datetime:1.4.0")
```
    
<p align="center" >
Greetings from my hamster Sergente Pistacchio
</p>

<p align="center" >
<img src="https://github.com/ivandisi/Hamster-Wheel-Rasberry-Hall-Effect-Sensor/blob/main/other/pistacchio.jpg" height="600">
</p>


   
