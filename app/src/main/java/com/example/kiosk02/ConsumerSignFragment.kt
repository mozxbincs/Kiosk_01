package com.example.kiosk02

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.databinding.FragmentConsumerSigBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConsumerSignFragment: Fragment(R.layout.fragment_consumer_sig) {
    private lateinit var binding: FragmentConsumerSigBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentConsumerSigBinding.bind(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.registerBackButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_mainActivity)
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val name = binding.nameEditText.text.toString()
            val phoneNumber = binding.phonNumberEditText.text.toString()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        saveConsumerDataToFirestore(email, name, phoneNumber)
                    } else {
                        Snackbar.make(binding.root, "회원가입 실패", Snackbar.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveConsumerDataToFirestore(email: String, name: String, phoneNumber: String) {
        val userMap = hashMapOf(
            "email" to email,
            "name" to name,
            "phoneNumber" to phoneNumber
        )
        firestore.collection("consumer").document(email)
            .set(userMap)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "회원가입 성공", Snackbar.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_to_mainActivity)
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "회원가입 실패", Snackbar.LENGTH_SHORT).show()
            }
    }


}