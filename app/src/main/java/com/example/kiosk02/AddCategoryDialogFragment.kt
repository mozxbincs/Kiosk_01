package com.example.kiosk02

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class AddCategoryDialogFragment(private val onCategoryAdded: (String) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_category, null)
        val inputCategoryName = dialogView.findViewById<EditText>(R.id.inputCategoryName)

        builder.setView(dialogView)

        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.addButton).setOnClickListener {
            val categoryName = inputCategoryName.text.toString().trim()
            if (categoryName.isEmpty()) {

                Toast.makeText(requireContext(), "카테고리명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                onCategoryAdded(categoryName)
                dialog.dismiss()
            }
        }

        return dialog
    }
}