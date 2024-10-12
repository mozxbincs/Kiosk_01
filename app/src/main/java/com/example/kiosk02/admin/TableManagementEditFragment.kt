package com.example.kiosk02.admin

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class TableManagementEditFragment : Fragment(R.layout.activity_table_management_edit) {
    private lateinit var tableList: LinearLayout
    private lateinit var tableFrame: FrameLayout
    private lateinit var removeTableButton: Button
    private var addedTables: MutableList<View> = mutableListOf() // 추가된 테이블 목록
    private var droppedTables: MutableList<View> = mutableListOf() // 드롭된 테이블 목록
    private var maxTablesInList = 6 // tableList에 추가될 수 있는 최대 테이블 수
    private var selectedTable: View? = null // 선택된 테이블
    private val db = FirebaseFirestore.getInstance() // Firestore 인스턴스
private lateinit var removeFloorButton:Button
    //
    private lateinit var floorSpinner: Spinner
    private lateinit var floorTextView: TextView
    private lateinit var addFloorButton: Button
    private var currentFloorCount = 0 // 현재 층수 저장



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        floorSpinner = view.findViewById(R.id.floor_spinner)

        // Firebase Firestore에서 floorCount 데이터를 불러옴
        loadFloorsFromFirestore()


        //
        tableList = view.findViewById(R.id.table_list)
        tableFrame = view.findViewById(R.id.table_frame)
        removeTableButton = view.findViewById(R.id.remove_table_button)

        removeTableButton.visibility = View.GONE



        // 삭제 버튼 클릭 이벤트
        removeTableButton.setOnClickListener {
            selectedTable?.let { tableToRemove ->
                // FrameLayout에서 선택된 테이블 제거
                removeTableFromFrame(tableToRemove) // tableFrame에서 테이블 삭제
                // 추가된 테이블 목록에서 제거
                addedTables.remove(tableToRemove)
                selectedTable = null // 선택 해제
                removeTableButton.visibility = View.GONE // 삭제 버튼 숨기기
                Toast.makeText(requireContext(), "테이블이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(requireContext(), "삭제할 테이블이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        view.findViewById<TextView>(R.id. back_activity_admin).setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_activity) // 관리자 초기 화면으로 이동
        }

        // 1인 테이블 TextView 클릭 이벤트
        view.findViewById<TextView>(R.id.one_seater_text).setOnClickListener {
            showQuantityInputDialog("1")
        }

        // 2인 테이블 TextView 클릭 이벤트
        view.findViewById<TextView>(R.id.two_seater_text).setOnClickListener {
            showQuantityInputDialog("2")
        }

        // 3인 테이블 TextView 클릭 이벤트
        view.findViewById<TextView>(R.id.three_seater_text).setOnClickListener {
            showQuantityInputDialog("3")
        }

        // 4인 테이블 TextView 클릭 이벤트
        view.findViewById<TextView>(R.id.four_seater_text).setOnClickListener {
            showQuantityInputDialog("4")
        }

        // 기타 테이블 클릭 리스너
        view.findViewById<TextView>(R.id.other_seater_text).setOnClickListener {
            showQuantityPeopleInputDialog()
        }

        // 층수 추가 및 제거 버튼에 대한 클릭 리스너 설정
        view.findViewById<Button>(R.id.add_floor_button).setOnClickListener {
            addFloor() // 층 추가
        }
        view.findViewById<Button>(R.id.remove_floor_button).setOnClickListener {
            removeFloor() // 층 제거
        }



        // 삭제 버튼 클릭 이벤트
        view.findViewById<Button>(R.id.delete_table_button).setOnClickListener {
            if (addedTables.isEmpty()) {
                Toast.makeText(requireContext(), "삭제할 테이블이 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 맨 오른쪽에 위치한 테이블 삭제
                var rightmostTable: View? = null
                var rightmostX = Float.MIN_VALUE

                // FrameLayout에서 모든 자식 뷰를 순회하여 가장 오른쪽에 위치한 뷰 찾기
                for (i in 0 until tableList.childCount) {
                    val table = tableList.getChildAt(i)
                    val tableX = table.x + table.width // 테이블의 오른쪽 끝 좌표

                    if (tableX > rightmostX) {
                        rightmostX = tableX
                        rightmostTable = table
                    }
                }

                rightmostTable?.let { tableToRemove ->
                    tableList.removeView(tableToRemove) // 테이블 삭제
                    addedTables.remove(tableToRemove) // 목록에서도 제거
                    Toast.makeText(requireContext(), "테이블이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "삭제할 테이블이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

//        view.findViewById<Button>(R.id.remove_table_button).setOnClickListener {
//
//
//        }
        setupTableFrameForDrop()

    }
//



    private fun getSelectedFloor(): String {
        return floorSpinner.selectedItem.toString() // 선택된 플로어 가져오기
    }

    // Firestore에 테이블 정보 저장
    // Firestore에 테이블 정보 저장
    private fun saveTableToFirestore(tableType: String, tableQuantity: Int) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val selectedFloor = getSelectedFloor() // 스피너에서 선택된 값 가져오기

        // Firestore에서 이미 저장된 테이블의 개수를 먼저 확인
        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .get()
            .addOnSuccessListener { result ->
                // 이미 존재하는 테이블 수를 가져옴
                val existingTableCount = result.size()

                // 새로 추가할 테이블의 ID는 기존 테이블 수 + 1부터 시작
                val startingTableNumber = existingTableCount + 1

                // 테이블 수량만큼 Firestore에 테이블 정보 저장
                for (i in 0 until tableQuantity) {
                    val tableId = "table_${startingTableNumber + i}" // 테이블 문서 ID 생성

                    // 저장할 테이블 데이터
                    val tableData = hashMapOf(
                        "tableType" to "${tableType}인",  // 테이블 종류 (몇 인 테이블)
                        "tableNumber" to (startingTableNumber + i),  // 테이블 번호
                        "x" to 0,  // 기본 x 좌표 (드래그 후 업데이트)
                        "y" to 0   // 기본 y 좌표 (드래그 후 업데이트)
                    )

                    // Firestore에 데이터 저장
                    db.collection("admin")
                        .document(userEmail)
                        .collection("floors")
                        .document(selectedFloor)
                        .collection("tables")
                        .document(tableId) // 문서 ID (table_1, table_2, ...)
                        .set(tableData)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Table ${startingTableNumber + i} successfully written!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error writing table ${startingTableNumber + i}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting documents", e)
            }
    }


///////////////////////

    private fun loadTablesForSelectedFloor(selectedFloor: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        tableFrame.removeAllViews() // 기존에 추가된 테이블 도형 삭제

        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val tableId = document.getString("tableId") ?: "0" // 기본값 설정
                    val x = document.getLong("x")?.toInt() ?: 0
                    val y = document.getLong("y")?.toInt() ?: 0
                    val tableType = document.getString("tableType") ?: "Unknown"

                    // 테이블 ID를 숫자로 변환, 실패 시 기본값 사용
                    val numericTableId = try {
                        tableId.toInt()
                    } catch (e: NumberFormatException) {
                        Log.e("Error", "Invalid tableId format: $tableId", e)
                        0 // 기본값 설정
                    }

                    // 테이블 도형을 생성하여 좌표에 배치
                    val tableView = createTableView(tableType, numericTableId).apply {
                        tag = tableId
                    }

                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = x
                        topMargin = y
                    }

                    tableFrame.addView(tableView, layoutParams)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting table data", e)
            }
    }


    // 테이블 도형을 생성하는 함수 (예시)
    // 테이블 뷰에 드래그 가능하도록 설정
    private fun createTableView(tableType: String, tableNumber: Int): View {
        val tableView = TextView(requireContext()).apply {
            text = "$tableType _ 테이블 $tableNumber"
            textSize = 18f
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.table_shape)

            // 드래그 시작
            setOnLongClickListener {
                val dragShadow = View.DragShadowBuilder(it)
                it.startDragAndDrop(null, dragShadow, it, 0)
                it.visibility = View.INVISIBLE
                true
            }

            // 태그 설정 (테이블 ID로 설정)
            tag = tableNumber.toString() // 또는 tableType을 사용할 수 있습니다.
        }
        return tableView
    }




    private fun updateFloorSpinner(floorOptions: Array<String>) {
        // ArrayAdapter를 사용해 Spinner에 값을 연결합니다.
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floorOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        floorSpinner.adapter = adapter

        // Spinner 아이템 선택 리스너 설정
        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFloor = parent.getItemAtPosition(position).toString()
                // 사용자가 층을 선택할 때 테이블 목록을 로드
                loadTablesForSelectedFloor(selectedFloor)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택하지 않았을 때는 처리 없음
            }
        }
    }

    private fun fetchFloorsFromFirestore() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        db.collection("admin").document(userEmail).collection("floors")
            .get()
            .addOnSuccessListener { documents ->
                val floorOptions = documents.map { it.id }.toTypedArray() // 층 이름 가져오기
                updateFloorSpinner(floorOptions) // 층 스피너 업데이트
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting floors", e)
            }
    }
//////////////////////////


    //파이어베이스 연동
// Firestore에서 totalfloorCount 필드를 가져와 Spinner에 적용하는 함수
    private fun loadFloorsFromFirestore() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Firestore에서 모든 층을 가져온 후 스피너 업데이트
                val floorOptions = querySnapshot.documents.map { document ->
                    document.id //
                }.toTypedArray()
                updateFloorSpinner(floorOptions)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load floors: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    private fun addFloor() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val floorsCollection = db.collection("admin").document(userEmail).collection("floors")

        // Firestore에서 현재 층 목록을 가져옴
        floorsCollection.get()
            .addOnSuccessListener { querySnapshot ->
                // Firestore에 저장된 층의 개수를 가져옴
                val existingFloorCount = querySnapshot.size()

                if (existingFloorCount >= 5) {
                    Toast.makeText(requireContext(), "더 이상 층을 추가할 수 없습니다. 최대 5개 층만 가능합니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 새로운 층 추가
                val newFloorNumber = existingFloorCount + 1
                val newFloorDoc = "floor-$newFloorNumber"
                val floorData = hashMapOf("exists" to true)

                floorsCollection.document(newFloorDoc)
                    .set(floorData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "$newFloorDoc 추가됨", Toast.LENGTH_SHORT).show()
                        updateTotalFloorCount(newFloorNumber) // 총 층수 업데이트
                        loadFloorsFromFirestore() // 스피너를 업데이트하기 위해 층수 다시 로드
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "층 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "층 목록을 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }









    private fun removeFloor() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val floorsCollection = db.collection("admin").document(userEmail).collection("floors")

        // Firestore에서 현재 층 목록을 가져옴
        floorsCollection.get()
            .addOnSuccessListener { querySnapshot ->
                val existingFloorCount = querySnapshot.size()

                if (existingFloorCount == 0) {
                    Toast.makeText(requireContext(), "삭제할 층이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 삭제할 층의 문서 이름
                val floorToRemove = "floor-$existingFloorCount"

                // Firestore에서 해당 층 문서 삭제
                floorsCollection.document(floorToRemove).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "$floorToRemove 삭제됨", Toast.LENGTH_SHORT).show()

                        // Firestore에서 삭제 후, totalFloorCount를 업데이트
                        updateTotalFloorCount(existingFloorCount - 1)
                        loadFloorsFromFirestore() // 스피너를 다시 로드하여 반영
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "층 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "층 목록을 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun updateTotalFloorCount(newCount: Int) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        db.collection("admin").document(userEmail).update("totalFloorCount", newCount)
            .addOnSuccessListener {
                // 층수 업데이트 성공
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update floor count: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // floorCount 값을 바탕으로 Spinner에 값을 설정하는 함수
    private fun setFloorSpinnerValues(totalFloorCount: Int) {
        // 1부터 floorCount까지의 숫자를 배열로 만듭니다.
        val floorOptions = (1..totalFloorCount).map { String.format("%d층", it) }.toTypedArray()

        // ArrayAdapter를 사용해 Spinner에 값을 연결합니다.
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floorOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        floorSpinner.adapter = adapter
    }


    // 수량 입력 다이얼로그를 표시하는 함수
    private fun showQuantityInputDialog(seaterType: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("$seaterType 인 테이블 수량 입력")

        // 수량 입력을 위한 EditText 생성
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton("확인") { dialog, which ->
            val count = input.text.toString().toIntOrNull() ?: 0
            if (canAddMoreTablesToList(count)) { // 추가 가능한 테이블 수 체크
                addTablesToList(count, seaterType) // 테이블 추가
            }

        }

        builder.setNegativeButton("취소") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    // 수량 입력 다이얼로그를 표시하는 함수 (기타 테이블)
    private fun showQuantityPeopleInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("기타 테이블 추가")

        // 레이아웃 생성
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // '몇 인 테이블' 입력 필드
        val seaterInput = EditText(requireContext()).apply {
            hint = "몇 인 테이블인지 입력 (5~20)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        // '테이블 수량' 입력 필드
        val quantityInput = EditText(requireContext()).apply {
            hint = "테이블 수량 입력"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        // 레이아웃에 입력 필드 추가
        layout.addView(seaterInput)
        layout.addView(quantityInput)
        builder.setView(layout)

        // 확인 버튼
        builder.setPositiveButton("확인") { dialog, which ->
            val seaterCount = seaterInput.text.toString().toIntOrNull() ?: 0
            val quantityCount = quantityInput.text.toString().toIntOrNull() ?: 0

            if (seaterCount in 5..20 && quantityCount > 0) {

                if (canAddMoreTablesToList(quantityCount)) { // 추가 가능한 테이블 수 체크
                    addTablesToList(quantityCount, seaterCount.toString())
                    saveTableToFirestore(seaterCount.toString(), quantityCount) //##
                }

            } else {
                Toast.makeText(requireContext(), "올바른 숫자를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 취소 버튼
        builder.setNegativeButton("취소") { dialog, which -> dialog.cancel() }

        // 다이얼로그 표시
        builder.show()
    }

    // FrameLayout에서 선택된 테이블을 제거하는 함수
//    private fun removeTableFromFrame(table: View) {
//        val owner = table.parent as? ViewGroup
//        owner?.removeView(table) // FrameLayout에서 테이블 제거
//    }
    // FrameLayout에서 선택된 테이블을 제거하는 함수
    private fun removeTableFromFrame(table: View) {
        val owner = table.parent as? ViewGroup
        owner?.removeView(table) // FrameLayout에서 테이블 제거

        // droppedTables에서도 제거
        droppedTables.remove(table)

        // addedTables에서도 제거
        addedTables.remove(table)
    }


    private fun canAddMoreTablesToList(count: Int): Boolean {
        // tableList에 최대 6개까지 추가 가능
        if (tableList.childCount + count <= maxTablesInList) {
            return true
        } else {
            Toast.makeText(
                requireContext(),
                "이 공간에 추가될 수 있는 테이블은 최대 $maxTablesInList 개입니다.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    @SuppressLint("MissingInflatedId")

    private fun addTablesToList(count: Int, tableType: String) {
        for (i in 1..count) {
            val tableId = "table_$i" // 테이블 ID 생성
            val tableView = LayoutInflater.from(requireContext()).inflate(R.layout.table_item, null)
            val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)

            // 테이블 이름 설정
            tableNameTextView.text = "$tableType 인"

            // 테이블 크기 및 스타일 설정 (예시로 고정 크기 사용)
            val size = (50 * resources.displayMetrics.density).toInt() // 각 테이블의 고정 크기
            val layoutParams = LinearLayout.LayoutParams(size, size).apply {
                // 여백 설정
                setMargins(8, 8, 8, 8)
            }

            // 드래그 앤 드롭을 위한 테이블에 터치 리스너 추가
            tableView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val dragData = ClipData.newPlainText("table", tableId) // 테이블 ID를 드래그 데이터로 설정
                    val dragShadowBuilder = View.DragShadowBuilder(v)
                    v.startDragAndDrop(dragData, dragShadowBuilder, v, 0)
                    v.performClick()  // 접근성 클릭
                    true
                } else {
                    false
                }
            }

            // 테이블 뷰를 LinearLayout (tableList)에 추가
            tableList.addView(tableView, layoutParams)
            addedTables.add(tableView) // 추가된 테이블을 목록에 저장
        }
    }

    // 드래그 리스너
// TableFrame에 드롭 가능하도록 설정
    private fun setupTableFrameForDrop() {
        tableFrame.setOnDragListener { view, dragEvent ->
            when (dragEvent.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> true
                DragEvent.ACTION_DRAG_EXITED -> true
                DragEvent.ACTION_DROP -> {
                    // 드래그된 뷰 가져오기
                    val droppedView = dragEvent.localState as View
                    val parent = droppedView.parent as ViewGroup
                    parent.removeView(droppedView)

                    // 드롭된 위치 계산
                    val x = dragEvent.x.toInt()
                    val y = dragEvent.y.toInt()

                    // 테이블 프레임에 드롭된 위치에 추가
                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = x
                        topMargin = y
                    }

                    tableFrame.addView(droppedView, layoutParams)
                    droppedView.visibility = View.VISIBLE

                    // 드래그된 테이블 ID 가져오기
                    val draggedTableId = (dragEvent.clipData.getItemAt(0).text.toString())

                    // 좌표 저장 로직
                    saveTablePositionToFirestore(draggedTableId, x, y)

                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!dragEvent.result) {
                        val droppedView = dragEvent.localState as View
                        droppedView.visibility = View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }
    }


    private fun saveTablePositionToFirestore(tableId: String, x: Int, y: Int) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val selectedFloor = getSelectedFloor()

        // Firestore에 좌표 정보 저장
        val tableData = hashMapOf(
            "tableId" to tableId,
            "x" to x,
            "y" to y

        )

        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .document(tableId)
            .update(tableData as Map<String, Any>, )
            .addOnSuccessListener {
                Log.d("Firestore", "Table position saved!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error saving table position", e)
            }
    }


    // tableList에서 테이블 수량 줄이는 함수
    private fun removeTableFromList(tableType: String) {
        for (i in 0 until tableList.childCount) {
            val tableView = tableList.getChildAt(i)
            val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)

            if (tableNameTextView.text.toString() == "$tableType 인") {
                tableList.removeViewAt(i) // 테이블 제거
                return
            }
        }
    }

    // 테이블 수량을 tableList에 다시 추가하는 함수
    private fun addTableToListBack(tableType: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("$tableType 인 테이블 수량 입력")

        // 수량 입력을 위한 EditText 생성
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)

        builder.setPositiveButton("확인") { dialog, which ->
            val count = input.text.toString().toIntOrNull() ?: 0
            if (canAddMoreTablesToList(count)) {
                addTablesToList(count, tableType) // 테이블 다시 추가
            }
        }

        builder.setNegativeButton("취소") { dialog, which -> dialog.cancel() }
        builder.show()
    }

}