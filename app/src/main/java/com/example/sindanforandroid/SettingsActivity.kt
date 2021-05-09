package com.example.sindanforandroid

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
    // TODO: 戻るを実装

    // ひとまずひな形、未呼び出し
    protected fun setListPreferenceData(lp: ListPreference): Boolean {
        // TODO: まずは Interface List を取得する
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw      = connectivityManager.activeNetwork ?: return false
        // NetworkCapabilities という名前の構造体（単数構造体の複数形ではない）単体でコレクション風
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false

        // 取得した結果を Entries, EntryValues にセット
        val entries =
            arrayOf<CharSequence>("English", "French")
        val entryValues = arrayOf<CharSequence>("1", "2")
        lp.setEntries(entries)
        lp.setDefaultValue("1")
        lp.setEntryValues(entryValues)

        return true
    }
}