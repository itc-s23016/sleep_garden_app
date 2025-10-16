package com.example.sleep_garden.alarm

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TimePicker
import com.example.sleep_garden.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class TimePickerBottomSheet(
    private val onTimeSelected: (Int, Int) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null)
        dialog.setContentView(view)

        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
        val okButton = view.findViewById<Button>(R.id.okButton)

        timePicker.setIs24HourView(true)

        okButton.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            onTimeSelected(hour, minute)
            dialog.dismiss()
        }

        return dialog
    }
}
