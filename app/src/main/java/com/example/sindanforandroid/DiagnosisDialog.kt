package com.example.sindanforandroid

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.SystemClock.sleep
import android.widget.TextView
import androidx.fragment.app.DialogFragment

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

        return builder.create()
    }

    private fun StartDiagnosis() {
    }

    private inner class DialogButtonClickLister : DialogInterface.OnClickListener {
        override fun onClick(p0: DialogInterface?, p1: Int) {
            TODO("Not yet implemented")
            // Abort Diagnosis
        }
    }

}