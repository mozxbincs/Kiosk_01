package com.example.kiosk02.admin.menu.data

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentManageCategoriesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ManageCategoriesFragment : Fragment(R.layout.fragment_manage_categories) {

    private lateinit var binding: FragmentManageCategoriesBinding
    private lateinit var categoriesAdapter: CategoriesAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManageCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // Firestore에서 카테고리 가져오기
        loadCategories()

        // 카테고리 추가 버튼 클릭 시
        binding.addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_edit_menu_fragment)
        }
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter{ category ->
            // 카테고리 삭제
            showDeleteCategoryDialog(category)
        }
        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoriesRecyclerView.adapter = categoriesAdapter
    }

    private fun loadCategories() {
        getAdminDocument().collection("category").get()
            .addOnSuccessListener { documents ->
                val categories = documents.map { it.getString("name") ?: "" }
                categoriesAdapter.submitList(categories)
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "카테고리 목록을 불러오는데 실패했습니다.", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun showAddCategoryDialog() {
        val input = EditText(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("카테고리 추가")
            .setView(input)
            .setPositiveButton("추가") { dialog, _ ->
                val categoryName = input.text.toString().trim()
                if (categoryName.isEmpty()) {
                    Snackbar.make(binding.root, "카테고리명을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
                } else {
                    addCategory(categoryName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun addCategory(categoryName: String) {
        // 중복 카테고리 검사
        getAdminDocument().collection("category").whereEqualTo("name", categoryName).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val cateDoc = getAdminDocument().collection("category").document(categoryName)
                    val category = hashMapOf("name" to categoryName)

                    cateDoc.set(category)  // 문서에 데이터 추가
                        .addOnSuccessListener {
                            Snackbar.make(binding.root, "카테고리가 추가되었습니다.", Snackbar.LENGTH_SHORT).show()
                            loadCategories() // 카테고리 리스트 업데이트
                        }
                        .addOnFailureListener {
                            Snackbar.make(binding.root, "카테고리 추가에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                        }
                } else {
                    Snackbar.make(binding.root, "이미 존재하는 카테고리입니다.", Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "카테고리 추가 중 오류가 발생했습니다.", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteCategoryDialog(category: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("카테고리 삭제")
            .setMessage("카테고리 '$category'를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { dialog, _ ->
                deleteCategory(category)
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteCategory(category: String) {

        getAdminDocument().collection("category").whereEqualTo("name", category).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    getAdminDocument().collection("category").document(document.id).delete()
                        .addOnSuccessListener {
                            Snackbar.make(binding.root, "카테고리가 삭제되었습니다.", Snackbar.LENGTH_SHORT).show()
                            loadCategories() // 카테고리 리스트 업데이트
                        }
                        .addOnFailureListener {
                            Snackbar.make(binding.root, "카테고리 삭제에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "카테고리 삭제 중 오류가 발생했습니다.", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun getAdminDocument(): DocumentReference {
        val email = getUserEmail()
        return firestore.collection("admin").document(email)
    }

    private fun getUserEmail():String{
        return user?.email.toString()
    }
}