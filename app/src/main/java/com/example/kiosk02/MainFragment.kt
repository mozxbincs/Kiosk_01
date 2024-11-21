package com.example.kiosk02

import android.content.Context
import android.content.Intent
//import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.map.SearchStoreActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore
    //private lateinit var sharedPreferences: SharedPreferences
    //private lateinit var editor: SharedPreferences.Editor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        auth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()

        //sharedPreferences = requireContext().getSharedPreferences("ConsumerLoginPrefs", Context.MODE_PRIVATE)
        //editor = sharedPreferences.edit()

        /*
        val isLoggedIn = sharedPreferences.getBoolean("consumerLoggedIn", false)
        if (isLoggedIn) {
            val savedEmail = sharedPreferences.getString("consumer_email", null).orEmpty()
            val savedPassword = sharedPreferences.getString("consumer_password", null).orEmpty()
            LoginToFirebase(savedEmail, savedPassword)
        }*/

        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val adminButton = view.findViewById<Button>(R.id.admin_button)

        adminButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment)
        }


        view.findViewById<Button>(R.id.login_button).setOnClickListener {
            val email = view.findViewById<EditText>(R.id.email_input).text.toString()
            val password = view.findViewById<EditText>(R.id.password_input).text.toString()
            LoginToFirebase(email, password)
            /*
            val rememberMeCheckbox = view.findViewById<CheckBox>(R.id.remember_me_checkbox)
            if (rememberMeCheckbox.isChecked) {
                editor.putBoolean("consumerLoggedIn", true)
                editor.putString("consumer_email", email)
                editor.putString("consumer_password", password)
                editor.apply()
            }*/
        }

        view.findViewById<Button>(R.id.sign_up_button).setOnClickListener {
            findNavController().navigate(R.id.action_to_fragment_consumer_sig)
        }

        view.findViewById<TextView>(R.id.forgot_email_text).setOnClickListener {
            findNavController().navigate(R.id.action_to_findEmailConsumerFragment)
        }

        view.findViewById<TextView>(R.id.forgot_password_text).setOnClickListener {
            findNavController().navigate(R.id.action_to_findPasswordFragment)
        }

        return view
    }

    private fun LoginToFirebase(email: String, password: String) {

        fireStore.collection("consumer")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { document ->
                if(!document.isEmpty) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val intent = Intent(context, SearchStoreActivity::class.java)
                                startActivity(intent)
                            } else {
                                Log.e("LoginError", "Login failed", task.exception)
                                Snackbar.make( requireView(), "이메일을 정확히 입력해주세요.", Snackbar.LENGTH_SHORT)
                                    .show()
                            }
                        }
                } else {
                    Snackbar.make( requireView(), "이메일을 정확히 입력해주세요.", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
    }
}
