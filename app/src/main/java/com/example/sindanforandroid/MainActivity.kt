package com.example.sindanforandroid

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
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
        }
        textview2.text = GetSSID()
    }

    private fun isWifiAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
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

    private fun GetSSID(): String{
        var ssid: String? = null
        val wifiManager: WifiManager = getSystemService(WIFI_SERVICE) as WifiManager;

        if (wifiManager.isWifiEnabled) {
            Toast.makeText(this, "WIFI OFF", Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled = false;
        } else {
            Toast.makeText(this, "WIFI ON", Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled = true;
        }
        if(!wifiManager.isWifiEnabled) return "";
        val connectInfo = wifiManager.connectionInfo
        val state = WifiInfo.getDetailedStateOf(connectInfo?.supplicantState)
        ssid = connectInfo.getSSID();
        return ssid
    }
}
