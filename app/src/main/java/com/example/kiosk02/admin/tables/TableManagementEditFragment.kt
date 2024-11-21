package com.example.kiosk02.admin.tables

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
import android.widget.ImageButton
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

class TableManagementEditFragment : Fragment(R.layout.activity_table_management_edit) {
    private lateinit var tableList: LinearLayout
    private lateinit var tableFrame: FrameLayout
    private lateinit var removeTableButton: LinearLayout
    private var addedTables: MutableList<View> = mutableListOf() // 추가된 테이블 목록
    private var droppedTables: MutableList<View> = mutableListOf() // 드롭된 테이블 목록
    private var maxTablesInList = 6 // tableList에 추가될 수 있는 최대 테이블 수
    private var selectedTable: View? = null // 선택된 테이블
    private val db = FirebaseFirestore.getInstance() // Firestore 인스턴스

    //
    private lateinit var floorSpinner: Spinner
    private var quantityCount: Int = 0
    private var lastSelectedTableType: String = ""
    private var maxTableId: Int = 0 // 최대 테이블 ID를 저장할 변수
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        floorSpinner = view.findViewById(R.id.floor_spinner)
        floorSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedFloor = parent.getItemAtPosition(position) as String
                loadTablesFromFirestore(selectedFloor) // 선택한 층에 대한 테이블 로드
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무 것도 선택되지 않았을 때의 처리
            }
        })
        // Firebase Firestore에서 floorCount 데이터를 불러옴
        loadFloorsFromFirestore()
        initializeMaxTableId { maxId ->
            maxTableId = maxId // 최대 테이블 ID 설정
            Log.d("Firestore", "Max table ID is now $maxTableId")
        }

        //
        tableList = view.findViewById(R.id.table_list)
        tableFrame = view.findViewById(R.id.table_frame)
        removeTableButton = view.findViewById(R.id.remove_table_button)

        removeTableButton.visibility = View.VISIBLE



        // 삭제 버튼 클릭 이벤트
// 삭제 버튼 클릭 이벤트
        removeTableButton.setOnClickListener {
            selectedTable?.let { tableToRemove ->
                // Firestore에서 선택된 테이블 삭제
                deleteTableFromFirestore(tableToRemove.tag.toString())
                // UI에서 선택된 테이블 제거
                removeTableFromFrame(tableToRemove) // tableFrame에서 테이블 삭제
                addedTables.remove(tableToRemove) // 추가된 테이블 목록에서 제거
                selectedTable = null // 선택 해제
                removeTableButton.visibility = View.VISIBLE // 삭제 버튼 숨기기

            } ?: run {
                Toast.makeText(requireContext(), "삭제할 테이블이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<TextView>(R.id. back_activity_admin).setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_activity) // 관리자 초기 화면으로 이동
        }
        // 기타 테이블 클릭 리스너
        view.findViewById<LinearLayout>(R.id.other_seater_text).setOnClickListener {
            showQuantityPeopleInputDialog()
        }

        // 층수 추가 및 제거 버튼에 대한 클릭 리스너 설정
        view.findViewById<ImageButton>(R.id.add_floor_button).setOnClickListener {
            addFloor() // 층 추가
        }
        view.findViewById<ImageButton>(R.id.remove_floor_button).setOnClickListener {
            removeFloor() // 층 제거
        }



        // 삭제 버튼 클릭 이벤트
        view.findViewById<ImageButton>(R.id.delete_table_button).setOnClickListener {
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

        setupTableFrameForDrop()
        onAppStart()
    }
//



    private fun getSelectedFloor(): String {
        return floorSpinner.selectedItem.toString() // 선택된 플로어 가져오기
    }





    private fun saveTableToFirestore(tableType: String, tableQuantity: Int, x: Int, y: Int, existingTableId: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val selectedFloor = getSelectedFloor()

        val tableData = hashMapOf(
            "tableType" to tableType,
            "tableNumber" to existingTableId.split("_")[1].toInt(), // 기존 ID에서 테이블 번호 추출
            "x" to x,
            "y" to y
        )

        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .document(existingTableId) // 여기서 기존 ID로 저장
            .set(tableData)
            .addOnSuccessListener {
                Log.d("Firestore", "Table $existingTableId successfully written at ($x, $y)!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing table $existingTableId", e)
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
                    val tableId = document.id // 기본값 설정
                    val x = document.getLong("x")?.toInt() ?: 0
                    val y = document.getLong("y")?.toInt() ?: 0
                    val tableType = document.getString("tableType") ?: "Unknown"

                    // 테이블 도형을 생성하여 좌표에 배치
                    val tableView = createTableView(tableType, tableId).apply {
                        tag = tableId // 태그를 tableId로 설정
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

                // 모든 테이블 로드 후 최대 테이블 ID 업데이트

            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting table data", e)
            }
    }
    private fun fetchMaxTableId() {
        initializeMaxTableId { maxId ->
            maxTableId = maxId // 최대 테이블 ID 설정
            Log.d("Firestore", "Max table ID is now $maxTableId")

            // 최대 ID가 설정된 후 테이블을 추가
            addNewTables(quantityCount, lastSelectedTableType)
        }
    }
    private fun onAppStart() {
        fetchMaxTableId() // 최대 ID 가져오기
        loadFloorsFromFirestore() // 층 로드
    }
    // 테이블 도형을 생성하는 함수 (예시)
// 테이블 뷰에 드래그 가능하도록 설정
    @SuppressLint("ClickableViewAccessibility")
    private fun createTableView(tableType: String, tableId: String): View {
        val tableView = TextView(requireContext()).apply {
            text = "$tableType" // 예: "3 인 테이블 1"
            textSize = 18f
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.table_shape)

            var isDragging = false
            var downX = 0f
            var downY = 0f

            // 드래그 및 클릭 리스너 설정
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        isDragging = false // 초기화
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val distanceX = Math.abs(event.x - downX)
                        val distanceY = Math.abs(event.y - downY)
                        if (distanceX > 10 || distanceY > 10) { // 10px 이상 움직이면 드래그로 인식
                            isDragging = true
                            val dragData = ClipData.newPlainText("table", tableId)
                            val dragShadowBuilder = View.DragShadowBuilder(v)
                            v.startDragAndDrop(dragData, dragShadowBuilder, v, 0)
                            v.visibility = View.INVISIBLE
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // 클릭으로 인식
                            selectedTable = this // 클릭한 테이블을 선택 상태로 변경
                            removeTableButton.visibility = View.VISIBLE // 삭제 버튼 표시
                        }
                        true
                    }
                    else -> false
                }
            }
//드래그 기능 추가하니까 클릭이 안먹혀서 삭제할 테이블이 없다고 뜸-> 클릭과 드래그를 구분



            // 드래그 리스너 설정
            setOnDragListener { view, dragEvent ->
                when (dragEvent.action) {
                    DragEvent.ACTION_DRAG_ENDED -> {
                        if (dragEvent.result) {
                            // 드래그가 성공적으로 종료되면, 테이블 위치를 Firestore에 업데이트
                            val newX = (view.layoutParams as FrameLayout.LayoutParams).leftMargin
                            val newY = (view.layoutParams as FrameLayout.LayoutParams).topMargin
                            updateTablePosition(tableId, newX, newY)
                        }
                        view.visibility = View.VISIBLE
                        true
                    }
                    else -> false
                }
            }

            // 태그 설정 (테이블 ID로 설정)
            tag = tableId

            // 초기 클릭 리스너 설정 (드래그가 없을 경우)
            setOnClickListener {
                selectedTable = this // 클릭한 테이블을 선택 상태로 변경
                removeTableButton.visibility = View.VISIBLE // 삭제 버튼 표시
            }
        }
        return tableView
    }





    private fun updateFloorSpinner(floorOptions: Array<String>) {
        // ArrayAdapter를 사용해 Spinner에 값을 연결
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floorOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        floorSpinner.adapter = adapter

        // Spinner 아이템 선택 리스너 설정
        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFloor = parent.getItemAtPosition(position).toString()
                // 사용자가 층을 선택할 때 테이블 목록을 로드
                loadTablesForSelectedFloor(selectedFloor)
                fetchMaxTableId() // 최대 테이블 ID를 가져오기
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택하지 않았을 때는 처리 없음
            }
        }
    }




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







    // 수량 입력 다이얼로그를 표시하는 함수 (기타 테이블)
    private fun showQuantityPeopleInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("테이블 추가")

        // 레이아웃 생성
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // '몇 인 테이블' 입력 필드
        val seaterInput = EditText(requireContext()).apply {
            hint = "몇 인 테이블인지 입력 (1~20)"
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

            if (seaterCount in 1..20 && quantityCount > 0) {
                // 현재 테이블 수가 6개 이하인지 확인
                if (canAddMoreTablesToList(quantityCount)) {
                    lastSelectedTableType = "$seaterCount 인" // 테이블 타입 저장
                    addTablesToList(quantityCount, lastSelectedTableType) // 테이블 타입 전달
                } else {
                    Toast.makeText(requireContext(), "최대 6개의 테이블만 추가할 수 있습니다.", Toast.LENGTH_SHORT).show()
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

            return false
        }
    }
//테이블 아이디 1부터 시작되는 문제 해결(나갔다 들어오면)->1부터 시작 부분
    @SuppressLint("MissingInflatedId")

    private fun addTablesToList(count: Int, tableType: String) {
        val existingTableCount = addedTables.size // 현재 추가된 테이블 수

        for (i in 0 until count) {
            val tableId = maxTableId + existingTableCount + i + 1  // 1부터 시작

            val tableView = LayoutInflater.from(requireContext()).inflate(R.layout.table_item, null)
            val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)

            // 테이블 이름 설정
            tableNameTextView.text = tableType

            // 테이블 ID를 태그로 설정
            tableView.tag = "table_$tableId" // 태그 설정

            // 레이아웃 설정
            val size = (50 * resources.displayMetrics.density).toInt()
            val layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(8, 8, 8, 8)
            }
            tableView.layoutParams = layoutParams

            // 드래그 앤 드롭 리스너
            tableView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val dragData = ClipData.newPlainText("table", "${tableView.tag};$tableType")
                    val dragShadowBuilder = View.DragShadowBuilder(v)
                    v.startDragAndDrop(dragData, dragShadowBuilder, v, 0)
                    v.performClick()
                    true
                } else {
                    false
                }
            }
            // 클릭 리스너 추가
            tableView.setOnClickListener {
                selectedTable = tableView // 선택된 테이블 저장
                removeTableButton.visibility = View.VISIBLE // 삭제 버튼 보이기
            }

            tableList.addView(tableView)
            addedTables.add(tableView) // 추가된 테이블을 목록에 저장
        }
    }


    private fun deleteTableFromFirestore(tableId: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val selectedFloor = getSelectedFloor() ?: return

        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .document(tableId) // 드롭한 테이블 ID로 Firestore에서 삭제
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Table $tableId successfully deleted!")
                Toast.makeText(requireContext(), "테이블이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting table $tableId", e)
                Toast.makeText(requireContext(), "테이블 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    //나갔다 들어왔을때, 층수 변동시 아이디 끼리 안겹치게 하기 위해 문서내 테이블 중 가장 큰값을 찾도록 함
    private fun initializeMaxTableId(callback: (Int) -> Unit) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        // 모든 층의 최대 테이블 ID를 찾기 위한 리스트
        val maxTableIds = mutableListOf<Int>()

        // Firestore에서 층 목록을 가져오기
        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .get()
            .addOnSuccessListener { floors ->
                // 모든 층에서 테이블 ID를 가져오기 위한 카운터
                var floorsProcessed = 0

                for (floor in floors.documents) {
                    // 각 층의 테이블 ID를 가져오기
                    val floorId = floor.id
                    db.collection("admin")
                        .document(userEmail)
                        .collection("floors")
                        .document(floorId)
                        .collection("tables")
                        .get()
                        .addOnSuccessListener { tables ->
                            for (table in tables.documents) {
                                val tableId = table.id // 테이블 ID는 문서 ID
                                val numericId = tableId.substringAfter("table_").toIntOrNull()
                                if (numericId != null) {
                                    maxTableIds.add(numericId)
                                }
                            }

                            // 모든 층을 확인한 후 최대 ID를 찾기
                            floorsProcessed++
                            if (floorsProcessed == floors.size()) {
                                val maxId = maxTableIds.maxOrNull() ?: 0
                                callback(maxId) // 최대 ID를 반환
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error getting tables for floor $floorId", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting floors", e)
            }
    }



    private fun addNewTables(quantityCount: Int, tableType: String) {
        val newIdBase = maxTableId // 여기서 maxTableId를 사용
        for (i in 0 until quantityCount) {
            val newTableId = "table_${newIdBase + i + 1}" // ID 생성
            // 테이블 추가 로직...
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
                    val droppedView = dragEvent.localState as View
                    val parent = droppedView.parent as ViewGroup
                    parent.removeView(droppedView)

                    val x = dragEvent.x.toInt()
                    val y = dragEvent.y.toInt()

                    // tableView에서 tableId 가져오기
                    val draggedTableId = droppedView.tag as String // tableView의 태그에서 ID 가져오기

                    // clipData의 길이를 체크하여 안전하게 접근
                    if (dragEvent.clipData.itemCount > 0) {
                        val tableType = dragEvent.clipData.getItemAt(0).text.toString().split(";").getOrNull(1) ?: "unknown"

                        // Firestore에 해당 테이블의 원래 ID로 저장
                        checkTableExists(draggedTableId, tableType, x, y, droppedView)

                        val layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            leftMargin = x
                            topMargin = y
                        }

                        // 테이블 뷰를 테이블 프레임에 추가
                        tableFrame.addView(droppedView, layoutParams)
                        droppedView.visibility = View.VISIBLE
                        return@setOnDragListener true
                    } else {
                        Log.w("DragEvent", "No clip data available.")
                        return@setOnDragListener false
                    }
                }
                else -> false
            }
        }
    }
//파이어베이스에 존재했을시에는 위치 값만 업데이트 해야하므로 체킹해주는 함수 필요
// ->드롭시 무조건 테이블 생성되게 되니까 같은 테이블을 위치 옮기는 건데도 테이블이 새로생성
    //->따라서 존재하는건지여부를 확인하게 함

    private fun checkTableExists(tableId: String, tableType: String, x: Int, y: Int, droppedView: View) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val selectedFloor = getSelectedFloor() ?: return

        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .document(tableId) // 드롭한 테이블 ID로 Firestore에서 확인
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    updateTablePosition(tableId, x, y) // 위치 업데이트
                } else {
                    // Firestore에 새 테이블 생성 (드롭 후)
                    saveTableToFirestore(tableType, 1, x, y, tableId) // 드롭한 테이블 ID로 저장
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error checking table existence", e)
            }
    }



    private fun loadTablesFromFirestore(floorId: String) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(floorId)
            .collection("tables")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val tableId = document.id
                    val tableType = document.getString("type") ?: "기타 테이블"
                    val x = document.getLong("x")?.toInt() ?: 0
                    val y = document.getLong("y")?.toInt() ?: 0

                    // 테이블 뷰 생성
                    val tableView = LayoutInflater.from(requireContext()).inflate(R.layout.table_item, null)
                    val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)
                    tableNameTextView.text = tableType
                    tableView.tag = tableId // 테이블 ID를 태그로 설정

                    // 클릭 리스너 추가
                    tableView.setOnClickListener {
                        removeTableFromFirestore(tableId, tableView)
                    }

                    // 레이아웃 설정
                    val layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = x
                        topMargin = y
                    }

                    // 테이블 뷰를 테이블 프레임에 추가
                    tableFrame.addView(tableView, layoutParams)
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting tables", e)
            }
    }

    private fun removeTableFromFirestore(tableId: String, tableView: View) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val selectedFloor = getSelectedFloor() ?: return

        // Firestore에서 테이블 삭제
        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .document(tableId)
            .delete()
            .addOnSuccessListener {
                // UI에서 테이블 뷰 제거
                tableFrame.removeView(tableView)
                Toast.makeText(requireContext(), "테이블이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting table", e)
            }
    }


    private fun updateTablePosition(tableId: String, x: Int, y: Int) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val selectedFloor = getSelectedFloor()

        Log.d("UpdateTable", "Updating table ID: $tableId to position ($x, $y)")

        // Firestore에서 해당 테이블의 위치 업데이트
        db.collection("admin")
            .document(userEmail)
            .collection("floors")
            .document(selectedFloor)
            .collection("tables")
            .document(tableId) // 드래그 앤 드롭한 테이블의 ID 사용
            .update("x", x, "y", y)
            .addOnSuccessListener {
                Log.d("Firestore", "Table $tableId position successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating table $tableId position", e)
            }
    }}







