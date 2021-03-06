package com.example.sindanforandroid

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.provider.Settings.Global.getString
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.google.gson.Gson
//import com.sun.xml.internal.ws.streaming.XMLStreamWriterUtil.getOutputStream
import org.json.JSONObject
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// TODO: 検討: inner class にする？ -> kotlin では data は inner にできない模様。
// お作法的に違う？ただ setter/getter 以外に何か必要そうには見えない
// layer, log_group, log_type, log_campaign_uuid, result, target, detail, occurred_at
data class DiagResult(val layer: String, val log_group: String, val log_type: String,
                      val log_campaign_uuid: UUID, val result: String, val target: String,
                      val detail: String, val occurred_at: String) {
}

/*
log_campaign_uuid:  "25838fa6-1be2-4cf5-bf6c-994d24852b6e"
mac_addr:           "00:11:22:AA:BB:CC"
os:                 "Android"
network_type:       ""
ssid:               "eduroam"
version:            "0.1.1"
occurred_at:        "2020-05-16 14:06:30"
*/

//data class 型である必要はないかも
data class LogCampaign(val log_campaign_uuid: String, val mac_addr: String, val os: String,
                       val network_type: String,
                       val ssid: String, val version: String,
                       val occurred_at: String) {

}

//簡単なセッターがあればいい気がする
//TODO: Diagnosis 自体に log_campaign を示すメンバを入れる
class Diagnosis constructor(val context: Context) {


    private var diag_results = mutableListOf<DiagResult>()
    private val log_campaign_uuid = UUID.randomUUID()
    private val resource = context.resources
    private val resInfo: String = resource.getInteger(R.integer.res_info).toString()
    private val resSuccess: String = resource.getInteger(R.integer.res_success).toString()
    private val resFail: String = resource.getInteger(R.integer.res_fail).toString()
    private val manager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    // Use the currentNetwork and linkProperty during diagnosis startup.
    private val currentNetwork = manager.activeNetwork
    private val lp = manager.getLinkProperties(currentNetwork)

    /*
    init {
    }
     */

    fun startDiagnosis() {
        phase1()
        phase2()
//        phase3()
//        phase4()
//        phase5()
        uploadResults()
        uploadLogCampaign()
    }

    private fun getSpeed(context: Context): String{
        val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager;
        val connectInfo = wifiManager.connectionInfo
        val state = WifiInfo.getDetailedStateOf(connectInfo?.supplicantState)
        return connectInfo.linkSpeed.toString()
    }


    private fun getWifiEnvironment(context: Context): WifiInfo {

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager;

        // API10 からアプリからの wifi on/off できなくなった
        // Wifi の設定画面を出して、enabled になるまで待つ、という処理が必要
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(context, "WIFI ON", Toast.LENGTH_SHORT).show()
            wifiManager.setWifiEnabled(true);
        }
        if(!wifiManager.isWifiEnabled) return wifiManager.connectionInfo;
        // connectInfo まで取れてそう、IP アドレスは正しい
        val connectInfo = wifiManager.connectionInfo
        //
        val state = WifiInfo.getDetailedStateOf(connectInfo?.supplicantState)
        return wifiManager.connectionInfo
    }

    private fun getInterfaceMacaddr(context: Context): String {
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
        return wifiManager.connectionInfo.macAddress
    }

    private fun getLocalTimeString(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        return current.format(formatter)
    }

    fun toJsonString(): String? {
        var result :String? = null
        if (diag_results.isEmpty())
            return null

        diag_results.forEach {
            //Json に書く

        }
        return result
    }


    private fun uploadResults() {
        // Kotlin では必要ないのかも
        if (diag_results.isEmpty())
            return

        diag_results.forEach {
            var gson = Gson()
            val jsonString = gson.toJson(it)
            Log.i("test", "json string: " + jsonString)
            /* フォーマット例
            {
                "detail": "48.268",
                "layer": "dns",
                "log_campaign_uuid": "25838fa6-1be2-4cf5-bf6c-994d24852b6e",
                "log_group": "IPv4",
                "log_type": "v4rtt_namesrv_max",
                "occurred_at": "2020-05-16 14:06:30",
                "result": "10",
                "target": "1.1.1.1"
            }
             */
            // 送信:
            var con: HttpURLConnection? = null
            try {
                val urlStr = "http://fluentd.sindan-net.com:8888/sindan.log"
                val url = URL(urlStr)
                con = url.openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.instanceFollowRedirects = false
                con.setRequestProperty("User-Agent", "Android");
                con.addRequestProperty("Content-Type", "application/json; charset=UTF-8")
                con.doOutput = true
//                con.doInput = true
                con.connect()

                val os = con.outputStream
                val ps = PrintStream(os)
                ps.print(jsonString)
                ps.close()
                val responseCode = con.responseCode
            } catch (e: InterruptedException) {
                // 送信ネットワークエラー
                e.printStackTrace()
            } finally {
                con?.disconnect()
            }
        }
    }

    private fun uploadLogCampaign() {
        /*
        log_campaign_uuid:  "25838fa6-1be2-4cf5-bf6c-994d24852b6e"
        mac_addr:           "00:11:22:AA:BB:CC"
        os:                 "Android"
        network_type:       ""
        ssid:               "eduroam"
        version:            "0.1.1"
        occurred_at:        "2020-05-16 14:06:30"
        */
        val formattedTime = getLocalTimeString()
        val logCampaign = LogCampaign(log_campaign_uuid.toString(), getInterfaceMacaddr(context),
                                        context.getString(R.string.os) + " "
                                        + android.os.Build.VERSION.RELEASE
                                        + " API level " + android.os.Build.VERSION.SDK_INT,
                                        context.getString(R.string.network_type),
                                        getWifiEnvironment(context).ssid,
                                        context.getString(R.string.version), formattedTime)
        var gson = Gson()
        val jsonString = gson.toJson(logCampaign)
        Log.i("test", "json string: " + jsonString)

        var con: HttpURLConnection? = null
        try {
            val urlStr = "http://fluentd.sindan-net.com:8888/sindan.log_campaign"
            val url = URL(urlStr)
            con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.instanceFollowRedirects = false
            con.setRequestProperty("User-Agent", "Android");
            con.addRequestProperty("Content-Type", "application/json; charset=UTF-8")
            con.doOutput = true
//                con.doInput = true
            con.connect()

            val os = con.outputStream
            val ps = PrintStream(os)
            ps.print(jsonString)
            ps.close()
            val responseCode = con.responseCode
        } catch (e: InterruptedException) {
            // 送信ネットワークエラー
            e.printStackTrace()
        } finally {
            con?.disconnect()
        }
    }

    private fun ipToString(ipAddress:Int):String{
        return (ipAddress and 0xFF).toString()+"."+(ipAddress shr 8 and 0xFF)+"."+(ipAddress shr 16 and 0xFF)+"."+(ipAddress shr 24 and 0xFF)
    }

    private fun frequencyToChannel(frequency: Int) :Int {
        val channelMapping : Map<Int, Int> = mapOf(
            2412 to 1, 2417 to 2, 2422 to 3, 2427 to 4, 2432 to 5, 2437 to 6, 2442 to 7,
            2447 to 8, 2452 to 9, 2457 to 10, 2462 to 11, 2467 to 12, 2472 to 13, 2475 to 14,
            5180 to 36, 5200 to 40, 5220 to 44, 5240 to 48, 5260 to 52, 5280 to 56,
            5300 to 60, 5320 to 64,
            5500 to 100, 5520 to 104, 5540 to 108, 5560 to 112, 5580 to 116, 5600 to 120,
            5620 to 124, 5640 to 128, 5660 to 132, 5680 to 136, 5700 to 140, 5720 to 144
        )

        val result = channelMapping[frequency]

        if (result != null)
            return result
        else
            return 0
    }

    private fun phase1() {
        val phase1 = "to-hutohu_phase1"

        var wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.e(phase1, "Wi-Fi is not enabled")
            return
        }

        var wifiInfo = wifiManager.connectionInfo
        var wifiResults = wifiManager.scanResults
        /*
        var wifiResult = wifiResults.find { r -> r.SSID == wifiInfo.ssid.replace("\"", "") }

        if (wifiResult == null) {
            Log.e(phase1, "Wi-Fi does not found")
            return
        }
        */
        Log.i(phase1, "\n\n\n=========== Phase1 Start ===========")

        // TODO: phase1 じゃなく、全体で計測に使う interface を決める必要がある
        // 複数（だいたいWWANとWiFiとBlueTooth）
        val interfaces = NetworkInterface.getNetworkInterfaces()

        // # Get current SSID
        // 本体の設定で位置情報の取得をこのアプリに許可していないと
        // connectInfo.ssid に <unknown_ssid> が入る
        var result = DiagResult("datalink", "Wi-Fi", "ssid",
                            log_campaign_uuid, resInfo,
                            "self", getWifiEnvironment(context).ssid,
                            getLocalTimeString())
        diag_results.add(result)

        // # Down, Up interface
        // if [ ${RECONNECT} = "yes" ]; then
        // # Down target interface
        // if [ "${VERBOSE}" = "yes" ]; then
        // echo " interface:${devicename} down"
        // fi
        // do_ifdown ${devicename}
        // sleep 2
        // TODO: 恐らく必要ではないので

        // # Start target interface
        // if [ "${VERBOSE}" = "yes" ]; then
        // echo " interface:${devicename} up"
        // fi
        // do_ifup ${devicename}
        // sleep 5
        // fi
        // TODO: 恐らく必要ではないので

        // # set specific ssid
        // if [ -n "${SSID}" -a -n "${SSID_KEY}" ]; then
        // echo " set SSID:${SSID}"
        // networksetup -setairportnetwork ${devicename} ${SSID} ${SSID_KEY}
        // sleep 5
        // #elif [ -n "${pre_ssid}" ]; then
        // #  networksetup -setairportnetwork ${devicename} "${pre_ssid}"
        // #  sleep 5
        // fi
        // Android 版では本体設定で設定してある wifi に接続する

        // # Check I/F status
        // result_phase1=${FAIL}
        // rcount=0
        // while [ "${rcount}" -lt "${MAX_RETRY}" ]; do
        //     ifstatus=$(get_ifstatus ${devicename})
        // if [ $? -eq 0 ]; then
        // result_phase1=${SUCCESS}
        // break
        // fi
        // sleep 5
        // rcount=$(( rcount + 1 ))
        // done
        // if [ -n "${ifstatus}" ]; then
        // write_json ${layer} "common" ifstatus ${result_phase1} self ${ifstatus} 0
        // fi

        // この流れだといつでもactiveだと思うけど
        // Log.i(phase1, "isActive: " + if (wifiManager.isWifiEnabled) "true" else "false")
        result = DiagResult("datalink", "common", "ifstatus",
            log_campaign_uuid, if (wifiManager.isWifiEnabled) resSuccess else resFail,
            "self", if (wifiManager.isWifiEnabled) "up" else "down",
            getLocalTimeString())
        diag_results.add(result)

        // # Get iftype
        //         write_json ${layer} "common" iftype ${INFO} self ${IFTYPE} 0
        // TODO: とりあえずWi-Fi固定
        result = DiagResult("datalink", "common", "iftype",
            log_campaign_uuid, resInfo,
            "self", "Wi-Fi",
            getLocalTimeString())
        diag_results.add(result)

        // # Get ifmtu
        //         ifmtu=$(get_ifmtu ${devicename})
        // if [ -n "${ifmtu}" ]; then
        // write_json ${layer} "common" ifmtu ${INFO} self ${ifmtu} 0
        // fi
        // TODO: 0 になるのでおかしい
        // default 値から違う場合のみ意味のある値を返して、デフォルトのままだと 0 を返す
        // default 値を取得するのは root が必要？
        result = DiagResult("datalink", "common", "ifmtu",
            log_campaign_uuid, resInfo,
            "self", manager.getLinkProperties(currentNetwork).mtu.toString() ,
            getLocalTimeString())
        diag_results.add(result)

        Log.i(phase1, "lp: " + lp.toString())


        // #
        // if [ "${IFTYPE}" != "Wi-Fi" ]; then
        // # Get media type
        // media=$(get_mediatype ${devicename})
        // if [ -n "${media}" ]; then
        // write_json ${layer} "${IFTYPE}" media ${INFO} self ${media} 0
        // fi
        // TODO: Wi-Fi固定なので不要

        // # Get Wi-Fi BSSID
        result = DiagResult("datalink", "Wi-Fi", "bssid",
            log_campaign_uuid, resInfo,
            "self", getWifiEnvironment(context).bssid,
            getLocalTimeString())
        diag_results.add(result)

        // # Get Wi-Fi channel
        result = DiagResult("datalink", "Wi-Fi", "channel",
            log_campaign_uuid, resInfo,
            "self", frequencyToChannel(getWifiEnvironment(context).frequency).toString(),
            getLocalTimeString())
        diag_results.add(result)

        // # Get Wi-Fi RSSI
        result = DiagResult("datalink", "Wi-Fi", "rssi",
            log_campaign_uuid, resInfo,
            "self", getWifiEnvironment(context).rssi.toString(),
            getLocalTimeString())
        diag_results.add(result)

        // # Get Wi-Fi noise
        //         noise=$(get_wifi_noise)
        // if [ -n "${noise}" ]; then
        // write_json ${layer} "${IFTYPE}" noise ${INFO} self ${noise} 0
        // fi
        // TODO: 計算可能？(取得できるプロパティには存在せず)

        // # Get Wi-Fi quality
        //         quarity=$(get_wifi_quality "$devicename")
        // if [ -n "$quarity" ]; then
        // write_json "$layer" "$IFTYPE" quarity "$INFO" self "$quarity" 0
        // fi
        // TODO: 不明(計算可能？)

        // # Get Wi-Fi rate
        //         rate=$(get_wifi_rate)
        // if [ -n "${rate}" ]; then
        // write_json ${layer} "${IFTYPE}" rate ${INFO} self ${rate} 0
        // fi
        result = DiagResult("datalink", "Wi-Fi", "rate",
            log_campaign_uuid, resInfo,
            "self", getSpeed(context),
            getLocalTimeString())
        diag_results.add(result)
        Log.i(phase1, "Rate: "+ wifiInfo.linkSpeed)

        // # Get Wi-Fi environment
        //         environment=$(get_wifi_environment)
        // if [ -n "${environment}" ]; then
        // write_json ${layer} "${IFTYPE}" environment ${INFO} self "${environment}" 0
        // fi
        // fi

        // SSID, BSSID, RSSI, CHANNEL
        Log.i(phase1, "Environment:")
        wifiResults.forEach({ r ->
            Log.i(phase1, "  SSID: " + r.SSID)
            Log.i(phase1, "    BSSID: " + r.BSSID)
            Log.i(phase1, "    RSSI: " + r.level)
            Log.i(phase1, "    Frequency: " + r.frequency)
            Log.i(phase1, "    Ch Width: " + r.channelWidth)
        })


        // ## Write campaign log file (pre)
        // #ssid=WIRED
        // #if [ "${IFTYPE}" = "Wi-Fi" ]; then
        // #  ssid=$(get_wifi_ssid ${devicename})
        // #fi
        // #write_json_campaign ${uuid} ${mac_addr} "${os}" "${ssid}"

        // # Report phase 1 results
        //         if [ "${VERBOSE}" = "yes" ]; then
        // echo " datalink information:"
        // echo "  datalink status: ${result_phase1}"
        // echo "  type: ${IFTYPE}, dev: ${devicename}"
        // echo "  status: ${ifstatus}, mtu: ${ifmtu} MB"
        // if [ "${IFTYPE}" != "Wi-Fi" ]; then
        // echo "  media: ${media}"
        // else
        // echo "  ssid: ${ssid}, ch: ${channel}, rate: ${rate} Mbps"
        // echo "  bssid: ${bssid}"
        // echo "  rssi: ${rssi} dB, noise: ${noise} dB"
        // echo "  environment:"
        // echo "${environment}"
        // fi
        // fi

        // echo " done."
    }
    private fun phase2() {
        val phase2 = "to-hutohu_phase2"

        val runtime = Runtime.getRuntime()

        var wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.e(phase2, "Wi-Fi is not enabled")
            return
        }

        var wifiInfo = wifiManager.connectionInfo
        var wifiResults = wifiManager.scanResults
        /*
        var wifiResult = wifiResults.find { r -> r.SSID == wifiInfo.ssid.replace("\"", "") }

        if (wifiResult == null) {
            Log.e(phase2, "Wi-Fi does not found")
            return
        }
        */
        var dhcpInfo =  wifiManager.dhcpInfo

        Log.i(phase2, "\n\n\n=========== Phase2 Start ===========")
/*
####################
## Phase 2
echo "Phase 2: Interface Layer checking..."
layer="interface"

## IPv4
# Get IPv4 I/F configurations
v4ifconf=$(get_v4ifconf "${IFTYPE}")
if [ -n "${v4ifconf}" ]; then
  write_json ${layer} IPv4 v4ifconf ${INFO} self ${v4ifconf} 0
fi
// https://stackoverflow.com/questions/15822884/android-dhcp-enabled-disabled-check
もうDeprecatedだった
*/
        // TODO: 不明



/*
# Check IPv4 autoconf
result_phase2_1=${FAIL}
rcount=0
while [ ${rcount} -lt "${MAX_RETRY}" ]; do
  v4autoconf=$(check_v4autoconf ${devicename} ${v4ifconf})
  if [ $? -eq 0 -a -n "${v4autoconf}" ]; then
    result_phase2_1=${SUCCESS}
    break
  fi
  sleep 5
  rcount=$(( rcount + 1 ))
done
write_json ${layer} IPv4 v4autoconf ${result_phase2_1} self "${v4autoconf}" 0
*/

        // TODO: autoconfがわからない


        // # Get IPv4 address
        var result = DiagResult("interface", "IPv4", "v4addr",
            log_campaign_uuid, resInfo,
            "self", ipToString(wifiInfo.ipAddress),
            getLocalTimeString())
        diag_results.add(result)

/*
# Get IPv4 netmask
netmask=$(get_netmask ${devicename})
if [ -n "${netmask}" ]; then
  write_json ${layer} IPv4 netmask ${INFO} self ${netmask} 0
fi
*/
        // これで取れるはずなのだが0が返ってくる
        Log.i(phase2, "" + dhcpInfo.netmask)

        // # Get IPv4 routers
        result = DiagResult("interface", "IPv4", "v4routers",
            log_campaign_uuid, resInfo,
            "self", ipToString(dhcpInfo.gateway),
            getLocalTimeString())
        diag_results.add(result)

        // # Get IPv4 name servers
        result = DiagResult("interface", "IPv4", "v4nameservers",
            log_campaign_uuid, resInfo,
            "self", ipToString(dhcpInfo.dns1)
                                   + if (dhcpInfo.dns2 != 0) "," + ipToString(dhcpInfo.dns2) else "",
            getLocalTimeString())
        diag_results.add(result)

        /*
# Get IPv4 NTP servers
#TBD

# Report phase 2 results (IPv4)
if [ "${VERBOSE}" = "yes" ]; then
  echo " interface information:"
  echo "  intarface status (IPv4): ${result_phase2_1}"
  echo "  IPv4 conf: ${v4ifconf}"
  echo "  IPv4 addr: ${v4addr}/${netmask}"
  echo "  IPv4 router: ${v4routers}"
  echo "  IPv4 namesrv: ${v4nameservers}"
fi

## IPv6
# Get IPv6 I/F configurations
v6ifconf=$(get_v6ifconf "${IFTYPE}")
if [ -n "${v6ifconf}" ]; then
  write_json ${layer} IPv6 v6ifconf ${INFO} self ${v6ifconf} 0
fi

# Get IPv6 linklocal address
v6lladdr=$(get_v6lladdr ${devicename})
if [ -n "${v6lladdr}" ]; then
  write_json ${layer} IPv6 v6lladdr ${INFO} self ${v6lladdr} 0
fi

# Get IPv6 RA flags
ra_flags=$(get_ra_flags ${devicename})
if [ -z "${ra_flags}" ]; then
  ra_flags="none"
fi
if [ -n "${ra_flags}" ]; then
  write_json ${layer} RA ra_flags ${INFO} self ${ra_flags} 0
fi

# Get IPv6 RA prefix
ra_prefixes=$(get_ra_prefixes ${devicename})
if [ -n "${ra_prefixes}" ]; then
  write_json ${layer} RA ra_prefixes ${INFO} self ${ra_prefixes} 0
fi

# Report phase 2 results (IPv6)
if [ "${VERBOSE}" = "yes" ]; then
  echo "  IPv6 conf: ${v6ifconf}"
  echo "  IPv6 lladdr: ${v6lladdr}"
fi

if [ "${ra_flags}" = "not_exist" ]; then
  if [ "${VERBOSE}" = "yes" ]; then
    echo "   RA does not exist."
  fi
else
  if [ "${VERBOSE}" = "yes" ]; then
    echo "  IPv6 RA flags: ${ra_flags}"
  fi
  count=0
  for pref in `echo ${ra_prefixes} | sed 's/,/ /g'`; do
    # Get IPv6 RA prefix flags
    ra_prefix_flags=$(get_ra_prefix_flags ${devicename} ${pref})
    write_json ${layer} RA ra_prefix_flags ${INFO} ${pref} "${ra_prefix_flags}" ${count}
    if [ "${VERBOSE}" = "yes" ]; then
      echo "  IPv6 RA prefix(flags): ${pref}(${ra_prefix_flags})"
    fi

    # Get IPv6 prefix length
    prefixlen=$(get_prefixlen ${pref})
    write_json ${layer} RA prefixlen ${INFO} ${pref} ${prefixlen} ${count}

    # Check IPv6 autoconf
    result_phase2_2=${FAIL}
    rcount=0
    while [ ${rcount} -lt "${MAX_RETRY}" ]; do
      # Get IPv6 address
      v6addrs=$(get_v6addrs ${devicename} ${pref})
      v6autoconf=$(check_v6autoconf ${devicename} ${v6ifconf} ${ra_flags} ${pref} ${ra_prefix_flags})
      if [ $? -eq 0 -a -n "${v6autoconf}" ]; then
        result_phase2_2=${SUCCESS}
        break
      fi
      sleep 5
      rcount=$(( rcount + 1 ))
    done
    write_json ${layer} IPv6 v6addrs ${INFO} ${pref} "${v6addrs}" ${count}
    write_json ${layer} IPv6 v6autoconf ${result_phase2_2} ${pref} "${v6autoconf}" ${count}
    if [ "${VERBOSE}" = "yes" ]; then
      for addr in `echo ${v6addrs} | sed 's/,/ /g'`; do
        echo "   IPv6 addr: ${addr}/${prefixlen}"
      done
      echo "   intarface status (IPv6): ${result_phase2_2}"
    fi
    count=$(( count + 1 ))
  done

  # Get IPv6 routers
  v6routers=$(get_v6routers ${devicename})
  if [ -n "${v6routers}" ]; then
    write_json ${layer} IPv6 v6routers ${INFO} self "${v6routers}" 0
  fi
  if [ "${VERBOSE}" = "yes" ]; then
    echo "  IPv6 routers: ${v6routers}"
  fi

  # Get IPv6 name servers
  v6nameservers=$(get_v6nameservers)
  if [ -n "${v6nameservers}" ]; then
    write_json ${layer} IPv6 v6nameservers ${INFO} self "${v6nameservers}" 0
  fi
  if [ "${VERBOSE}" = "yes" ]; then
    echo "  IPv6 nameservers: ${v6nameservers}"
  fi

  # Get IPv6 NTP servers
  #TBD

  echo " done."
 */
        // TODO: IPv6側(Android APIの提供がない？ 少なくともip addressとかはInt型だから直接はv6を返せないはず)
        // ip routeは出力が無い(Androidはポリシールーティングらしい？)
        // [AndroidのIPv6アドレス, ルーティングテーブルの確認 (モバイルがIPv6接続の時) - Qiita](https://qiita.com/ip6/items/68178d4c864ec6504d5d)
        runtime.exec("ip addr")

    }

    private fun phase3() {
        val phase3 = "to-hutohu_phase3"

        val runtime = Runtime.getRuntime()

        var wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.e(phase3, "Wi-Fi is not enabled")
            return
        }

        var wifiInfo = wifiManager.connectionInfo
        var wifiResults = wifiManager.scanResults
        var wifiResult = wifiResults.find { r -> r.SSID == wifiInfo.ssid.replace("\"", "") }

        if (wifiResult == null) {
            Log.e(phase3, "Wi-Fi does not found")
            return
        }
        var dhcpInfo =  wifiManager.dhcpInfo
        var routerAddr = InetAddress.getByAddress(dhcpInfo.gateway.toBigInteger().toByteArray().reversedArray()).hostAddress
        var dnsAddr = InetAddress.getByAddress(dhcpInfo.dns1.toBigInteger().toByteArray().reversedArray()).hostAddress

        Log.i(phase3, "\n\n\n=========== Phase3 Start ===========")

//        var traceResult = TraceRoute.traceRoute("8.8.8.8")
//        Log.d(phase3, traceResult.toString())

        val dnsResolver = DnsResolver.getInstance()
        dnsResolver.query(null, "google.co.jp", DnsResolver.FLAG_NO_CACHE_LOOKUP, ContextCompat.getMainExecutor(context), null, object: DnsResolver.Callback<List<InetAddress>> {
            override fun onAnswer(p0: List<InetAddress>, p1: Int) {
                Log.d(phase3, "Int: " + p1)
                p0.map {addr ->
                    Log.d(phase3, "Addr: " + addr.hostAddress)
                }
            }

            override fun onError(p0: DnsResolver.DnsException) {
                Log.d(phase3, p0.toString())
            }
        })

        dnsResolver.rawQuery(null, "google.co.jp".toByteArray(), DnsResolver.FLAG_NO_CACHE_LOOKUP, ContextCompat.getMainExecutor(context), null, object: DnsResolver.Callback<ByteArray> {
            override fun onAnswer(p0: ByteArray, p1: Int) {
                Log.d(phase3, p0.toString())
            }

            override fun onError(p0: DnsResolver.DnsException) {
                Log.d(phase3, p0.toString())
            }
        })

        Log.d(phase3, "end")
        /*
        ## Phase 3
echo "Phase 3: Localnet Layer checking..."
layer="localnet"

# Do ping to IPv4 routers
count=0
for target in $(echo "$v4routers" | sed 's/,/ /g'); do
  cmdset_ping "$layer" 4 router "$target" "$count" &
  count=$(( count + 1 ))
done
*/
        runtime.exec("ping -c " + routerAddr)
        /*
# Do ping to IPv4 nameservers
count=0
for target in $(echo "$v4nameservers" | sed 's/,/ /g'); do
  cmdset_ping "$layer" 4 namesrv "$target" "$count" &
  count=$(( count + 1 ))
done
*/
        runtime.exec("ping -c " + dnsAddr)

        /*
# Do ping to IPv6 routers
count=0
for target in $(echo "$v6routers" | sed 's/,/ /g'); do
  cmdset_ping "$layer" 6 router "$target" "$count" &
  count=$(( count + 1 ))
done
*/

        /*
# Do ping to IPv6 nameservers
count=0
for target in $(echo "$v6nameservers" | sed 's/,/ /g'); do
  cmdset_ping "$layer" 6 namesrv "$target" "$count" &
  count=$(( count + 1 ))
done
*/

    }

    private fun phase4() {
        val phase4 = "to-hutohu_phase4"

        val runtime = Runtime.getRuntime()

        var wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.e(phase4, "Wi-Fi is not enabled")
            return
        }

        var wifiInfo = wifiManager.connectionInfo
        var wifiResults = wifiManager.scanResults
        var wifiResult = wifiResults.find { r -> r.SSID == wifiInfo.ssid.replace("\"", "") }

        if (wifiResult == null) {
            Log.e(phase4, "Wi-Fi does not found")
            return
        }
        var dhcpInfo =  wifiManager.dhcpInfo
        var routerAddr = InetAddress.getByAddress(dhcpInfo.gateway.toBigInteger().toByteArray().reversedArray()).hostAddress
        var dnsAddr = InetAddress.getByAddress(dhcpInfo.dns1.toBigInteger().toByteArray().reversedArray()).hostAddress
        var externalDNSServerAddr = "8.8.8.8"
        var externalDNSServerIPv6Addr = "2001:4860:4860::8888"

        Log.i(phase4, "\n\n\n=========== Phase4 Start ===========")
        /*
        ## Phase 4
echo "Phase 4: Globalnet Layer checking..."
layer="globalnet"

if [ "$EXCL_IPv4" != "yes" ]; then
  v4addr_type=$(check_v4addr "$v4addr")
else
  v4addr_type="linklocal"
fi
if [ "$v4addr_type" = "private" ] || [ "$v4addr_type" = "grobal" ]; then
  count=0
  for target in $(echo "$PING_SRVS" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      # Do ping to IPv4 routers
      count_r=0
      for target_r in $(echo "$v4routers" | sed 's/,/ /g'); do
        cmdset_ping "$layer" 4 router "$target_r" "$count_r" &
        count_r=$(( count_r + 1 ))
      done
    fi
    */
        runtime.exec("ping -c 3 " + routerAddr)

        /*
    # Do ping to extarnal IPv4 servers
    cmdset_ping "$layer" 4 srv "$target" "$count" &
    */
        runtime.exec("ping -c 3 " + externalDNSServerAddr)

        /*
    # Do traceroute to extarnal IPv4 servers
    cmdset_trace "$layer" 4 srv "$target" "$count" &
    */
//        var traceResult = TraceRoute.traceRoute(externalDNSServerAddr)
//        Log.i(phase4, traceResult.toString())

        /*
    if [ "$MODE" = "client" ]; then
      # Check path MTU to extarnal IPv4 servers
      cmdset_pmtud "$layer" 4 srv "$target" "$ifmtu" "$count" &
    fi

    count=$(( count + 1 ))
  done
fi
*/
        // TODO: 不明

        /*
if [ -n "$v6addrs" ]; then
  count=0
  for target in $(echo "$PING6_SRVS" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      # Do ping to IPv6 routers
      count_r=0
      for target_r in $(echo "$v6routers" | sed 's/,/ /g'); do
        cmdset_ping "$layer" 6 router "$target_r" "$count_r" &
        count_r=$(( count_r + 1 ))
      done
    fi
    */
        // TODO: IPv6のGWの取得方法がわからない

        /*
    # Do ping to extarnal IPv6 servers
    cmdset_ping "$layer" 6 srv "$target" "$count" &
    */
//        exec("ping6 -c 3 " + externalDNSServerIPv6Addr)

        /*
    # Do traceroute to extarnal IPv6 servers
    cmdset_trace "$layer" 6 srv "$target" "$count" &
    */
//        var traceResultIPv6 = TraceRoute.traceRoute(externalDNSServerIPv6Addr)
//        Log.i(phase4, traceResultIPv6.toString())

        /*
    if [ "$MODE" = "client" ]; then
      # Check path MTU to extarnal IPv6 servers
      cmdset_pmtud "$layer" 6 srv "$target" "$ifmtu" "$count" &
    fi
    */
        // TODO: 不明

        /*
    count=$(( count + 1 ))
  done
fi

wait
echo " done."
         */
    }

    private fun phase5() {
        val phase5 = "to-hutohu_phase5"

        val runtime = Runtime.getRuntime()

        var wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.e(phase5, "Wi-Fi is not enabled")
            return
        }

        var wifiInfo = wifiManager.connectionInfo
        var wifiResults = wifiManager.scanResults
        var wifiResult = wifiResults.find { r -> r.SSID == wifiInfo.ssid.replace("\"", "") }

        if (wifiResult == null) {
            Log.e(phase5, "Wi-Fi does not found")
            return
        }
        var dhcpInfo =  wifiManager.dhcpInfo

        var dnsResolver = DnsResolver.getInstance()

        var routerAddr = InetAddress.getByAddress(dhcpInfo.gateway.toBigInteger().toByteArray().reversedArray()).hostAddress
        var dnsAddr = InetAddress.getByAddress(dhcpInfo.dns1.toBigInteger().toByteArray().reversedArray()).hostAddress
        var externalDNSServerAddr = "8.8.8.8"
        var externalDNSServerIPv6Addr = "2001:4860:4860::8888"

        var v4Server = "www.wide.ad.jp"
        var v6Server = "www.wide.ad.jp"

        Log.i(phase5, "\n\n\n=========== Phase5 Start ===========")
        /*
        ## Phase 5
echo "Phase 5: DNS Layer checking..."
layer="dns"

# Clear dns local cache
#TBD

if [ "$v4addr_type" = "private" ] || [ "$v4addr_type" = "grobal" ]; then
  count=0
  for target in $(echo "$v4nameservers" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      # Do ping to IPv4 nameservers
      cmdset_ping "$layer" 4 namesrv "$target" "$count" &
    fi
    */
        runtime.exec("ping -c 3 " + dnsAddr)
/*
    # Do dns lookup for A record by IPv4
    cmdset_dnslookup "$layer" 4 A "$target" "$count" &
    */
        dnsResolver.query(null, v4Server, DnsResolver.TYPE_A, DnsResolver.FLAG_NO_CACHE_LOOKUP, ContextCompat.getMainExecutor(context), null, object: DnsResolver.Callback<List<InetAddress>> {
            override fun onAnswer(p0: List<InetAddress>, p1: Int) {
                Log.i(phase5, "Domain: " + v4Server)
                p0.forEach {addr ->
                    Log.i(phase5, "  Addr: " + p0.toString())
                }
            }

            override fun onError(p0: DnsResolver.DnsException) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })

        /*
    # Do dns lookup for AAAA record by IPv4
    cmdset_dnslookup "$layer" 4 AAAA "$target" "$count" &

    count=$(( count + 1 ))
  done
  */
        dnsResolver.query(null, v4Server, DnsResolver.TYPE_AAAA, DnsResolver.FLAG_NO_CACHE_LOOKUP, ContextCompat.getMainExecutor(context), null, object: DnsResolver.Callback<List<InetAddress>> {
            override fun onAnswer(p0: List<InetAddress>, p1: Int) {
                Log.i(phase5, "Domain: " + v4Server)
                p0.forEach {addr ->
                    Log.i(phase5, "  Addr: " + p0.toString())
                }
            }

            override fun onError(p0: DnsResolver.DnsException) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })

        /*
  count=0
  for target in $(echo "$GPDNS4" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      # Do ping to IPv4 routers
      count_r=0
      for target_r in $(echo "$v4routers" | sed 's/,/ /g'); do
        cmdset_ping "$layer" 4 router "$target_r" "$count_r" &
        count_r=$(( count_r + 1 ))
      done

      # Do ping to IPv4 nameservers
      cmdset_ping "$layer" 4 namesrv "$target" "$count" &

      # Do traceroute to IPv4 nameservers
      cmdset_trace "$layer" 4 namesrv "$target" "$count" &
    fi

    # Do dns lookup for A record by IPv4
    cmdset_dnslookup "$layer" 4 A "$target" "$count" &

    # Do dns lookup for AAAA record by IPv4
    cmdset_dnslookup "$layer" 4 AAAA "$target" "$count" &

    count=$(( count + 1 ))
  done
fi
*/
        // TODO: 外部のDNSサーバーを指定できない

        /*
exist_dns64="no"
if [ -n "$v6addrs" ]; then
  count=0
  for target in $(echo "$v6nameservers" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      # Do ping to IPv6 nameservers
      cmdset_ping "$layer" 6 namesrv "$target" "$count" &
    fi

    # Do dns lookup for A record by IPv6
    cmdset_dnslookup "$layer" 6 A "$target" "$count" &

    # Do dns lookup for AAAA record by IPv6
    cmdset_dnslookup "$layer" 6 AAAA "$target" "$count" &

    # check DNS64
    exist_dns64=$(check_dns64 "$target")

    count=$(( count + 1 ))
  done

  count=0
  for target in $(echo "$GPDNS6" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      # Do ping to IPv6 routers
      count_r=0
      for target_r in $(echo "$v6routers" | sed 's/,/ /g'); do
        cmdset_ping "$layer" 6 router "$target_r" "$count_r" &
        count_r=$(( count_r + 1 ))
      done

      # Do ping to IPv6 nameservers
      cmdset_ping "$layer" 6 namesrv "$target" "$count" &

      # Do traceroute to IPv6 nameservers
      cmdset_trace "$layer" 6 namesrv "$target" "$count" &
    fi

    # Do dns lookup for A record by IPv6
    cmdset_dnslookup "$layer" 6 A "$target" "$count" &

    # Do dns lookup for AAAA record by IPv6
    cmdset_dnslookup "$layer" 6 AAAA "$target" "$count" &

    count=$(( count + 1 ))
  done
fi

wait
echo " done."
         */
    }

    private fun phase6() {
        val phase6 = "to-hutohu_phase6"

        var wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            Log.e(phase6, "Wi-Fi is not enabled")
            return
        }

        var wifiInfo = wifiManager.connectionInfo
        var wifiResults = wifiManager.scanResults
        var wifiResult = wifiResults.find { r -> r.SSID == wifiInfo.ssid.replace("\"", "") }

        if (wifiResult == null) {
            Log.e(phase6, "Wi-Fi does not found")
            return
        }
        var dhcpInfo =  wifiManager.dhcpInfo

        var dnsResolver = DnsResolver.getInstance()

        var routerAddr = InetAddress.getByAddress(dhcpInfo.gateway.toBigInteger().toByteArray().reversedArray()).hostAddress
        var dnsAddr = InetAddress.getByAddress(dhcpInfo.dns1.toBigInteger().toByteArray().reversedArray()).hostAddress
        var externalDNSServerAddr = "8.8.8.8"
        var externalDNSServerIPv6Addr = "2001:4860:4860::8888"

        var v4Server = "www.wide.ad.jp"
        var v6Server = "www.wide.ad.jp"

        var v4HTTPServer = "http://www.wide.ad.jp"
        var v6HTTPServer = "http://www.wide.ad.jp"

        Log.i(phase6, "\n\n\n=========== Phase6 Start ===========")
        /*
        ## Phase 6
echo "Phase 6: Web Layer checking..."
layer="web"

if [ "$v4addr_type" = "private" ] || [ "$v4addr_type" = "grobal" ]; then
  count=0
  for target in $(echo "$V4WEB_SRVS" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      # Do ping to IPv4 routers
      count_r=0
      for target_r in $(echo "$v4routers" | sed 's/,/ /g'); do
        cmdset_ping "$layer" 4 router "$target_r" "$count_r" &
        count_r=$(( count_r + 1 ))
      done
      */
//        exec("ping -c 3 " + routerAddr)

        /*
      # Do ping to IPv4 web servers
      cmdset_ping "$layer" 4 srv "$target" "$count" &
      */
 //       exec("ping -c 3 " + v4Server)

        /*
      # Do traceroute to IPv4 web servers
      cmdset_trace "$layer" 4 srv "$target" "$count" &
    fi
    */
 //       var traceResult = TraceRoute.traceRoute(v4Server)

        /*
    # Do curl to IPv4 web servers by IPv4
    cmdset_http "$layer" 4 srv "$target" "$count" &
    */
//        var req = Request.Builder().get().url(v4HTTPServer).build()
//        var res = OkHttpClient().newCall(req).execute()
//        Log.i(phase6, "HTTP Status Code: " + res.code)

        /*
    # Do measure http throuput by IPv4
    #TBD
    # v4http_throughput_srv

    count=$(( count + 1 ))
  done
fi

if [ -n "$v6addrs" ]; then
  count=0
  for target in $(echo "$V6WEB_SRVS" | sed 's/,/ /g'); do
    if [ "$MODE" = "probe" ]; then
      count_r=0
      for target_r in $(echo "$v6routers" | sed 's/,/ /g'); do
        cmdset_ping "$layer" 6 router "$target_r" "$count_r" &
        count_r=$(( count_r + 1 ))
      done

      # Do ping to IPv6 web servers
      cmdset_ping "$layer" 6 srv "$target" "$count" &

      # Do traceroute to IPv6 web servers
      cmdset_trace "$layer" 6 srv "$target" "$count" &
    fi

    # Do curl to IPv6 web servers by IPv6
    cmdset_http "$layer" 6 srv "$target" "$count" &

    # Do measure http throuput by IPv6
    #TBD
    # v6http_throughput_srv

    count=$(( count + 1 ))
  done

  # DNS64
  if [ "$exist_dns64" = "yes" ]; then
    echo " exist dns64 server"
    count=0
    for target in $(echo "$V4WEB_SRVS" | sed 's/,/ /g'); do
      if [ "$MODE" = "probe" ]; then
        # Do ping to IPv6 routers
        count_r=0
        for target_r in $(echo "$v6routers" | sed 's/,/ /g'); do
          cmdset_ping "$layer" 6 router "$target_r" "$count_r" &
          count_r=$(( count_r + 1 ))
        done

        # Do ping to IPv4 web servers by IPv6
        cmdset_ping "$layer" 6 srv "$target" "$count" &

        # Do traceroute to IPv4 web servers by IPv6
        cmdset_trace "$layer" 6 srv "$target" "$count" &
      fi

      # Do curl to IPv4 web servers by IPv6
      cmdset_http "$layer" 6 srv "$target" "$count" &

      # Do measure http throuput by IPv6
      #TBD
      # v6http_throughput_srv

      count=$(( count + 1 ))
    done
  fi
fi

wait
echo " done."
         */
    }
}