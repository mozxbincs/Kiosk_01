package com.example.kiosk02.admin

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.text.InputType
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.kiosk02.R

class TableManagementEditFragment : Fragment(R.layout.activity_table_management_edit) {
    private lateinit var tableList: LinearLayout
    private lateinit var tableFrame: FrameLayout
    private lateinit var removeTableButton: Button
    private var addedTables: MutableList<View> = mutableListOf() // 추가된 테이블 목록
    private var droppedTables: MutableList<View> = mutableListOf() // 드롭된 테이블 목록
    private var maxTablesInList = 6 // tableList에 추가될 수 있는 최대 테이블 수
    private var selectedTable: View? = null // 선택된 테이블

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableList = view.findViewById(R.id.table_list)
        tableFrame = view.findViewById(R.id.table_frame)
        removeTableButton = view.findViewById(R.id.remove_table_button)

        removeTableButton.visibility = View.GONE

        // 드롭할 수 있는 FrameLayout에 드롭 리스너 추가
        tableFrame.setOnDragListener(dragListener)

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

        // 삭제 버튼 클릭 이벤트
        view.findViewById<Button>(R.id.delete_table_button).setOnClickListener {
            if (addedTables.isEmpty()) {
                Toast.makeText(requireContext(), "삭제할 테이블이 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 마지막으로 생성된 테이블 삭제
                val tableToRemove = addedTables.last()
                tableList.removeView(tableToRemove)
                addedTables.removeAt(addedTables.size - 1) // 목록에서도 제거
                Toast.makeText(requireContext(), "테이블이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
     }
//        view.findViewById<Button>(R.id.remove_table_button).setOnClickListener {
//
//
//        }


        // 드롭할 수 있는 FrameLayout에 드롭 리스너 추가
        tableFrame.setOnDragListener(dragListener)
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

//    // FrameLayout에서 선택된 테이블을 제거하는 함수
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
            Toast.makeText(requireContext(), "이 공간에 추가될 수 있는 테이블은 최대 $maxTablesInList 개입니다.", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun addTablesToList(count: Int, tableType: String) {
        for (i in 1..count) {
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
                    val dragData = ClipData.newPlainText("table", tableType)
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
    private val dragListener = View.OnDragListener { v, event ->
        val draggedView = event.localState as? View // 드래그된 뷰 가져오기
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                true
            }
            DragEvent.ACTION_DROP -> {
                draggedView?.let { view ->
                    // 뷰를 소유자에서 제거
                    val owner = view.parent as? ViewGroup
                    owner?.removeView(view)

                    // FrameLayout에 뷰 추가
                    val destination = v as FrameLayout

                    // 드롭 시 고정 위치 설정
                    val layoutParams = FrameLayout.LayoutParams(view.layoutParams.width, view.layoutParams.height)
                    layoutParams.leftMargin = (event.x - (view.layoutParams.width / 2)).toInt()
                    layoutParams.topMargin = (event.y - (view.layoutParams.height / 2)).toInt()
                    destination.addView(view, layoutParams)

                    // 드롭된 테이블 목록에 추가
                    droppedTables.add(view)
                    //테이블 클릭 리스너 추가
                    view.setOnClickListener {
                        selectedTable = view
                        removeTableButton.visibility = View.VISIBLE // 삭제 버튼 보이기
                }}
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                if (event.result) {
                    Toast.makeText(requireContext(), "드롭 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "드롭 실패", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> false
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
