package com.example.sindanforandroid

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var greeting = "SINDAN エージェントプロトタイプ"
        val isconnect = isWifiAvailable(this)
        if (isconnect == true) {
            textview.text = "connected"
        } else {
            textview.text = "disconnected"
        } // ここまでは正しく動いているっぽい
        textview2.text = GetWifiEnvironment(this)
        // 速度は取れた
        textview3.text = GetSpeed(this) + WifiInfo.LINK_SPEED_UNITS
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId) {
            R.id.setting -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // MainActivity 内にあるのもおかしい？
    private fun isWifiAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            // NetworkCapabilities という名前の構造体（単数構造体の複数形ではない）単体でコレクション風
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            // return true only　when wifi connection is established.
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                // actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                // actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                // actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            // for android 4.4 and earlier(broken?)
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }
    //ToDo: 正しい SSID を取れていない / Manifest いじったのにまだだめ
    //ToDo: データ取得をどうするか。リスト返してもらったほうがいいけど、GC とかちゃんとあるのかな
    private fun GetWifiEnvironment(context: Context): String{
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager;

        // API10 からアプリからの wifi on/off できなくなった
        // Wifi の設定画面を出して、enabled になるまで待つ、という処理が必要
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(context, "WIFI ON", Toast.LENGTH_SHORT).show()
            wifiManager.setWifiEnabled(true);
        }
        if(!wifiManager.isWifiEnabled) return "";
        // connectInfo まで取れてそう、IP アドレスは正しい
        val connectInfo = wifiManager.connectionInfo
        //
        val state = WifiInfo.getDetailedStateOf(connectInfo?.supplicantState)
        return wifiManager.connectionInfo.ssid
    }

    private fun GetSpeed(context: Context): String{
        val wifiManager: WifiManager = getSystemService(WIFI_SERVICE) as WifiManager;
        val connectInfo = wifiManager.connectionInfo
        val state = WifiInfo.getDetailedStateOf(connectInfo?.supplicantState)
        return connectInfo.linkSpeed.toString()
    }


}
