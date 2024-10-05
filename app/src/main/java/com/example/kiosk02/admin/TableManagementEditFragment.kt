package com.example.kiosk02.admin

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import com.example.kiosk02.R

class TableManagementEditFragment : Fragment(R.layout.activity_table_management_edit) {
    private lateinit var tableList: LinearLayout
    private lateinit var tableGrid: GridLayout
    private lateinit var oneSeaterCountEditText: EditText
    private var addedTables: MutableList<View> = mutableListOf() // 추가된 테이블 목록

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tableList = view.findViewById(R.id.table_list)
        tableGrid = view.findViewById(R.id.table_grid)
        oneSeaterCountEditText = view.findViewById(R.id.one_seater_count)

        // 수량 제한: 0부터 4까지
        oneSeaterCountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val count = oneSeaterCountEditText.text.toString().toIntOrNull() ?: 0
                if (count > 4) {
                    oneSeaterCountEditText.setText("4") // 최대 4로 설정
                    Toast.makeText(requireContext(), "최대 4개 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 1인 테이블 추가 버튼 클릭 이벤트
        view.findViewById<Button>(R.id.add_one_seater_button).setOnClickListener {
            val count = oneSeaterCountEditText.text.toString().toIntOrNull() ?: 0
            if (addedTables.size + count > 6) {
                Toast.makeText(requireContext(), "전체 테이블 수는 최대 6개입니다.", Toast.LENGTH_SHORT).show() // 6개 초과 경고
            } else if (count > 4) {
                Toast.makeText(requireContext(), "최대 4개까지 추가 가능합니다.", Toast.LENGTH_SHORT).show() // 한 번에 4개 초과 경고
            } else {
                addTablesToList(count, "1인")
            }
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

    @SuppressLint("MissingInflatedId")
    private fun addTablesToList(count: Int, tableType: String) {
        for (i in 1..count) {
            val tableView = LayoutInflater.from(requireContext()).inflate(R.layout.table_item, null)
            (tableView.findViewById<TextView>(R.id.table_name)).text = tableType

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

            // 테이블의 레이아웃 파라미터 설정
            val size = (50 * resources.displayMetrics.density).toInt() // 고정 크기로 설정 (100dp x 100dp)
            tableView.layoutParams = LinearLayout.LayoutParams(size, size)

            val margin = 8 // 원하는 마진 값
            val params = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }

            // 테이블 목록에 추가
            tableList.addView(tableView, params) // 추가할 때 레이아웃 파라미터 전달

            addedTables.add(tableView) // 추가된 테이블 목록에 추가
        }
    }

    // 드래그 리스너 설정 (드롭을 처리하는 부분)
    private val dragListener = View.OnDragListener { v, event ->
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
                val draggedView = event.localState as View
                val owner = draggedView.parent as ViewGroup
                owner.removeView(draggedView)

                // 드롭한 뷰를 GridLayout에 추가
                val destination = v as GridLayout

                // GridLayout에 뷰를 추가하면서 LayoutParams를 사용해 위치 지정
                val params = GridLayout.LayoutParams()
                params.width = GridLayout.LayoutParams.WRAP_CONTENT
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) // 컬럼 위치
                params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) // 행 위치

                destination.addView(draggedView, params)

                // 드래그된 뷰가 다시 보이도록 설정
                draggedView.visibility = View.VISIBLE
                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                v.invalidate()
                true
            }

            else -> false
        }
    }
}
