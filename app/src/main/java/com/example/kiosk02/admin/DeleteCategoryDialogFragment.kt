package com.example.kiosk02.admin

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.kiosk02.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteCategoryDialogFragment(
    private val categoryName: String,
    private val onCategoryDeleted: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_delete_category, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.messageTextView)

        messageTextView.text = "정말로 '$categoryName' 카테고리를 삭제하시겠습니까??"

        builder.setView(dialogView)

        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            onCategoryDeleted()
            dialog.dismiss()
        }

        return dialog
    }
}