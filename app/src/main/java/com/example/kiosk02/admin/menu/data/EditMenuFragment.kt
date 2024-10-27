package com.example.kiosk02.admin.menu.data

import android.net.Uri
import android.os.Bundle

import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentEditMenuBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import java.util.ArrayList
import java.util.UUID

class EditMenuFragment : Fragment(R.layout.fragment_edit_menu) {
    private lateinit var binding: FragmentEditMenuBinding

    private var selectedUri: Uri? = null
    private var menuModel: MenuModel? = null

    private val user = Firebase.auth.currentUser
    private val firestore = FirebaseFirestore.getInstance()

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

        menuModel = arguments?.getParcelable("menuModel")

        if (menuModel != null) {
            //수정모드
            setupEditMode()
        } else {
            //추가모드
            setupAddMode()
        }

        binding = FragmentEditMenuBinding.bind(view)
        //이미지 추가 버튼 활성화, 제거 버튼 비활성화

        setupClearButton()

        setupPhotoImageView()

        setupClearButton()

        setupConfirmButton()

        loadCategoryList()

        setupDeleteButton()





        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_menu_list_fragment)
        }

        binding.addCategoryButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_manage_categories_fragment)
        }

    }

    private fun setupEditMode() {
        binding.titleTextview.text = "메뉴 수정"
        binding.confirmButton.text = "메뉴 수정하기"
        binding.deleteButton.isVisible = true
        binding.addImageButton.isVisible = false
        binding.clearImageButton.isVisible = true

        menuModel?.let { menu ->
            Glide.with(binding.menuImageView.context)
                .load(menu.imageUrl)
                .into(binding.menuImageView)

            selectedUri = Uri.parse(menu.imageUrl)

            //기존 메뉴 정보들 load
            binding.menuNameEditText.setText(menu.menuName)
            binding.priceEditText.setText(menu.price.toString())
            binding.compositionEditText.setText(menu.composition)
            binding.menuDetailEditText.setText(menu.detail)
        }
    }

    private fun setupAddMode() {

        binding.titleTextview.text = "메뉴 추가"
        binding.confirmButton.text = "메뉴 추가하기"
        binding.deleteButton.isVisible = false
        binding.addImageButton.isVisible = true
        binding.clearImageButton.isVisible = false

        // 이미지와 텍스트 필드 초기화
        binding.menuImageView.setImageResource(0)
        binding.menuNameEditText.text = null
        binding.priceEditText.text = null
        binding.compositionEditText.text = null
        binding.menuDetailEditText.text = null
    }

    private fun setupClearButton() {
        binding.clearImageButton.setOnClickListener {
            selectedUri = null
            binding.menuImageView.setImageURI(null)
            binding.addImageButton.isVisible = true
            binding.clearImageButton.isVisible = false
        }
    }

    private fun setupDeleteButton() {
        binding.deleteButton.setOnClickListener {
            menuModel?.let { menu ->

                //Storage 경로 설정
                val storageRef = Firebase.storage.getReferenceFromUrl(menu.imageUrl ?: "")

                // Storage 이미지 삭제
                storageRef.delete()
                    .addOnSuccessListener {
                        Log.d("DeleteStorage", "이미지가 삭제되었습니다.")
                        //Firestore 문서 삭제
                        getAdminDocument().collection("menu").document(menu.menuId ?: "")
                            .delete()
                            .addOnSuccessListener {
                                Snackbar.make(binding.root, "메뉴가 삭제되었습니다.", Snackbar.LENGTH_SHORT)
                                    .show()
                                findNavController().navigate(R.id.adminMenuListFragment)
                            }
                            .addOnFailureListener {
                                Snackbar.make(binding.root, "메뉴 삭제에 실패했습니다.", Snackbar.LENGTH_SHORT)
                                    .show()
                            }
                    }.addOnFailureListener { exception ->
                        Log.e("StorageError", "이미지 삭제 실패: ${exception.message}")
                        Snackbar.make(binding.root, "이미지 삭제에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                    }

            }
        }
    }

    private fun setupPhotoImageView() {
        binding.menuImageView.setOnClickListener {
            if (selectedUri == null) {
                startPicker()
            }
        }
    }

    private fun loadCategoryList() {
        val categoryList = ArrayList<String>()

        getAdminDocument().collection("category")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val category = document.getString("name")
                    if (category != null) {
                        categoryList.add(category)
                    }
                }
                setUpSpinner(categoryList)
            }
            .addOnFailureListener {
                Log.e("Spinner", "데이터를 불러오는데 실패하였습니다.")
            }
    }

    private fun setUpSpinner(categories: List<String>) {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter

    }

    private fun setupConfirmButton() {
        binding.confirmButton.setOnClickListener {
            showProgress()
            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadImage(
                    uri = photoUri,
                    successHandler = {
                        if (menuModel != null) {
                            // 수정 모드: 기존 메뉴 업데이트
                            updateMenu(
                                it,
                                binding.menuNameEditText.text.toString(),
                                binding.priceEditText.text.toString().toInt(),
                                binding.compositionEditText.text.toString(),
                                binding.menuDetailEditText.text.toString(),
                                binding.categorySpinner.selectedItem.toString()
                            )
                        } else {
                            // 추가 모드: 새로운 메뉴 추가
                            uploadArticle(
                                it,
                                binding.menuNameEditText.text.toString(),
                                binding.priceEditText.text.toString().toInt(),
                                binding.compositionEditText.text.toString(),
                                binding.menuDetailEditText.text.toString(),
                                binding.categorySpinner.selectedItem.toString()
                            )
                        }
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

    private fun updateMenu(
        photoUri: String,
        menuName: String,
        price: Int,
        composition: String,
        detail: String,
        category: String
    ) {
        val updatedMenuModel = MenuModel(
            imageUrl = photoUri,
            menuId = menuModel?.menuId,
            menuName = menuName,
            price = price,
            composition = composition,
            detail = detail,
            category = category,
        )

        val menuDoc = getAdminDocument().collection("menu").document(menuName)

        menuDoc.set(updatedMenuModel)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "메뉴가 수정되었습니다.", Snackbar.LENGTH_SHORT).show()
                findNavController().navigate(R.id.adminMenuListFragment)
                hideProgress()
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "메뉴 수정에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                hideProgress()
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
        val menuName = binding.menuNameEditText.text.toString()

        //파일이름
        val filename = "${menuName}.png"

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
                    Log.e("UploadError", "Image upload failed: ${task.exception?.message}")
                    errorHandler(task.exception)
                }
            }
    }

    private fun uploadArticle(
        photoUri: String,
        menuName: String,
        price: Int,
        composition: String,
        detail: String,
        category: String
    ) {
        val menuId = UUID.randomUUID().toString()
        val menuModel = MenuModel(
            imageUrl = photoUri,
            menuId = menuId,
            menuName = menuName,
            price = price,
            composition = composition,
            detail = detail,
            category = category,

            )

        val menuName = binding.menuNameEditText.text.toString()
        // 하위 컬렉션 menu에서 메뉴 문서 참조
        val menuDoc = getAdminDocument().collection("menu").document(menuName)

        menuDoc.set(menuModel)
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

    private fun getAdminDocument(): DocumentReference {
        val email = getUserEmail()
        return firestore.collection("admin").document(email)
    }

    private fun getUserEmail(): String {
        return user?.email.toString()
    }

}