package com.example.kiosk02.consumer.menu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.admin.menu.data.MenuModel
import com.example.kiosk02.databinding.FragmentConsumerMenuFragmentBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ConsumerMenuFragment(private val bundle: Bundle?) : Fragment() {

    private lateinit var binding: FragmentConsumerMenuFragmentBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var category: String? = null

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String, bundle: Bundle?): ConsumerMenuFragment {
            return ConsumerMenuFragment(bundle).apply {
                arguments = Bundle(bundle).apply { //카테고리와 충돌나지 않는지 확인해보기
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConsumerMenuFragmentBinding.inflate(inflater, container, false)
        category = arguments?.getString(ARG_CATEGORY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        loadMenuItemsForCategory(category)
    }

    private fun setupRecyclerView() {
        val adapter = ConsumerMenuListAdapter { menuModel ->
            // 메뉴 클릭 시, ConsumerOrderFragment로 이동
            val bundleWithMenuModel = Bundle(bundle).apply {
                putParcelable("menuModel", menuModel)
            }
            findNavController().navigate(R.id.action_to_ConsumerOrderFragment, bundleWithMenuModel)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 1)
        binding.recyclerview.layoutManager = gridLayoutManager
        binding.recyclerview.adapter = adapter
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
                    binding.recyclerview.visibility = View.GONE
                    binding.emptySpaceNoticeTextView.apply {
                        text = "표시할 메뉴가 없습니다."
                        visibility = View.VISIBLE
                    }
                } else {
                    // 아이템이 있는 경우 RecyclerView와 데이터 설정
                    (binding.recyclerview.adapter as ConsumerMenuListAdapter).submitList(menuItems)
                    binding.recyclerview.visibility = View.VISIBLE
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

    // 경로 지정 함수
    private fun getAdminDocument(): DocumentReference {
        val email = bundle?.getString("Aemail") ?: ""
        return firestore.collection("admin").document(email)
    }

}