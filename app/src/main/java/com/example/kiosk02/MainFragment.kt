package com.example.kiosk02

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class MainFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()

        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val adminButton = view.findViewById<Button>(R.id.admin_button)

        adminButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment)
        }


        view.findViewById<Button>(R.id.login_button).setOnClickListener {
            val email = view.findViewById<EditText>(R.id.email_input).text.toString()
            val password = view.findViewById<EditText>(R.id.password_input).text.toString()
            LoginToFirebase(email, password)
        }

        view.findViewById<Button>(R.id.sign_up_button).setOnClickListener {
            findNavController().navigate(R.id.action_to_fragment_consumer_sig)
        }

        return view
    }

    private fun LoginToFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(context, SearchStoreActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.e("LoginError", "Login failed", task.exception)
                }
            }

    }

}
