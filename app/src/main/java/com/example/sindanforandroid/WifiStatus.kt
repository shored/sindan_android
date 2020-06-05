package com.example.sindanforandroid

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager

// Singleton にはしない、update もしないので constructor が呼ばれた状態のみ保存
// primary constructor 付き class であれば最初から context が入れられる？
class WifiStatus (val context: Context){
    // この場合 connectivityManager は not null であることは保証されてるっぽい（kotlin の挙動）
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // getActiveNetwork は open fun getActiveNetwork(): Network? なので、NULL 許容、Null が返ってくることはある
    private val nw      = connectivityManager.activeNetwork
    // NetworkCapabilities という名前の構造体（単数構造体の複数形ではない）単体でコレクション風
    // 同様に ? なので Null が来ることはある
    private val actNw = connectivityManager.getNetworkCapabilities(nw)
}