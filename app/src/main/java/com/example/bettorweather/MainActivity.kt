package com.example.bettorweather

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bettorweather.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding //valの代わりに使用
    private var WeatherText ="";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getWeather()
        Thread.sleep(3000)
        binding.textViewWeather.text = WeatherText

    }

    private fun ChangeUnixTime(unixTime: String): String {
        var timeform = SimpleDateFormat("HH")
        var timetmp = Date(unixTime.toInt() * 1000L)
        var nowTime = timeform.format(timetmp)
        return nowTime
    }

    private fun getWeather(): Job { //Job型を返しGlobalScopeで同期させる
        return GlobalScope.launch{
            WeatherText = ""
            var API_KEY = "074778ef9b933a2b81d72930de64a034"
            var API_URL = "http://api.openweathermap.org/data/2.5/forecast?" +
                    "q=Nagoya&lang=" + "ja" + "&" + "APPID=" + API_KEY
            var apiurl = URL(API_URL)
            var info = BufferedReader(InputStreamReader(apiurl.openStream()))
            var str = info.readText()
            var json = JSONObject(str)

            var timeobj = json.getJSONArray("list").getJSONObject(0)
            var time =timeobj.getString("dt")

            var changeTime = ChangeUnixTime(time).toInt()

            var tomorrow = ((36 - changeTime) / 3).toInt()

            var obj = json.getJSONArray("list").getJSONObject(tomorrow)
            var obj2 = obj.getJSONArray("weather").getJSONObject(0)
            var next_weather = obj2.getString("description")
            WeatherText = next_weather


        }
    }
}