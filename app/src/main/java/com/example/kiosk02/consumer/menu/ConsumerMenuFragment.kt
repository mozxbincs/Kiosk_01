package com.example.kiosk02.consumer.menu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.admin.menu.data.MenuModel
import com.example.kiosk02.databinding.FragmentConsumerMenuFragmentBinding
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ConsumerMenuFragment(private val bundle: Bundle?) : Fragment() {

    private lateinit var binding: FragmentConsumerMenuFragmentBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var category: String? = null

    private var isOrderPlaced = false // 주문 여부를 나타내는 플래그 추가
    private var Aemail: String? = null
    private var Uemail: String? = null
    private var selectedTableId: String? = null
    private var selectedFloor: String? = null

    // Shared ViewModel
    private lateinit var navigationViewModel: NavigationViewModel

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String, bundle: Bundle?): ConsumerMenuFragment {
            return ConsumerMenuFragment(bundle).apply {
                arguments = Bundle(bundle).apply {
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

        // Shared ViewModel 초기화
        navigationViewModel = ViewModelProvider(requireActivity()).get(NavigationViewModel::class.java)

        setupRecyclerView()

        loadMenuItemsForCategory(category)

        Aemail = arguments?.getString("Aemail")
        Uemail = arguments?.getString("Uemail")
        selectedTableId = arguments?.getString("selectedTableId")
        selectedFloor = arguments?.getString("selectedFloor")
    }

//    override fun onResume() {
//        super.onResume()
//        // Fragment로 돌아올 때 select 생성
//        if (!Aemail.isNullOrEmpty() && !selectedTableId.isNullOrEmpty() && !selectedFloor.isNullOrEmpty()) {
//            createSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
//                .addOnSuccessListener {
//                    Log.d("ConsumerMenuFragment", "select 재생성 완료")
//                }
//                .addOnFailureListener { e ->
//                    Log.e("ConsumerMenuFragment", "select 재생성 실패", e)
//                }
//        }
//    }

//    override fun onPause() {
//        super.onPause()
//        // 주문 완료 상태에서는 onPause 동작 방지
//        if (navigationViewModel.isOrderPlaced.value == true) {
//            Log.d("ConsumerOrderFragment", "Order placed - Skipping onPause actions")
//            return
//        }
//
//        if (!isOrderPlaced &&
//            !Aemail.isNullOrEmpty() &&
//            !selectedTableId.isNullOrEmpty() &&
//            !selectedFloor.isNullOrEmpty()
//        ) {
//            deleteSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
//                .addOnSuccessListener {
//                    Log.d("ConsumerMenuFragment", "select 삭제 완료")
//                }
//                .addOnFailureListener { e ->
//                    Log.e("ConsumerMenuFragment", "select 삭제 실패", e)
//                }
//        }
//    }

    private fun createSelectCollection(Aemail: String, floor: String, tableId: String): Task<Void> {
        val selectDocRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floor)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(Uemail!!)

        return selectDocRef.set(mapOf("select" to true))
    }

    private fun deleteSelectCollection(Aemail: String, floor: String, tableId: String): Task<Void> {
        val selectDocRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floor)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(Uemail!!)

        return selectDocRef.delete()
    }

    private fun setupRecyclerView() {
        val adapter = ConsumerMenuListAdapter { menuModel ->
            // 메뉴 클릭 시, NavigationViewModel을 통해 isNavigated 설정
            navigationViewModel.setNavigated(true)
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