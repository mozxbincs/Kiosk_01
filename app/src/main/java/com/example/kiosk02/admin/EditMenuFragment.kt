package com.example.kiosk02.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.kiosk02.databinding.FragmentEditMenuBinding

class EditMenuFragment : Fragment(){
    private lateinit var binding: FragmentEditMenuBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentEditMenuBinding.bind(view)



    }
}