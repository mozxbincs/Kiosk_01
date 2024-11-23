package com.example.kiosk02.admin.menu.data

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentMenuListBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class MenuListFragment : Fragment() {

    private lateinit var binding: FragmentMenuListBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var category: String? = null
    private val user = Firebase.auth.currentUser

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String): MenuListFragment {
            return MenuListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuListBinding.inflate(inflater, container, false)
        category = arguments?.getString(ARG_CATEGORY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        loadMenuItemsForCategory(category)
    }

    private fun setupRecyclerView() {
        val adapter = MenuListAdapter { menuModel ->
            // 메뉴 클릭 시, EditMenuFragment(수정모드)로 이동
            val bundle = Bundle().apply {
                putParcelable("menuModel", menuModel)
            }
            findNavController().navigate(R.id.action_to_edit_menu_fragment, bundle)
        }
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.menuListRecyclerView.layoutManager = gridLayoutManager
        binding.menuListRecyclerView.adapter = adapter

    }

    private fun loadMenuItemsForCategory(category: String?) {
        category ?: return

        // Firestore 데이터 로드 시작
        getAdminDocument().collection("menu")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { documents ->
                val menuItems = documents.toObjects(MenuModel::class.java)

                if (menuItems.isEmpty()) {
                    // 아이템이 없는 경우 ProgressBar를 숨기고, RecyclerView도 숨김
                    binding.progressBarLayout.visibility = View.GONE
                    binding.menuListRecyclerView.visibility = View.GONE
                    binding.emptySpaceNoticeTextView.apply {
                        text = "표시할 메뉴가 없습니다."
                        visibility = View.VISIBLE
                    }
                } else {
                    // 아이템이 있는 경우 RecyclerView와 데이터 설정
                    (binding.menuListRecyclerView.adapter as MenuListAdapter).submitList(menuItems)
                    binding.menuListRecyclerView.visibility = View.VISIBLE
                    binding.emptySpaceNoticeTextView.visibility = View.GONE
                }

            }
            .addOnFailureListener {
                // 데이터 로드 실패 시 메시지 표시
                Snackbar.make(binding.root, "데이터를 불러오는데 실패했습니다.", Snackbar.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                // 로드 완료 후 ProgressBar 숨기기
                binding.progressBarLayout.visibility = View.GONE
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