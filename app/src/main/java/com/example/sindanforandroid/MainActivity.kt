package com.example.sindanforandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        measure.setOnClickListener{
            var diag = Diagnosis(this)
            AsyncHttpRequest().execute(diag)
        } */

        val measure = findViewById<Button>(R.id.measure)
        measure.setOnClickListener(MeasureClickListener())

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

    private inner class MeasureClickListener : View.OnClickListener {
        override fun onClick(p0: View?) {
            val measureDialog = DiagnosisDialog.newInstance("表示するメッセージ")
            measureDialog.show(supportFragmentManager, "DiagnosisDialog")

            //権限取得
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.ACCESS_NETWORK_STATE
                        ),1
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
/*
    inner class AsyncHttpRequest : AsyncTask<Diagnosis, Diagnosis, Void>()
    {
//        private var mainActivity: Activity ;
        override fun onPreExecute() {
            // open progress dialog
      //      text.setText("start")
            // change measure button unclickable
            measure.isClickable = false
            Thread.sleep(800)
        }
        // start diagnosis asynchronously
        // TODO: 何回も実行される？同期処理の使い方がおかしいかも
        override fun doInBackground(vararg params: Diagnosis?): Void? {
            var diag = params[0]
            diag?.startDiagnosis()
            AsyncHttpRequest().execute(diag)
            return null
        }
        override fun onProgressUpdate(vararg values: Diagnosis?) {
            super.onProgressUpdate(*values)
        }

        // called when a diagnosis finished
        override fun onPostExecute(result: Void?) {
            // change measure button clickable
            measure.isClickable = true

            // 取得した結果をテキストビューに入れちゃったり
    //        TextView tv =(TextView) mainActivity . findViewById (R.id.name);
    //        tv.setText(result)
        }

    }

 */
/*
    class DiagnosisDialog //空のコンストラクタ（DialogFragmentのお約束）
        : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = Dialog(activity!!)
//            dialog.setContentView(R.layout.dialog_measure)
            dialog.setCancelable(false)
            dialog.setTitle("@string/measuring")
            return dialog
        }

        companion object {
            //インスタンス作成
            fun newInstance(): DiagnosisDialog {
                return DiagnosisDialog()
            }
        }
    }
    */
    /*
    fun onMeasureButtonTapped(view: View?): Boolean {
        var diag = Diagnosis(this)
        diag.startDiagnosis()

        return true
    }

     */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId) {
            R.id.setting -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // MainActivity 内にあるのはおかしい
    // Diagnosis に移動すべき
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

        // need to grant access permission to location service
        // otherwise, wifiManager.connectionInfo.ssid returns 'unknown_ssid'
        val connectInfo = wifiManager.connectionInfo
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
