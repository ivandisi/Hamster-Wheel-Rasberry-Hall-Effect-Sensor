# Hamster Wheel Hall Effect Sensor

POC of a simple solution based on Rasberry PI with a Hall Effect Sensor to retrive metric from wheel trips 

* Local Network Server with simple API SET
* Local DB 
* Hall Effect Sensor data input
* Android APP to read data

## General info

The doc should provide all information needed to understand the methods, but if not feel free to add a issue in github and I'll address any questions! :)

### POC

Project is based on Rasberry PI board and AZ-Delivery KY-024
 
[<img src="https://www.az-delivery.de/cdn/shop/products/ky-024-linear-magnetic-hall-sensor-726599.jpg"
     alt="Go to the sensor"
     height="70">](https://www.az-delivery.de/it/products/hall-sensor-modul?srsltid=AfmBOoqa0LqeN9f1y9NHcd2nGP9CpNzuJWmSTXOFI5jDvWtwVy-QouF8)
[<img src="https://assets.raspberrypi.com/static/8f6eca535dbb23a21eb41a748050e3a0/33b96/16gb.webp"
     alt="Go to the board"
     height="70">](https://www.raspberrypi.com/products/raspberry-pi-5/)

Wheel size is 29CM, you have to know your wheel size, retrive some minor metric and IP to run it

## How to run

### To run the backend:

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
