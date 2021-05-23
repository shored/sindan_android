package com.example.sindanforandroid

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.SystemClock.sleep
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.util.concurrent.Executors

class DiagnosisDialog : DialogFragment() {

    companion object {
        fun newInstance(message: String): DiagnosisDialog {
            val instance = DiagnosisDialog()
            val arguments = Bundle()
            arguments.putString("測定中", message)
            instance.arguments = arguments
            return instance
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mMessage = arguments!!.getString("message")

        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.dialog_measure, null)
        val mMessageTextView = view.findViewById(R.id.progress_message) as TextView
        mMessageTextView.text = mMessage
        builder.setNegativeButton(R.string.bt_cancel, DialogButtonClickLister())
        builder.setView(view)

        // block できないのでここで非同期タスクを実行
        startDiagnosis()

        return builder.create()
    }

    private inner class DiagnosisWorker(): Runnable {
        override fun run() {
            var diagnosis = Diagnosis(requireContext())
            diagnosis.startDiagnosis()

            //終了したら Destroy
            this@DiagnosisDialog.dismiss()
        }
    }

    private fun startDiagnosis() {
        val backgroundReceiver = DiagnosisWorker()
        val executeService = Executors.newSingleThreadExecutor()
        executeService.submit(backgroundReceiver)
    }


    private inner class DialogButtonClickLister : DialogInterface.OnClickListener {
        override fun onClick(p0: DialogInterface?, p1: Int) {
            // Abort Diagnosis
            TODO("キャンセル時に計測停止")
        }
    }

}