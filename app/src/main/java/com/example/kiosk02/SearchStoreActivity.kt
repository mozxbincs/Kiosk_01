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
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.kiosk02.admin.AdminActivity
import com.example.kiosk02.admin.AdminSignFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var auth: FirebaseAuth
    private lateinit var locationSource: FusedLocationSource
    private var isMapInit = false
    private val markerList = mutableListOf<Marker>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 5000

    private val locationCheckInterval: Long = 300000
    private val handler = Handler(Looper.getMainLooper())

    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var restaurantListAdapter = RestaurantListAdapter { title ->
        checkStoreInFireStore(title)
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

        auth = FirebaseAuth.getInstance()

        if (!hasPermission()) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                LOCATION_PERMISSION_REQUEST_CODE
            )
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

                        var cityOnly =""
                        if (currentLocationName != null && currentLocationName.contains(" ")) {
                            cityOnly = currentLocationName.split(" ")[0]
                        } else if (currentLocationName != null) {
                            cityOnly = currentLocationName
                        }

                        if (currentLocationName != null) {
                            searchQuery = "$cityOnly $query"
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
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }
    private fun checkStoreInFireStore (title : String) {
        Log.d("checkStoreInFireStore", "dd")
        val FireStore = FirebaseFirestore.getInstance()

        FireStore.collection("admin")
            .whereEqualTo("tradeName", title)
            .get()
            .addOnSuccessListener { docuemts ->
                if (!docuemts.isEmpty) {
                    val getEmail = docuemts.first().getString("email") ?: "정보 없음"
                    val userEmail = auth?.currentUser?.email.toString()
                    Log.d("firestore Uemail check", "$userEmail")
                    Log.d("firestore Aemail check", "$getEmail")
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("Aemail", getEmail)
                        putExtra("Uemail", userEmail)
                        putExtra("fragmentToShow", "targetFragment")
                    }
                    startActivity(intent)
                    finish()

                } else {
                    Snackbar.make(binding.root, "등록된 가게가 아닙니다.", Snackbar.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Log.d("firestore 정보 연결 실패", "정보 연결 실패")
            }
    }

    private fun search(searchQuery: String, onCamerMove: (LatLng) -> Unit) {
        clearMarkers()

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
                    Log.d("SearchStoreActivity", "검색 결과 좌표: ${latLng}")
                    Marker(latLng).apply {
                        captionText = it.title
                        map = naverMap
                    }
                }
                markerList.addAll(markers)
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

    private fun clearMarkers() {
        for (marker in markerList) {
            marker.map = null
        }
        markerList.clear()
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
        val apiKey = "AIzaSyC1so0FTzQDWI9x702fGjPdOojwhECSirw"  // 여기에 유효한 Google Maps Geocoding API 키를 입력하세요
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
                    val jsonObject = JSONObject(response)
                    val results = jsonObject.getJSONArray("results")

                    for (i in 0 until results.length()) {
                        val resultItem = results.getJSONObject(i)
                        val addressComponents = resultItem.getJSONArray("address_components")

                        var sublocality = ""
                        var locality = ""

                        for (j in 0 until addressComponents.length()) {
                            val component = addressComponents.getJSONObject(j)
                            val types = component.getJSONArray("types")
                            val longName = component.getString("long_name")

                            when {
                                types.toString().contains("sublocality_level_2") -> sublocality = longName
                                types.toString().contains("locality") -> locality = longName
                            }
                        }

                        if (sublocality.isNotEmpty() && locality.isNotEmpty()) {
                            resultAddress = "$locality $sublocality"
                            break
                        }
                    }

                    if (resultAddress == null) {
                        Log.e("Geocoder Error", "No matching address components found")
                    }
                } else {
                    Log.e("Geocoder Error", "Error response code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("Geocoder Error", "Error retrieving address: ${e.message}")
            }

            return@withContext resultAddress
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

    private fun startLocationUpdates() {
        Log.d("SearchStoreActivity", "위치 업데이트 요청 중...")

        locationSource.activate { location ->
            if (location != null) {
                Log.d("SearchStoreActivity", "위치 업데이트 성공: 위도=${location.latitude}, 경도=${location.longitude}")
                val currentLatLng = LatLng(location.latitude, location.longitude)
                moveCamera(currentLatLng)

                val address = runBlocking { getAddressFormLatLng(location.latitude, location.longitude) }
                Log.d("SearchStoreActivity", "초기 위치 주소: $address")
                if (address != null) {
                    search(address) { resultLatLng ->
                        moveCamera(resultLatLng)
                    }
                } else {
                    Log.d("SearchStoreActivity", "초기 위치 주소를 가져올 수 없습니다.")
                    Toast.makeText(this, "초기 위치 주소를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("SearchStoreActivity", "업데이트된 위치 정보를 가져올 수 없습니다.")
            }
        }
    }

    override fun onMapReady(mapObject: NaverMap) {
        naverMap = mapObject
        isMapInit = true

        naverMap.locationSource = locationSource

        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        if (hasPermission()) {
            Log.d("SearchStoreActivity", "위치 권한 있음, 초기 위치 업데이트 요청")
            startLocationUpdates()  // 위치 업데이트 요청 시작
        } else {
            Log.d("SearchStoreActivity", "위치 권한이 없음, 권한 요청")
            ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE)
        }

        binding.myLocationButton.setOnClickListener {
            val location = locationSource.lastLocation
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val currentLatLng = LatLng(latitude, longitude)

                val address = runBlocking { getAddressFormLatLng(latitude, longitude) }
                if (address != null) {
                    Log.d("SearchStoreActivity", "현위치 주소: $address")
                    search(address) {}
                    moveCamera(currentLatLng)
                }
            } else {
                Log.d("SearchStoreActivity", "위치 정보를 가져올 수 없습니다.")
            }
        }

    }
}
