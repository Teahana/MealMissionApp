package com.example.meal_mission_app.pages.customer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.meal_mission_app.DTO.ChangePasswordRequest
import com.example.meal_mission_app.DTO.Restaurant
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.auth.LoginActivity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class ProfileActivityCustomer : CustomerBaseActivity() {


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
       OfflineStorageService.clearUserCredentials(this)

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

    private lateinit var textViewName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewPhone: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_basic_info, container, false)
        textViewName = view.findViewById(R.id.textViewName)
        textViewEmail = view.findViewById(R.id.textViewEmail)
        textViewPhone = view.findViewById(R.id.textViewPhone)

        fetchUserDetails()

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUserDetails() {
        val userId: Long = OfflineStorageService.getUserId(requireContext())?.toLong()
            ?: throw IllegalArgumentException("Invalid userId")
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"

        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.getUserDetails(userId, token)
                if (response.isSuccessful) {
                    response.body()?.let { userDetails ->
                        textViewName.text = userDetails.name
                        textViewEmail.text = userDetails.email
                        textViewPhone.text = userDetails.phoneNumber
                    }
                } else {
                    showToast("Failed to fetch user details")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
class ChangePasswordFragment : Fragment() {

    private lateinit var editTextCurrentPassword: EditText
    private lateinit var editTextNewPassword: EditText
    private lateinit var editTextConfirmNewPassword: EditText
    private lateinit var buttonChangePassword: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)
        editTextCurrentPassword = view.findViewById(R.id.editTextCurrentPassword)
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword)
        editTextConfirmNewPassword = view.findViewById(R.id.editTextConfirmNewPassword)
        buttonChangePassword = view.findViewById(R.id.buttonChangePassword)

        buttonChangePassword.setOnClickListener {
            handleChangePassword()
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleChangePassword() {
        val currentPassword = editTextCurrentPassword.text.toString().trim()
        val newPassword = editTextNewPassword.text.toString().trim()
        val confirmNewPassword = editTextConfirmNewPassword.text.toString().trim()

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            showToast("Please fill in all fields")
            return
        }

        if (newPassword != confirmNewPassword) {
            showToast("New passwords do not match")
            return
        }

        if (newPassword.length < 6) {
            showToast("Password must be at least 6 characters")
            return
        }

        // Add any additional password strength validation here

        val userId = OfflineStorageService.getUserId(requireContext())
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"

        val changePasswordRequest = userId?.let {
            ChangePasswordRequest(
                userId = it.toLong(),
                currentPassword = currentPassword,
                newPassword = newPassword
            )
        }

        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.changePassword(changePasswordRequest, token)
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success) {
                            showToast("Password changed successfully")
                            // Optionally, clear the input fields
                            editTextCurrentPassword.text.clear()
                            editTextNewPassword.text.clear()
                            editTextConfirmNewPassword.text.clear()
                        } else {
                            showToast(it.message)
                        }
                    }
                } else {
                    showToast("Failed to change password")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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


// AddressFragment.

class AddressFragment(
    private val isRestaurantContext: Boolean = false  // Differentiates between customer and restaurant
) : Fragment(R.layout.fragment_address) {

    private lateinit var addressListView: ListView
    private lateinit var addressAdapter: UserLocationAdapter
    private var addressList = mutableListOf<UserLocation>()

    private var initialLatitude: Double? = null
    private var initialLongitude: Double? = null
    private var initialCity: String? = null
    private var initialAddress: String? = null

    private lateinit var addAddressButton: Button

    // New fields for restaurant context
    private lateinit var editTextRestaurantName: EditText
    private lateinit var editTextRestaurantDescription: EditText
    private lateinit var buttonSaveRestaurant: Button
    private lateinit var logoutButton: Button

    private lateinit var restaurant: Restaurant
    private val authToken: String by lazy { "Bearer ${OfflineStorageService.getToken(requireContext())}" }
    private val restaurantId: Long by lazy {
        OfflineStorageService.getRestaurantId(requireContext())?.toLongOrNull() ?: 0L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addressListView = view.findViewById(R.id.addressListView)
        addAddressButton = view.findViewById(R.id.addAddressButton)

        val restaurantDetailsLayout = view.findViewById<LinearLayout>(R.id.restaurantDetailsLayout)

        if (isRestaurantContext) {
            // Initialize restaurant fields
            restaurantDetailsLayout.visibility = View.VISIBLE
            addressListView.visibility = View.GONE
            addAddressButton.text = "Edit Location" // Since restaurant has only one location

            editTextRestaurantName = view.findViewById(R.id.editTextRestaurantName)
            editTextRestaurantDescription = view.findViewById(R.id.editTextRestaurantDescription)
            buttonSaveRestaurant = view.findViewById(R.id.buttonSaveRestaurant)
            logoutButton = view.findViewById(R.id.logoutButton)

            buttonSaveRestaurant.setOnClickListener {
                saveRestaurantDetails()
            }
            // Make the logout button visible for the restaurant context
            logoutButton.visibility = View.VISIBLE
            logoutButton.setOnClickListener {
                logoutUser()
            }

            fetchRestaurantDetails()
        } else {
            restaurantDetailsLayout.visibility = View.GONE
            addressListView.visibility = View.VISIBLE
            addAddressButton.text = "Add New Address"

            fetchUserLocations()
        }

        addAddressButton.setOnClickListener {
            val bottomSheet = AddAddressBottomSheet(
                initialLatitude = initialLatitude,
                initialLongitude = initialLongitude,
                initialAddress = initialAddress,
                initialCity = initialCity,
                isRestaurantContext = isRestaurantContext
            )
            bottomSheet.setLocationAddedListener(object : AddAddressBottomSheet.OnLocationAddedListener {
                override fun onLocationAdded() {
                    if (isRestaurantContext) {
                        fetchRestaurantDetails()
                    } else {
                        fetchUserLocations()  // Refresh locations
                    }
                }
            })
            bottomSheet.show(parentFragmentManager, "AddAddressBottomSheet")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchRestaurantDetails() {
        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.getRestaurantDetails(
                    mapOf("restaurantId" to restaurantId.toString()),
                    authToken
                )
                if (response.isSuccessful) {
                    restaurant = response.body()!!
                    populateRestaurantDetails()
                } else {
                    showToast("Failed to fetch restaurant details")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun populateRestaurantDetails() {
        editTextRestaurantName.setText(restaurant.name)
        editTextRestaurantDescription.setText(restaurant.description)

        initialLatitude = restaurant.latitude
        initialLongitude = restaurant.longitude
        initialCity = restaurant.city
        initialAddress = restaurant.address
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveRestaurantDetails() {
        restaurant.name = editTextRestaurantName.text.toString()
        restaurant.description = editTextRestaurantDescription.text.toString()

        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.updateRestaurant(restaurant, authToken)
                if (response.isSuccessful) {
                    showToast("Restaurant details updated successfully")
                } else {
                    showToast("Failed to update restaurant details")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }
    private fun logoutUser() {
        // Clear user data from offline storage
        OfflineStorageService.clearUserCredentials(requireContext())

        // Navigate back to the login activity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()  // Finish current activity
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUserLocations() {
        val userId = OfflineStorageService.getUserId(requireContext())
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"

        val requestData = mapOf(
            "userId" to userId.toString()
        )

        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.getUserLocations(requestData, token)
                if (response.isSuccessful) {
                    response.body()?.let { locations ->
                        addressList.clear()
                        addressList.addAll(locations)
                        renderAddressList()
                    }
                } else {
                    showToast("Failed to fetch addresses")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
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

        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.deleteLocation(requestData, token)
                if (response.isSuccessful) {
                    showToast("Location deleted successfully")
                    fetchUserLocations()  // Refresh list after deletion
                } else {
                    showToast("Failed to delete location")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}




class AddAddressBottomSheet(
    private val initialLatitude: Double? = null,
    private val initialLongitude: Double? = null,
    private val initialAddress: String? = null,
    private val initialCity: String? = null,
    private val isRestaurantContext: Boolean = false // Differentiates between customer and restaurant
) : BottomSheetDialogFragment(), OnMapReadyCallback {

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
    private lateinit var cityEditText: EditText
    private lateinit var locationService: LocationService
    private lateinit var loadingTextView: TextView
    private lateinit var saveAddressButton: Button
    private lateinit var closeButton: ImageButton

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
        cityEditText = view.findViewById(R.id.cityEditText)
        saveAddressButton = view.findViewById(R.id.saveAddressButton)
        closeButton = view.findViewById(R.id.closeButton)
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
            if (isRestaurantContext) {
                saveRestaurantLocation()
            } else {
                saveCustomerLocation()
            }
        }

        locationService = LocationService(requireContext())

        // Show or hide fields based on context
        if (isRestaurantContext) {
            descriptionEditText.visibility = View.GONE
            cityEditText.visibility = View.VISIBLE
            if (initialCity != null) {
                cityEditText.setText(initialCity)
                selectedCity = initialCity
            }
            // Adjust saveAddressButton layout_below to cityEditText
            val params = saveAddressButton.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.BELOW, R.id.cityEditText)
            saveAddressButton.layoutParams = params
        } else {
            descriptionEditText.visibility = View.VISIBLE
            cityEditText.visibility = View.GONE
            // Adjust saveAddressButton layout_below to descriptionEditText
            val params = saveAddressButton.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.BELOW, R.id.descriptionEditText)
            saveAddressButton.layoutParams = params
        }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (initialLatitude != null && initialLongitude != null) {
            val initialLatLng = LatLng(initialLatitude!!, initialLongitude!!)
            placeMarkerOnMap(initialLatLng, initialPlacer = true)
        } else {
            if (checkLocationPermission()) {
                checkLocationSettings()
            } else {
                requestLocationPermission()
            }
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

        if (initialPlacer) {
            if (initialAddress != null) {
                selectedAddress = initialAddress
                addressEditText.setText(initialAddress)
            } else {
                updateAddressAndCity(latLng)
            }

            if (initialCity != null) {
                selectedCity = initialCity
                if (isRestaurantContext) {
                    cityEditText.setText(initialCity)
                }
            } else {
                updateAddressAndCity(latLng)
            }
        } else {
            updateAddressAndCity(latLng)
        }
    }

    private fun updateAddressAndCity(latLng: LatLng) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        lifecycleScope.launch {
            try {
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.filterNotNull().orEmpty()
                }
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    selectedAddress = address.getAddressLine(0)
                    selectedCity = address.locality ?: address.subAdminArea ?: address.adminArea

                    addressEditText.setText(selectedAddress)
                    if (isRestaurantContext) {
                        cityEditText.setText(selectedCity)
                    }
                } else {
                    Toast.makeText(requireContext(), "No address found for this location.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(requireContext(), "Error getting address: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveCustomerLocation() {
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

        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.saveUserLocation(requestData, token)

                if (response.isSuccessful) {
                    locationAddedListener?.onLocationAdded()  // Notify listener
                    Toast.makeText(requireContext(), "Location saved successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to save location.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveRestaurantLocation() {
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"
        val restaurantId = OfflineStorageService.getRestaurantId(requireContext())
        val longitude = selectedLongitude
        val latitude = selectedLatitude
        val address = addressEditText.text.toString().trim()
        val city = cityEditText.text.toString().trim()

        if (longitude == null || latitude == null || address.isEmpty() || city.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a valid location and enter city.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestData = mapOf(
            "restaurantId" to restaurantId.toString(),
            "longitude" to longitude.toString(),
            "latitude" to latitude.toString(),
            "address" to address,
            "city" to city
        )

        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.saveRestaurantLocation(requestData, token)

                if (response.isSuccessful) {
                    locationAddedListener?.onLocationAdded()  // Notify listener
                    Toast.makeText(requireContext(), "Location saved successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to save location.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
        requestPermissions(
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
        // Update the loading message to include the attempt count
        showLoading("Fetching your location... (Attempt ${retryCount + 1} of $MAX_RETRIES)")

        locationService.getCurrentLocation { location ->
            if (location != null) {
                if (location.accuracy <= 30) {
                    // Precise location found, proceed
                    hideLoading()
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    placeMarkerOnMap(userLatLng, true)
                } else {
                    if (retryCount >= MAX_RETRIES - 1) {
                        // After MAX_RETRIES, use the current location regardless of accuracy
                        hideLoading()
                        Toast.makeText(
                            requireContext(),
                            "Couldn't get precise location, using the best available.",
                            Toast.LENGTH_LONG
                        ).show()
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        placeMarkerOnMap(userLatLng, true)
                    } else {
                        // Retry
                        retryCount++
                        handler.postDelayed({ getUserLocation() }, 2000)
                    }
                }
            } else {
                if (retryCount >= MAX_RETRIES - 1) {
                    // No location found after retries
                    hideLoading()
                    Toast.makeText(
                        requireContext(),
                        "Unable to get your location. Please select it on the map.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Retry
                    retryCount++
                    handler.postDelayed({ getUserLocation() }, 2000)
                }
            }
        }
    }



    private fun showLoading(message: String) {
        loadingTextView.text = message
        loadingTextView.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingTextView.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION_PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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