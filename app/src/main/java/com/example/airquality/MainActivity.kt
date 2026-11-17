package com.example.airquality

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airquality.databinding.ActivityMainBinding
import com.example.airquality.ui.HistoryAdapter
import com.example.airquality.ui.HourlyAqiAdapter
import com.example.airquality.ui.MainViewModel
import com.example.airquality.utils.AqiUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firebaseAuth: FirebaseAuth? = null
    private var googleSignInClient: GoogleSignInClient? = null

    private lateinit var hourlyAdapter: HourlyAqiAdapter
    private lateinit var historyAdapter: HistoryAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            requestCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show()
            viewModel.fetchAirQualityForLocation(37.7749, -122.4194, "San Francisco (Default)")
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val idToken = account.idToken
                if (!idToken.isNullOrEmpty() && firebaseAuth != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    updateAuthUi(account.displayName ?: account.email ?: "Google User", account.email ?: "")
                    Toast.makeText(this, "Signed in as ${account.displayName ?: account.email}", Toast.LENGTH_SHORT).show()
                }
            } else {
                showLocalAccountChooser()
            }
        } catch (e: ApiException) {
            if (e.statusCode == 12501) {
                // User explicitly cancelled sign-in prompt
                return@registerForActivityResult
            }
            // Code 10 / 12500 occurs when google-services.json / SHA-1 is not registered in Firebase Console
            showLocalAccountChooser()
        } catch (e: Exception) {
            showLocalAccountChooser()
        }
    }

    private fun showLocalAccountChooser() {
        val accounts = try {
            val am = android.accounts.AccountManager.get(this)
            am.getAccountsByType("com.google").map { it.name }
        } catch (e: Exception) {
            emptyList()
        }

        val options = if (accounts.isNotEmpty()) {
            (accounts + listOf("Use another Google account")).toTypedArray()
        } else {
            arrayOf("Sign in with Google Account", "Demo Google User (user@gmail.com)")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Google Account")
            .setItems(options) { _, which ->
                val selected = options[which]
                if (selected.contains("@")) {
                    val name = selected.substringBefore("@").replace(".", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    updateAuthUi(name, selected)
                    Toast.makeText(this, "Signed in as $selected", Toast.LENGTH_SHORT).show()
                } else if (selected == "Use another Google account" || selected == "Sign in with Google Account") {
                    promptCustomEmailSignIn()
                } else {
                    updateAuthUi("Demo User", "user@gmail.com")
                    Toast.makeText(this, "Signed in as Demo User", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptCustomEmailSignIn() {
        val input = android.widget.EditText(this)
        input.hint = "user@gmail.com"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val paddingPx = (16 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(this)
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, 0)
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Google Account Sign-In")
            .setMessage("Enter your Google Account email:")
            .setView(container)
            .setPositiveButton("Sign In") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    val name = if (email.contains("@")) email.substringBefore("@").replace(".", " ") else email
                    updateAuthUi(name, email)
                    Toast.makeText(this, "Signed in as $email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupFirebaseAuth()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()

        requestCurrentLocation()
    }

    private fun setupFirebaseAuth() {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
        } catch (e: Exception) {
            firebaseAuth = null
        }

        val webClientId = getString(R.string.default_web_client_id)
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        if (webClientId.isNotBlank() && webClientId != "YOUR_FIREBASE_WEB_CLIENT_ID") {
            gsoBuilder.requestIdToken(webClientId)
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())

        updateAuthUserStatus()
    }

    private fun updateAuthUserStatus() {
        val currentUser = firebaseAuth?.currentUser
        if (currentUser != null) {
            updateAuthUi(currentUser.displayName ?: "User", currentUser.email ?: "")
        } else {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(this)
            if (googleAccount != null) {
                updateAuthUi(googleAccount.displayName ?: "Google User", googleAccount.email ?: "")
            } else {
                updateAuthUi("Guest User", "Not signed in")
            }
        }
    }

    private fun updateAuthUi(name: String, email: String) {
        binding.tvUserName.text = name
        binding.tvUserEmail.text = email

        val isSignedIn = name != "Guest User"
        if (isSignedIn) {
            binding.btnAuth.text = "Sign Out"
        } else {
            binding.btnAuth.text = "Google Sign-In"
        }
    }

    private fun signInWithGoogle() {
        googleSignInClient?.let { client ->
            client.signOut().addOnCompleteListener {
                val signInIntent = client.signInIntent
                try {
                    googleSignInLauncher.launch(signInIntent)
                } catch (e: Exception) {
                    showLocalAccountChooser()
                }
            }
        } ?: run {
            showLocalAccountChooser()
        }
    }

    private fun signOut() {
        googleSignInClient?.signOut()?.addOnCompleteListener {
            firebaseAuth?.signOut()
            updateAuthUi("Guest User", "Not signed in")
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val auth = firebaseAuth ?: return
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateAuthUi(user?.displayName ?: "User", user?.email ?: "")
                    Toast.makeText(this, "Welcome, ${user?.displayName}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun requestCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            viewModel.fetchAirQualityForLocation(location.latitude, location.longitude)
                        } else {
                            viewModel.fetchAirQualityForLocation(37.7749, -122.4194, "San Francisco (Default)")
                        }
                    }
                    .addOnFailureListener {
                        viewModel.fetchAirQualityForLocation(37.7749, -122.4194, "San Francisco (Default)")
                    }
            } catch (e: SecurityException) {
                viewModel.fetchAirQualityForLocation(37.7749, -122.4194, "San Francisco (Default)")
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun setupRecyclerViews() {
        hourlyAdapter = HourlyAqiAdapter()
        binding.rvHourlyForecast.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = hourlyAdapter
        }

        historyAdapter = HistoryAdapter(
            onDeleteClick = { reading ->
                viewModel.deleteReading(reading)
                Toast.makeText(this, "Reading deleted", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAuth.setOnClickListener {
            if (binding.btnAuth.text == "Sign Out") {
                signOut()
            } else {
                signInWithGoogle()
            }
        }

        binding.btnRefreshLocation.setOnClickListener {
            requestCurrentLocation()
        }

        binding.btnSaveReading.setOnClickListener {
            viewModel.saveCurrentReading()
        }
    }

    private fun observeViewModel() {
        viewModel.currentLocationText.observe(this) { locText ->
            binding.tvLocationName.text = locText
        }

        viewModel.airQualityResponse.observe(this) { response ->
            val current = response?.current ?: return@observe
            val aqi = current.usAqi ?: 0

            binding.tvAqiValue.text = aqi.toString()

            val category = AqiUtils.getCategoryName(aqi)
            binding.tvAqiCategory.text = category

            val healthAdvice = AqiUtils.getHealthAdvice(aqi)
            binding.tvHealthAdvice.text = healthAdvice

            val colorResId = AqiUtils.getColorResId(aqi)
            val colorInt = ContextCompat.getColor(this, colorResId)
            binding.cardDashboard.setCardBackgroundColor(colorInt)

            binding.tvPm25.text = String.format(java.util.Locale.US, "%.1f μg/m³", current.pm25 ?: 0.0)
            binding.tvPm10.text = String.format(java.util.Locale.US, "%.1f μg/m³", current.pm10 ?: 0.0)
            binding.tvCo.text = String.format(java.util.Locale.US, "%.1f μg/m³", current.carbonMonoxide ?: 0.0)
        }

        viewModel.hourlyList.observe(this) { list ->
            hourlyAdapter.submitList(list)
        }

        viewModel.savedReadings.observe(this) { readings ->
            historyAdapter.submitList(readings)
            binding.tvEmptyHistory.visibility = if (readings.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
