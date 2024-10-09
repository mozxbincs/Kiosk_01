package com.example.kiosk02.admin

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.admin.data.MenuModel
import com.example.kiosk02.databinding.FragmentEditMenuBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

class EditMenuFragment : Fragment(R.layout.fragment_edit_menu) {
    private lateinit var binding: FragmentEditMenuBinding

    private var selectedUri: Uri? = null

    val user = Firebase.auth.currentUser


    //이미지 등록
    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedUri = uri
            binding.menuImageView.setImageURI(uri)
            binding.addImageButton.isVisible = false
            binding.clearImageButton.isVisible = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (user == null) {
            findNavController().navigate(R.id.adminFragment)
            return
        }

        binding = FragmentEditMenuBinding.bind(view)

        setupClearButton()

        setupPhotoImageView()

        setupClearButton()

        setupConfirmButton()

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_activity)
        }

    }


    private fun setupClearButton() {
        binding.clearImageButton.setOnClickListener {
            selectedUri = null
            binding.menuImageView.setImageURI(null)
            binding.addImageButton.isVisible = true
            binding.clearImageButton.isVisible = false
        }
    }

    private fun setupPhotoImageView() {
        binding.menuImageView.setOnClickListener {
            if (selectedUri == null) {
                startPicker()
            }
        }
    }

    private fun setupConfirmButton() {
        binding.confirmButton.setOnClickListener {
            showProgress()
            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadImage(
                    uri = photoUri,
                    successHandler = {
                        uploadArticle(
                            it,
                            binding.menuNameEditText.text.toString(),
                            binding.priceEditText.text.toString().toInt(),
                            binding.compositionEditText.text.toString(),
                            binding.menuDetailEditText.text.toString()
                        )
                    },
                    errorHandler = {
                        Snackbar.make(binding.root, "메뉴 등록에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                        hideProgress()
                    }
                )
            } else {
                Snackbar.make(binding.root, "이미지가 선택되지 않았습니다.", Snackbar.LENGTH_SHORT).show()
                hideProgress()
            }
        }
    }


    private fun startPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showProgress() {
        binding.progressBarLayout.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBarLayout.isVisible = false
    }

    private fun uploadImage(
        uri: Uri,
        successHandler: (String) -> Unit,
        errorHandler: (Throwable?) -> Unit
    ) {
        val email = user?.email?.replace(".", "_") // 이메일에 '.'을 '_'로 변환
        val menuName = binding.menuNameTextview.text.toString()

        //파일이름
        val filename = "${UUID.randomUUID()}.png"

        //파일 경로
        val storagePath = "$email/$menuName/$filename"
        Firebase.storage.reference.child("articles/photo").child(storagePath)
            .putFile(uri)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Firebase.storage.reference.child("articles/photo/$storagePath")
                        .downloadUrl
                        .addOnSuccessListener {
                            successHandler(it.toString())
                        }.addOnFailureListener {
                            errorHandler(it)

                        }
                } else {
                    errorHandler(task.exception)
                }
            }
    }

    private fun uploadArticle(
        photoUri: String,
        menuName: String,
        price: Int,
        composition: String,
        detail: String
    ) {
        val menuId = UUID.randomUUID().toString()
        val menuModel = MenuModel(
            menuId = menuId,
            menuName = menuName,
            price = price,
            composition = composition,
            detail = detail,

            )
        val email = user?.email.toString()
        val menuName = binding.menuNameEditText.text.toString()

        val firestore = FirebaseFirestore.getInstance()
        // admin 컬렉션에서 로그인한 유저 email 문서 참조
        val adminDoc = firestore.collection("admin").document(email)
        // 하위 컬렉션 menu에서 메뉴 문서 참조
        val menuDoc = adminDoc.collection("menu").document(menuName)

        Firebase.firestore.collection("articles").document(menuDoc.path)
            .set(menuModel)
            .addOnSuccessListener {
                findNavController().navigate(R.id.adminMenuListFragment)
                hideProgress()
            }.addOnFailureListener {
                it.printStackTrace()
                view?.let { view ->
                    Snackbar.make(view, "메뉴 등록에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                }
                hideProgress()
            }
    }

}