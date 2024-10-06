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
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.kiosk02.R

class TableManagementEditFragment : Fragment(R.layout.activity_table_management_edit) {
    private lateinit var tableList: LinearLayout
    private lateinit var tableGrid: GridLayout


    private var addedTables: MutableList<View> = mutableListOf() // 추가된 테이블 목록

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableList = view.findViewById(R.id.table_list)
        tableGrid = view.findViewById(R.id.table_grid)


        // 기타 테이블 관련 UI 요소 초기화





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
                // 마지막으로 추가된 테이블 삭제
                val tableToRemove = addedTables.last()
                tableList.removeView(tableToRemove)
                addedTables.removeAt(addedTables.size - 1) // 목록에서도 제거
                Toast.makeText(requireContext(), "테이블이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 드롭할 수 있는 그리드 레이아웃에 드롭 리스너 추가
        tableGrid.setOnDragListener(dragListener)
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
            if (addedTables.size + count > 6) {
                Toast.makeText(requireContext(), "전체 테이블 수는 최대 6개입니다.", Toast.LENGTH_SHORT).show()
            } else if (count > 4) {
                Toast.makeText(requireContext(), "최대 4개까지 추가 가능합니다.", Toast.LENGTH_SHORT).show()
            } else {
                addTablesToList(count, seaterType) // 테이블 추가
            }
        }

        builder.setNegativeButton("취소") { dialog, which -> dialog.cancel() }

        builder.show()
    }


    // 수량 입력 다이얼로그를 표시하는 함수
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

            if (seaterCount in 5..50 && quantityCount > 0) {
                addTablesToList(quantityCount, seaterCount.toString())
            } else {
                Toast.makeText(requireContext(), "올바른 숫자를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 취소 버튼
        builder.setNegativeButton("취소") { dialog, which -> dialog.cancel() }

        // 다이얼로그 표시
        builder.show()
    }


    @SuppressLint("MissingInflatedId")
    private fun addTablesToList(count: Int, tableType: String) {
        for (i in 1..count) {
            val tableView = LayoutInflater.from(requireContext()).inflate(R.layout.table_item, null)
            val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)

            // 테이블 이름 설정
            tableNameTextView.text = tableType+"인"

            // 테이블 크기 및 스타일 설정 (1인, 2인, 3인, 4인 등)
//            val size = when (tableType) {
//                "1" -> (50 * resources.displayMetrics.density).toInt() // 1인 테이블 크기
//                "2" -> (60 * resources.displayMetrics.density).toInt() // 2인 테이블 크기
//                "3" -> (70 * resources.displayMetrics.density).toInt() // 3인 테이블 크기
//                "4" -> (80 * resources.displayMetrics.density).toInt() // 4인 테이블 크기
//                else -> (90 * resources.displayMetrics.density).toInt() // 기타 테이블 크기
//            }
            val size=(50*resources.displayMetrics.density).toInt()

            // 테이블의 드래그 앤 드롭 기능 추가
            tableView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val dragData = ClipData.newPlainText("table", tableType)
                    val dragShadowBuilder = View.DragShadowBuilder(v)
                    v.startDragAndDrop(dragData, dragShadowBuilder, v, 0)
                    v.performClick()  // Accessibility를 위해 클릭 처리
                    true
                } else {
                    false
                }
            }

            // 레이아웃 파라미터 및 마진 설정
            val margin = 8 // 원하는 마진 값
            val params = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }

            // 테이블 목록에 추가
            tableList.addView(tableView, params)
            addedTables.add(tableView) // 추가된 테이블 목록에 저장
        }
    }


    private fun addTables(seaterCount: Int, tableCount: Int) {
        for (i in 1..tableCount) {
            addTablesToList(seaterCount, "기타")
        }
    }


    // 드래그 리스너 설정 (드롭을 처리하는 부분)
    private val dragListener = View.OnDragListener { v, event ->
        val draggedView = event.localState as? View // 드래그된 뷰 가져오기
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                v.invalidate()
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> true
            DragEvent.ACTION_DRAG_EXITED -> {
                v.invalidate()
                true
            }
            DragEvent.ACTION_DROP -> {
                draggedView?.let { view ->
                    val owner = view.parent as ViewGroup
                    owner.removeView(view)

                    // 드롭한 뷰를 GridLayout에 추가
                    val destination = v as GridLayout

                    // GridLayout에 뷰를 추가하면서 LayoutParams를 사용해 위치 지정
                    val params = GridLayout.LayoutParams()
                    params.width = GridLayout.LayoutParams.WRAP_CONTENT
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) // 컬럼 위치
                    params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) // 로우 위치
                    destination.addView(view, params)
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                // 드래그가 종료된 경우, 드래그가 성공적이었는지 확인
                if (event.result) {
                    // 드래그가 성공적으로 종료된 경우
                    draggedView?.visibility = View.VISIBLE
                } else {
                    // 드래그가 실패한 경우, 원래 위치로 복원
                    draggedView?.visibility = View.VISIBLE
                }
                true
            }
            else -> false
        }
    }

}
