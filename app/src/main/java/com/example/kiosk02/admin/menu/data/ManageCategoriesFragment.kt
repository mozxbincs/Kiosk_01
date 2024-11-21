package com.example.kiosk02.admin.menu.data

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.Callback.makeMovementFlags
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
            // 이전 BackStackEntry의 savedStateHandle에 데이터 설정
            findNavController().previousBackStackEntry?.savedStateHandle?.apply {
                // 현재 EditMenuFragment에서 입력한 데이터 가져오기
                set("menuName", arguments?.getString("menuName") ?: "")
                set("price", arguments?.getString("price") ?: "")
                set("composition", arguments?.getString("composition") ?: "")
                set("detail", arguments?.getString("detail") ?: "")
                set("imageUri", arguments?.getString("imageUri") ?: "")
                set("from_manage_categories", true)
            }
            findNavController().popBackStack() // 이전 화면으로 돌아가기
        }
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter { category ->
            // 카테고리 삭제
            showDeleteCategoryDialog(category)
        }
        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoriesRecyclerView.adapter = categoriesAdapter

        // ItemTouchHelper 연결
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = 0 // 스와이프 동작 비활성화
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // 아이템 이동 처리
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                categoriesAdapter.notifyItemMoved(fromPosition, toPosition)

                // Firestore에서 `order` 업데이트
                updateCategoryOrder(fromPosition, toPosition)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })

        itemTouchHelper.attachToRecyclerView(binding.categoriesRecyclerView)
    }
    private fun updateCategoryOrder(fromPosition: Int, toPosition: Int) {
        val currentList = categoriesAdapter.currentList.toMutableList()

        // 리스트에서 순서 변경
        val movedItem = currentList.removeAt(fromPosition)
        currentList.add(toPosition, movedItem)

        // Firestore 업데이트
        val batch = firestore.batch()
        currentList.forEachIndexed { index, category ->
            val categoryDoc = getAdminDocument().collection("category").document(category)
            batch.update(categoryDoc, "order", index)
        }

        batch.commit()
            .addOnSuccessListener {
                Snackbar.make(binding.root, "카테고리 순서가 업데이트되었습니다.", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "순서 업데이트에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun loadCategories() {
        getAdminDocument().collection("category").orderBy("order").get()
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
                    // Firestore에서 현재 카테고리 개수를 확인
                    getAdminDocument().collection("category").get()
                        .addOnSuccessListener { existingCategories ->
                            val orderValue = existingCategories.size() // 카테고리 개수로 order 설정

                            val cateDoc = getAdminDocument().collection("category").document(categoryName)
                            val category = hashMapOf(
                                "name" to categoryName,
                                "order" to orderValue // order 값 추가
                            )

                            cateDoc.set(category) // 문서에 데이터 추가
                                .addOnSuccessListener {
                                    Snackbar.make(binding.root, "카테고리가 추가되었습니다.", Snackbar.LENGTH_SHORT).show()
                                    loadCategories() // 카테고리 리스트 업데이트
                                }
                                .addOnFailureListener {
                                    Snackbar.make(binding.root, "카테고리 추가에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Snackbar.make(binding.root, "카테고리 개수를 확인하는 중 오류가 발생했습니다.", Snackbar.LENGTH_SHORT).show()
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