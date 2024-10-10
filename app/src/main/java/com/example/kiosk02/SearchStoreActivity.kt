package com.example.kiosk02

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiosk02.databinding.ActivitySearchStoreBinding
import com.google.android.material.snackbar.Snackbar
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.Tm128
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SearchStoreActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivitySearchStoreBinding
    private lateinit var naverMap: NaverMap
    private var isMapInit = false

    private var restaurantListAdapter = RestaurantListAdapter {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchStoreBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        binding.bottomSheetLayout.searchResultRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
                adapter = restaurantListAdapter
        }

        binding.searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
               return if(query?.isNotEmpty() == true) {
                    SearchRepository.getStore(query).enqueue(object : Callback<SearchResult>{
                        override fun onResponse(call: Call<SearchResult>, response: Response<SearchResult>) {
                            val searchItemList = response.body()?.items.orEmpty()

                            if(searchItemList.isEmpty()) {
                                Snackbar.make(binding.root, "검색결과 없음", Snackbar.LENGTH_SHORT).show()
                                return
                            } else if(isMapInit.not()) {
                                Snackbar.make(binding.root, "오류 발생", Snackbar.LENGTH_SHORT).show()
                                return
                            }

                            searchItemList.forEach {
                                Log.d("SearchStoreActivity", "검색 결과 좌표: mapx=${it.mapx}, mapy=${it.mapy}")
                            }

                           val markers = searchItemList.map {
                               val latLng = LatLng(it.mapy.toDouble() / 10000000, it.mapx.toDouble() / 10000000)
                               Log.d("SearchStoreActivity", "검색 결과 좌표: ${latLng}")
                                Marker(latLng).apply {
                                    captionText = it.title
                                    map = naverMap
                                }
                            }

                            restaurantListAdapter.setData(searchItemList)

                            moveCamera(markers.first().position)

                        }

                        override fun onFailure(call: Call<SearchResult>, t: Throwable) {
                            Snackbar.make(binding.root, "검색 중 오류가 발생했습니다.", Snackbar.LENGTH_SHORT).show()
                        }

                    })
                    false
                } else {
                    return true
                }
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })


    }

    private fun moveCamera(position: LatLng) {
        if(isMapInit.not()) return

        val cameraUpdate = CameraUpdate.scrollTo(position)
            .animate(CameraAnimation.Easing)
        naverMap.moveCamera(cameraUpdate)
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onMapReady(mapObject: NaverMap) {
        naverMap = mapObject
        isMapInit = true


    }
}