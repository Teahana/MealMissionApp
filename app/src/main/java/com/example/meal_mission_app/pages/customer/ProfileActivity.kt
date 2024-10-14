package com.example.meal_mission_app.pages.customer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.recreate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.BaseActivity
import com.example.meal_mission_app.pages.LoginActivity
import com.example.meal_mission_app.services.LocationService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class ProfileActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_profile, findViewById(R.id.activity_content))

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val logoutButton: Button = findViewById(R.id.logoutButton)

        viewPager.adapter = ProfilePagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Basic Info"
                1 -> "Change Password"
                2 -> "Address"
                else -> "Basic Info"
            }
        }.attach()

        // Set up logout button click listener
        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        // Clear user data from offline storage
       // OfflineStorageService.clearUserCredentials(this)

        // Navigate back to the login activity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()  // Finish current activity
    }
    override fun getSelectedItemId(): Int {
        return R.id.nav_profile  // Ensure the "Profile" icon is highlighted in the bottom nav
    }
}
class ProfilePagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BasicInfoFragment()
            1 -> ChangePasswordFragment()
            2 -> AddressFragment()
            else -> BasicInfoFragment()
        }
    }
}

class BasicInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_basic_info, container, false)
    }
}
class ChangePasswordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }
}
class UserLocationAdapter(
    context: Context,
    private val locations: List<UserLocation>,
    private val onDeleteClick: (UserLocation) -> Unit
) : ArrayAdapter<UserLocation>(context, 0, locations) {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.address_list_item, parent, false)

        val location = getItem(position)

        val addressTextView: TextView = view.findViewById(R.id.addressTextView)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)

        // Update the TextView with the address and city
        addressTextView.text = "${location?.address}, ${location?.city}"

        // Set up the delete button
        deleteButton.setOnClickListener {
            location?.let { onDeleteClick(it) }
        }

        return view
    }
}


class AddressFragment : Fragment() {

    private lateinit var addressListView: ListView
    private lateinit var addressAdapter: UserLocationAdapter
    private var addressList = mutableListOf<UserLocation>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_address, container, false)

        addressListView = view.findViewById(R.id.addressListView)
        val addAddressButton: Button = view.findViewById(R.id.addAddressButton)

        addAddressButton.setOnClickListener {
            val bottomSheet = AddAddressBottomSheet()
            bottomSheet.setLocationAddedListener(object : AddAddressBottomSheet.OnLocationAddedListener {
                override fun onLocationAdded() {
                    fetchUserLocations()  // Refresh locations
                }
            })
            bottomSheet.show(parentFragmentManager, "AddAddressBottomSheet")
        }

        fetchUserLocations()
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUserLocations() {
        val userId = OfflineStorageService.getUserId(requireContext())
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"

        val requestData = mapOf(
            "userId" to userId.toString()
        )

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.apiService.getUserLocations(requestData, token)
                if (response.isSuccessful) {
                    response.body()?.let { locations ->
                        addressList.clear()
                        addressList.addAll(locations)
                        withContext(Dispatchers.Main) {
                            renderAddressList()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to fetch addresses.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderAddressList() {
        addressAdapter = UserLocationAdapter(requireContext(), addressList) { location ->
            deleteUserLocation(location.id)
        }
        addressListView.adapter = addressAdapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteUserLocation(locationId: Long) {
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"

        val requestData = mapOf(
            "id" to locationId.toString()
        )

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.apiService.deleteLocation(requestData, token)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Location deleted successfully!", Toast.LENGTH_SHORT).show()
                        fetchUserLocations()  // Refresh list after deletion
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete location.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


class AddAddressBottomSheet : BottomSheetDialogFragment(), OnMapReadyCallback {
    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS_CODE = 1001
        private const val REQUEST_CHECK_SETTINGS = 1002
        private const val MAX_RETRIES = 5
    }
    interface OnLocationAddedListener {
        fun onLocationAdded()
    }

    private var locationAddedListener: OnLocationAddedListener? = null

    fun setLocationAddedListener(listener: OnLocationAddedListener) {
        this.locationAddedListener = listener
    }

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var addressEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var locationService: LocationService
    private lateinit var loadingTextView: TextView

    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress: String? = null
    private var selectedCity: String? = null
    private var bestLocation: Location? = null

    private var currentMarker: Marker? = null
    private var handler = Handler(Looper.getMainLooper())
    private var retryCount = 0

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_add_address, container, false)

        mapView = view.findViewById(R.id.mapContainer)
        addressEditText = view.findViewById(R.id.addressEditText)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        val saveAddressButton: Button = view.findViewById(R.id.saveAddressButton)
        val closeButton: ImageButton = view.findViewById(R.id.closeButton)
        loadingTextView = view.findViewById(R.id.loadingTextView)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Disable swiping down on the map from closing the bottom sheet
        mapView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    v.performClick() // Ensure performClick is called for accessibility
                }
            }
            false
        }

        // Disable swipe-to-dismiss for the bottom sheet
        view.viewTreeObserver.addOnGlobalLayoutListener {
            val behavior = BottomSheetBehavior.from(view.parent as View)
            behavior.isDraggable = false // Disable dragging/swiping down to dismiss
        }

        closeButton.setOnClickListener {
            dismiss()
        }

        saveAddressButton.setOnClickListener {
            saveUserLocation()
        }

        locationService = LocationService(requireContext())

        return view
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (checkLocationPermission()) {
            checkLocationSettings()
        } else {
            requestLocationPermission()
        }

        googleMap.setOnMapClickListener { latLng ->
            placeMarkerOnMap(latLng, initialPlacer = false)
        }
    }

    private fun placeMarkerOnMap(latLng: LatLng, initialPlacer: Boolean) {
        currentMarker?.remove()
        currentMarker = googleMap.addMarker(
            MarkerOptions().position(latLng).title("Selected Location")
        )

        if (initialPlacer) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }

        selectedLatitude = latLng.latitude
        selectedLongitude = latLng.longitude
        updateAddressAndCity(latLng)
    }

    private fun updateAddressAndCity(latLng: LatLng) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.filterNotNull().orEmpty()
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    selectedAddress = address.getAddressLine(0)
                    selectedCity = address.locality

                    launch(Dispatchers.Main) {
                        addressEditText.setText(selectedAddress)
                    }
                } else {
                    launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No address found for this location.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error getting address: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveUserLocation() {
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"
        val userId = OfflineStorageService.getUserId(requireContext())
        val longitude = selectedLongitude
        val latitude = selectedLatitude
        val address = addressEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val city = selectedCity

        if (longitude == null || latitude == null || address.isEmpty() || city == null) {
            Toast.makeText(requireContext(), "Please select a valid location.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestData = mapOf(
            "userId" to userId.toString(),
            "longitude" to longitude.toString(),
            "latitude" to latitude.toString(),
            "address" to address,
            "description" to description,
            "city" to city
        )

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.apiService.saveUserLocation(requestData, token)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        locationAddedListener?.onLocationAdded()  // Notify listener
                        Toast.makeText(requireContext(), "Location saved successfully!", Toast.LENGTH_SHORT).show()
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save location.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION_PERMISSIONS_CODE
        )
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            println("sucess")
            getUserLocation() // Proceed to fetch the user location if settings are enabled
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(requireContext(), "Unable to resolve location settings.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enable location services to continue.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun getUserLocation() {
        showLoading("Fetching precise location...")

        fetchCurrentLocation { location ->
            if (location != null) {
                if (location.accuracy <= 30) {
                    bestLocation = location
                    hideLoading()
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    placeMarkerOnMap(userLatLng, true)
                } else {
                    if (bestLocation == null || location.accuracy < bestLocation?.accuracy ?: Float.MAX_VALUE) {
                        bestLocation = location
                    }
                    if (retryCount < MAX_RETRIES) {
                        retryCount++
                        handler.postDelayed({ getUserLocation() }, 2000)
                    } else {
                        hideLoading()
                        bestLocation?.let {
                            Toast.makeText(requireContext(), "Unable to find accurate location, using close approximation.", Toast.LENGTH_LONG).show()
                            val userLatLng = LatLng(it.latitude, it.longitude)
                            placeMarkerOnMap(userLatLng, true)
                        } ?: run {
                            Toast.makeText(requireContext(), "Unable to find location. Please select your location on the map.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    handler.postDelayed({ getUserLocation() }, 2000)
                } else {
                    hideLoading()
                    bestLocation?.let {
                        Toast.makeText(requireContext(), "Unable to find accurate location, using close approximation.", Toast.LENGTH_LONG).show()
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        placeMarkerOnMap(userLatLng, true)
                    } ?: run {
                        Toast.makeText(requireContext(), "Unable to find location. Please select your location on the map.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun showLoading(message: String) {
        loadingTextView.text = message
        loadingTextView.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingTextView.visibility = View.GONE
    }
    private fun fetchCurrentLocation(callback: (Location?) -> Unit) {
        locationService.getCurrentLocation { location ->
            callback(location)
        }
    }

    private fun updateMapWithLocation(userLatLng: LatLng) {
        googleMap.clear() // Clear previous markers if any
        googleMap.addMarker(
            MarkerOptions()
                .position(userLatLng)
                .title("Your Location")
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                getUserLocation()
            } else {
                Toast.makeText(requireContext(), "GPS is required to add an address.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

// UserLocation data class without the User field
data class UserLocation(
    val id: Long,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?
)

// Response data class for saving user location
data class SaveUserLocationResponse(
    val userLocation: UserLocation,
    val message: String
)