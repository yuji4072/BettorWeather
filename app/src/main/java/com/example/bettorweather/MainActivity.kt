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

    //過去の天気と天気予報を格納する変数
    private var WeatherTextFut =""
    private var WeatherTextPas =""

    //ユーザーの所持通貨（今回はデータベースまで実装できなかったので固定500スタート)
    private var Money:Int = 500

    //昨日の日時を覚える変数
    private var yesterday = ""

    //今回は実装できなかったが、データベースでオッズをいじって表示させたかった
    private var Odds_sun = 1
    private var Odds_cloud = 1
    private var Odds_rain = 1
    private var Odds_snow = 1

    //昨日の当選オッズを記憶する変数
    private var Odds_yesterday = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        //DataBindを使用して簡単に参照できるようにした
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //3000ms待つ間に描画関係を整える
        getWeatherFut()
        getWeatherPas()
        Thread.sleep(3000)
        binding.textViewResultVal.text = WeatherTextPas
        binding.textViewWeather.text = WeatherTextFut
        binding.textViewMoney.text = Money.toString()
        binding.textViewResultDate.text = yesterday
        //binding.textViewGuessVal.text = "昨日賭けた天気"

        //昨日の結果が当たっているかどうか判定している
        val contain = Regex(WeatherTextPas)
        if(!contain.containsMatchIn(binding.textViewGuessVal.text)){
            binding.buttonReward.isClickable = false
            binding.buttonReward.isEnabled = false
        }

        //それぞれのボタンの挙動を指定している
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
        binding.buttonReward.setOnClickListener {
            //Money = Money + "昨日賭けた金額"*Odds_yesterday
            //binding.textViewMoney.text = Money.toString()
        }

    }

    //epoch秒と日時を変換して扱いやすくする
    private fun ChangeUnixTime(unixTime: String): String {
        var timeform = SimpleDateFormat("HH") //時間のみを取得する
        var timetmp = Date(unixTime.toInt() * 1000L) //Kotlinの記法
        var nowTime = timeform.format(timetmp) //フォームに従ってunixタイムを時間に変える
        return nowTime
    }

    //apiをたたく関数
    private fun getWeatherFut(): Job { //Job型を返しGlobalScopeで同期させる
        return GlobalScope.launch{
            WeatherTextFut = ""
            var API_KEY = "各自のAPIKEY" //APIKEYを指定
            var API_URL = "http://api.openweathermap.org/data/2.5/forecast?" +
                    "q=Nagoya&lang=" + "ja" + "&" + "APPID=" + API_KEY //クエリを指定

            //APIに投げる
            var apiurl = URL(API_URL) //StringからURLオブジェクトを生成
            var info = BufferedReader(InputStreamReader(apiurl.openStream())) //テキストをまとめて読み込み、バイトをテキストに
            var str = info.readText() //文字列として扱えるように
            var json = JSONObject(str) //Jsonオブジェクトにする

            //帰ってきたものを加工していく
            var timeobj = json.getJSONArray("list").getJSONObject(0)
            var time =timeobj.getString("dt")

            //OpenWeatherMapはUnix秒で管理しているので、日時に直す
            var changeTime = ChangeUnixTime(time).toInt()

            //次に0時を超えた正午を"明日"として、明日を指定する
            var tomorrow = ((36 - changeTime) / 3).toInt()

            //"明日"の天気を取得して変数に投げる
            var obj = json.getJSONArray("list").getJSONObject(tomorrow)
            var obj2 = obj.getJSONArray("weather").getJSONObject(0)
            var next_weather = obj2.getString("description")
            WeatherTextFut = next_weather
        }
    }

    //同じくApiをたたいて過去の天気を知る
    private fun getWeatherPas(): Job { //Job型を返しGlobalScopeで同期させる
        return GlobalScope.launch{

            //前回の判定まで時間を巻き戻す(api levelが26必要)
            val localDate = LocalDateTime.now() //現在の時間を日時で取得する
            var localDateTime:LocalDateTime = if(localDate.hour < 12){ //前回の判定まで巻き戻すために、日時に分岐を入れる
                localDate.minusDays(1).withHour(12)
                        .truncatedTo(ChronoUnit.HOURS) //分以下を全て0に
            }else{
                localDate.withHour(12)
                        .truncatedTo(ChronoUnit.HOURS)
            }

            //時刻をZoneDateTimeに直し、さらにそれをEpoch秒に直す
            val dt_Zone = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Tokyo"))
            val dt: Long = dt_Zone.toEpochSecond()

            //ここからまたApiをたたいていく(ほとんど手順は同じ)
            WeatherTextPas = ""
            var API_KEY = "各自のAPIKEY" //(ここも各自のAPIKEY）
            var API_URL = "https://api.openweathermap.org/data/2.5/onecall/timemachine?" +
                    "lat=35.185587&lon=136.899091&dt=" + dt + "&lang=ja&APPID=" + API_KEY
            var apiurl = URL(API_URL)
            var info = BufferedReader(InputStreamReader(apiurl.openStream()))
            var str = info.readText()
            var json = JSONObject(str)

            var obj = json.getJSONObject("current")
            var obj2 = obj.getJSONArray("weather").getJSONObject(0)
            var pre_weather = obj2.getString("main")

            //今回は取得した天気だけではなく、判定した日の情報も返す
            yesterday = dt_Zone.monthValue.toString() + "月" + dt_Zone.dayOfMonth + "日"
            WeatherTextPas = pre_weather
        }
    }

    //1度betしたあとはもう一度クリックできないようにする(本当は一日中そうしていたいが、アプリを落とすと復活してしまう)
    private fun disclickable(){
        binding.buttonSnow.isClickable = false //クリックできないように
        binding.buttonSun.isClickable = false
        binding.buttonCloud.isClickable = false
        binding.buttonRain.isClickable = false
        binding.buttonSnow.isEnabled = false //色をクリックできないような色に
        binding.buttonSun.isEnabled = false
        binding.buttonCloud.isEnabled = false
        binding.buttonRain.isEnabled = false
    }

    //投票する関数
    private fun vote(wheather: String) { //どのボタンが押されたかを記憶
        var return_val:Int = 0

        //今回はbetできる値を4つに指定（本当はシークバーで扱いたかった）
        val betmoney = arrayOf<String>("1", "10", "100", "500")

        //AlertDialogというサブクラスをしようしてダイアログを実装していく
        AlertDialog.Builder(this)
                .setTitle("Will you bet to $wheather ?") //タイトル
                .setPositiveButton("Bet!"){dialog, which -> //bet用ボタン
                    binding.textViewMoney.text = (Money - return_val).toString() //betをするとお金が減る
                    Money -= return_val
                    disclickable() //クリックできなくなる関数を呼ぶ
                }
                .setNegativeButton("Cancel"){dialog, which -> //cancel用ボタン（飾り）
                }
                .setSingleChoiceItems(betmoney, 0) { dialog, which -> //リストを実装する
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
