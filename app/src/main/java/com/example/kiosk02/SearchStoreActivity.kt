package com.example.kiosk02

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiosk02.databinding.ActivitySearchStoreBinding
import com.google.android.material.snackbar.Snackbar
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.ln
import kotlin.math.log


class SearchStoreActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivitySearchStoreBinding
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private var isMapInit = false
    private val LOCATION_PERMISSION_REQUEST_CODE = 5000

    private val locationCheckInterval: Long = 300000
    private val handler = Handler(Looper.getMainLooper())

    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

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

        if (!hasPermission()) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                LOCATION_PERMISSION_REQUEST_CODE
            ) // 권한이 있는 경우 위치 업데이트 시작
        } else {
            initMapView()
        }

        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        Log.d("locationSource: $locationSource", "")

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        handler.post(checkLocationTask)

        binding.bottomSheetLayout.searchResultRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = restaurantListAdapter
        }

        binding.searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return if (query?.isNotEmpty() == true) {

                    var searchQuery = query
                    val location = locationSource.lastLocation
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        val currentLocationName = runBlocking {getAddressFormLatLng(latitude, longitude)}

                        if (currentLocationName != null) {
                            searchQuery = "$currentLocationName $query"
                        }
                    }

                    Log.d("searchQuery", "$searchQuery")

                    search(searchQuery) { resultLatLng ->
                        if (resultLatLng != null) {
                            moveCamera(resultLatLng)
                        }
                    }
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

    private fun search(searchQuery: String, onCamerMove: (LatLng) -> Unit) {
        SearchRepository.getStore(searchQuery).enqueue(object : Callback<SearchResult> {
            override fun onResponse(
                call: Call<SearchResult>,
                response: Response<SearchResult>
            ) {
                val searchItemList = response.body()?.items.orEmpty()

                if (searchItemList.isEmpty()) {
                    Snackbar.make(binding.root, "검색결과 없음", Snackbar.LENGTH_SHORT).show()
                    return
                } else if (isMapInit.not()) {
                    Snackbar.make(binding.root, "오류 발생", Snackbar.LENGTH_SHORT).show()
                    return
                }

                searchItemList.forEach {
                    /*Log.d(
                        "Search",
                        "검색 결과 좌표: mapx=${it.mapx}, mapy=${it.mapy}"
                    )*/
                }

                val markers = searchItemList.map {
                    val latLng = LatLng(
                        it.mapy.toDouble() / 10000000,
                        it.mapx.toDouble() / 10000000
                    )
                    //Log.d("SearchStoreActivity", "검색 결과 좌표: ${latLng}")
                    Marker(latLng).apply {
                        captionText = it.title
                        map = naverMap
                    }
                }

                restaurantListAdapter.setData(searchItemList)

                onCamerMove(markers.first().position)
                //moveCamera(markers.first().position)


            }

            override fun onFailure(call: Call<SearchResult>, t: Throwable) {
                Snackbar.make(binding.root, "검색 중 오류가 발생했습니다.", Snackbar.LENGTH_SHORT)
                    .show()
            }

        })
    }

    private fun initMapView() {
        val fm = supportFragmentManager
        val mapFragment =
            fm.findFragmentById(R.id.map_view) as MapFragment? ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_view, it).commit()
            }
        mapFragment.getMapAsync(this)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun hasPermission(): Boolean {
        for (permission in PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private val checkLocationTask = object : Runnable {
        override fun run() {
            checkCurrentLocation()
            handler.postDelayed(this, locationCheckInterval)
        }
    }

    private fun checkCurrentLocation() {
        val location = locationSource.lastLocation
        if (location != null) {
            val latitude = location.latitude
            val longitude = location.longitude
            Log.d("SearchStoreActivity 위경도", "현재 위치: 위도=$latitude, 경도=$longitude")

            val address = runBlocking { getAddressFormLatLng(latitude, longitude) }
            Log.d("SearchStoreActivity 현주소", "현재 주소: $address")
            if (address != null) {
                Log.d("SearchStoreActivity 현주소", "현재 주소: $address")
            }
        } else {
            Log.d("SearchStoreActivity", "위치 정보를 가져올 수 없습니다.")
        }
    }

    suspend fun getAddressFormLatLng(lat: Double, lng: Double): String? {
        val apiKey = "AIzaSyB77AQD-C0eNPC8YEVqOrGU9y3L5BFiPUw"  // 여기에 유효한 Google Maps Geocoding API 키를 입력하세요
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&key=$apiKey&language=ko"

        return withContext(Dispatchers.IO) {
            var resultAddress: String? = null

            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    //Log.d("Response", "응답 코드: $response")

                    val jsonObject = JSONObject(response)
                    val results = jsonObject.getJSONArray("results")
                    //Log.d("Results", "Results array: $results")

                    for (i in 0 until results.length()) {
                        val resultItem = results.getJSONObject(i)
                        val typesArray = resultItem.getJSONArray("types")

                        if (typesArray.length() > 0 && typesArray.getString(0) == "street_address") {
                            val formattedAddress = resultItem.getString("formatted_address")
                            val addressParts = formattedAddress.split(" ")
                            val roadName = addressParts.getOrNull(3) ?: ""
                            val cityName = addressParts.getOrNull(2) ?: ""

                            resultAddress = "$cityName $roadName"
                            //Log.d("Parsed Address", "Extracted address: $resultAddress")
                            break
                        }
                    }

                    if (resultAddress == null) {
                        Log.e("Geocoder Error", "No 'street_address' type found in results")
                    }
                } else {
                    Log.e("Geocoder Error", "Error response code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("Geocoder Error", "Error retrieving address: ${e.message}")
            }

            return@withContext resultAddress

            /*
        val geocoder = Geocoder(this, Locale.getDefault())
        val address = geocoder.getFromLocation(lat, lng, 1)
        if (!address.isNullOrEmpty()) {
            val fullAddress = address[0].getAddressLine(0)
            Log.d("Full Address", fullAddress)

            val addressParts = fullAddress.split(" ")
            val city = addressParts.getOrNull(2)
            val roadAdress = addressParts.getOrNull(3)
            Log.d("Full Address", "$city $roadAdress")
            return "$city $roadAdress"
        }
        return null
        */
        }

    }

    private fun moveCamera(position: LatLng) {
        if (isMapInit.not()) return

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

        naverMap.locationSource = locationSource
        naverMap.uiSettings.isLocationButtonEnabled = true

        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        val location = locationSource.lastLocation
        if (location != null) {
            val latitude = location.latitude
            val longitude = location.longitude

            moveCamera(LatLng(latitude, longitude))
            val address = runBlocking { getAddressFormLatLng(latitude, longitude) }
            if (address != null) {
                search(address) {}
            }
        }

        naverMap.addOnLocationChangeListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d("SearchStoreActivity", "현위치: 위도=$latitude, 경도=$longitude")

                val address = runBlocking { getAddressFormLatLng(latitude, longitude) }
                if (address != null) {
                    Log.d("SearchStoreActivity", "현위치 주소: $address")
                    search(address) {}
                }
            } else {
                Log.d("SearchStoreActivity", "위치 정보를 가져올 수 없습니다.")
            }
        }


    }
}