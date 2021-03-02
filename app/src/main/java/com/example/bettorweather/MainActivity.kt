package com.example.bettorweather

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bettorweather.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding //valの代わりに使用
    private var WeatherTextFut =""
    private var WeatherTextPas =""
    private var Money:Int = 500

    private var yesterday = ""

    private var Odds_sun = 1
    private var Odds_cloud = 1
    private var Odds_rain = 1
    private var Odds_snow = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getWeatherFut()
        getWeatherPas()
        Thread.sleep(3000)
        binding.textViewResultVal.text = WeatherTextPas
        binding.textViewWeather.text = WeatherTextFut
        binding.textViewMoney.text = Money.toString()
        binding.textViewResultDate.text = yesterday


        binding.buttonSun.setOnClickListener {
            vote("Sun")
        }
        binding.buttonCloud.setOnClickListener {
            vote("Cloud")
        }
        binding.buttonRain.setOnClickListener {
            vote("Rain")
        }
        binding.buttonSnow.setOnClickListener {
            vote("Snow")
        }

    }

    private fun ChangeUnixTime(unixTime: String): String {
        var timeform = SimpleDateFormat("HH")
        var timetmp = Date(unixTime.toInt() * 1000L)
        var nowTime = timeform.format(timetmp)
        return nowTime
    }

    private fun getWeatherFut(): Job { //Job型を返しGlobalScopeで同期させる
        return GlobalScope.launch{
            WeatherTextFut = ""
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
            WeatherTextFut = next_weather
        }
    }

    private fun getWeatherPas(): Job { //Job型を返しGlobalScopeで同期させる
        return GlobalScope.launch{
            val localDate = LocalDateTime.now()
            var localDateTime:LocalDateTime = if(localDate.hour < 12){
                localDate.minusDays(1).withHour(12)
                        .truncatedTo(ChronoUnit.HOURS)
            }else{
                localDate.withHour(12)
                        .truncatedTo(ChronoUnit.HOURS)
            }
            val dt_Zone = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Tokyo"))
            val dt: Long = dt_Zone.toEpochSecond()
            WeatherTextPas = ""
            var API_KEY = "074778ef9b933a2b81d72930de64a034"
            var API_URL = "https://api.openweathermap.org/data/2.5/onecall/timemachine?" +
                    "lat=35.185587&lon=136.899091&dt=" + dt + "&lang=ja&APPID=" + API_KEY
            var apiurl = URL(API_URL)
            var info = BufferedReader(InputStreamReader(apiurl.openStream()))
            var str = info.readText()
            var json = JSONObject(str)

            var obj = json.getJSONObject("current")
            var obj2 = obj.getJSONArray("weather").getJSONObject(0)
            var pre_weather = obj2.getString("description")

            yesterday = dt_Zone.monthValue.toString() + "月" + dt_Zone.dayOfMonth + "日"
            WeatherTextPas = pre_weather
        }
    }

    private fun disclickable(){
        binding.buttonSnow.isClickable = false
        binding.buttonSun.isClickable = false
        binding.buttonCloud.isClickable = false
        binding.buttonRain.isClickable = false
        binding.buttonSnow.isEnabled = false
        binding.buttonSun.isEnabled = false
        binding.buttonCloud.isEnabled = false
        binding.buttonRain.isEnabled = false
    }

    private fun vote(wheather: String) {
        var return_val:Int = 0

        val betmoney = arrayOf<String>("1", "10", "100", "500")
        val builder = AlertDialog.Builder(this)
                .setTitle("Will you bet to$wheather ?")
                .setPositiveButton("Bet!"){dialog, which ->
                    binding.textViewMoney.text = (Money - return_val).toString()
                    Money -= return_val
                    disclickable()
                }
                .setNegativeButton("Cancel"){dialog, which ->
                }
                .setSingleChoiceItems(betmoney, 0) { dialog, which ->
            when (which) {
                0 -> return_val = 1
                1 -> return_val = 10
                2 -> return_val = 100
                3 -> return_val = 500
            }
                }
                .show()
    }
}