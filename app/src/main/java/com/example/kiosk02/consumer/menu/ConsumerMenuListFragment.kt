package com.example.kiosk02.consumer.menu

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentConsumerMenuListBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.Task
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ConsumerMenuListFragment : Fragment(R.layout.fragment_consumer_menu_list) {
    private var _binding: FragmentConsumerMenuListBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    private var Aemail: String? = null
    private var orderType: String? = null
    private var selectedTableId: String? = null
    private var selectedFloor: String? = null

    private var isNavigated = false // 장바구니 버튼이나 아이템 클릭 여부 플래그
    private var isOrdered = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentConsumerMenuListBinding.bind(view)

        auth = FirebaseAuth.getInstance()

        val bundle = arguments
        Aemail = bundle?.getString("Aemail")
        orderType = bundle?.getString("orderType")
        selectedTableId = bundle?.getString("selectedTableId")
        selectedFloor = bundle?.getString("selectedFloor") // floor 값 받아오기
        isOrdered = arguments?.getBoolean("isOrderd", false) ?: false


        if (Aemail.isNullOrEmpty()) {
            Log.e("ConsumerMenuListFragment", "Aemail is missing or empty")
            Toast.makeText(requireContext(), "관리자 정보가 누락되었습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        loadCategoriesToTabs(Aemail!!)

        setupRecyclerView()

        binding.cartImageButton.setOnClickListener {
            isNavigated = true
            findNavController().navigate(R.id.action_to_ConsumerCartFragment, bundle)
        }

        binding.backButton.setOnClickListener {
            // Firestore 데이터 삭제 후 프래그먼트 전환
            if (!Aemail.isNullOrEmpty() && !selectedTableId.isNullOrEmpty() && !selectedFloor.isNullOrEmpty()) {
                deleteSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
                    .addOnSuccessListener {
                        navigateBack(orderType, bundle)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ConsumerMenuList", "Error deleting select collection: ", e)
                        Toast.makeText(requireContext(), "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
            } else {
                navigateBack(orderType, bundle)
            }
        }

        // 시스템 백 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!Aemail.isNullOrEmpty() && !selectedTableId.isNullOrEmpty() && !selectedFloor.isNullOrEmpty()) {
                        deleteSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
                            .addOnSuccessListener {
                                navigateBack(orderType, bundle)
                            }
                            .addOnFailureListener { e ->
                                Log.e("ConsumerMenuList", "Error deleting select collection: ", e)
                                Toast.makeText(
                                    requireContext(),
                                    "삭제 중 오류가 발생했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        navigateBack(orderType, bundle)
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (!Aemail.isNullOrEmpty() && !selectedTableId.isNullOrEmpty() && !selectedFloor.isNullOrEmpty()) {
            createSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
                .addOnSuccessListener {
                    Log.d("ConsumerMenuList", "select 재생성 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("ConsumerMenuList", "select 재생성 실패")
                }
        }
    }




    private fun loadCategoriesToTabs(Aemail: String) {
        getAdminDocument(Aemail).collection("category")
//            .orderBy("order")
            .get()
            .addOnSuccessListener { documents ->
                Log.e("TabLayout", "탭 레이아웃 설정 성공")
                val categories = documents.map { it.getString("name") ?: "" }
                setupTabLayoutWithViewPager(categories, arguments)
            }.addOnFailureListener {
                Log.e("TabLayout", "탭 레이아웃 설정 실패")
                Toast.makeText(requireContext(), "카테고리 로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupTabLayoutWithViewPager(categories: List<String>, bundle: Bundle?) {
        val viewPagerAdapter = ConsumerMenuPagerAdapter(this, bundle)
        viewPagerAdapter.setCategories(categories)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = viewPagerAdapter.getCategoryTitle(position)
        }.attach()
    }
    // 메뉴 아이템 클릭 시 select 삭제 되지 않도록
    private fun setupRecyclerView() {
        val adapter = ConsumerMenuListAdapter { menuModel ->
                isNavigated = true
                val bundle = Bundle(arguments).apply {
                    putParcelable("menuModel", menuModel)
                }
                findNavController().navigate(R.id.action_to_ConsumerOrderFragment, bundle)
        }
        binding.viewPager.adapter = adapter // RecyclerView에 어댑터 설정
    }

    override fun onPause() {
        super.onPause()
        // 화면을 벗어났을 때 select 삭제(to 장바구니, 메뉴 아이템 제외)
        if (!isNavigated &&
            !isOrdered &&
            !Aemail.isNullOrEmpty() &&
            !selectedTableId.isNullOrEmpty() &&
            !selectedFloor.isNullOrEmpty()
        ) {
//            findNavController().navigate(R.id.action_to_table_Select_Fragment,arguments)
            deleteSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
                .addOnSuccessListener {
                    Log.d("ConsumerMenuListFragment", "select 삭제 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("ConsumerMenuListFragment", "select 삭제 실패", e)
                }
        }
        isNavigated = false
    }

    // 경로 지정 함수
    private fun getAdminDocument(Aemail: String): DocumentReference {
        return firestore.collection("admin").document(Aemail)
    }


    private fun deleteSelectCollection(Aemail: String, floor: String, tableId: String): Task<Void> {
        val Uemail =
            auth.currentUser?.email ?: return Tasks.forException(Exception("User not logged in"))

        if (Uemail.isNullOrEmpty()) {
            Log.e("ConsumerMenuListFragment", "Uemail is null or empty")
            return Tasks.forException(Exception("Uemail is null or empty"))
        }

        val selectDocRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floor)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(Uemail)

        return selectDocRef.delete()
    }

    private fun navigateBack(orderType: String?, bundle: Bundle?) {
        if (orderType == "pickup") {
            findNavController().navigate(R.id.action_to_OderMethod, bundle)
        } else {
            findNavController().navigate(R.id.action_to_table_Select_Fragment, arguments)
        }
    }

    private fun createSelectCollection(Aemail: String, floor: String, tableId: String): Task<Void> {
        val Uemail =
            auth.currentUser?.email ?: return Tasks.forException(Exception("로그인이 되어 있지 않습니다."))

        if (Uemail.isNullOrEmpty()) {
            Log.e(
                "ConsumerMenuListFragment", "Uemail 비어있음"
            )
            return Tasks.forException(Exception("Uemail 비어있음"))
        }

        val selectDocRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floor)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(Uemail)

        // Firestore에 select 문서를 생성
        val selectData = hashMapOf("select" to true)
        return selectDocRef.set(selectData)
    }
}