package com.example.farmapp.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.farmapp.R
import com.example.farmapp.data.model.Resource
import com.example.farmapp.data.repository.MainRepository
import com.example.farmapp.ui.viewmodel.MainViewModel
import com.example.farmapp.ui.viewmodel.MainViewModelFactory
import com.example.farmapp.utils.Config

import com.example.farmapp.utils.TranslationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvLocation: TextView
    private lateinit var tvWeatherToday: TextView
    private lateinit var tvWeatherTomorrow: TextView
    private lateinit var tvSoilData: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvLocationTR: TextView
    private lateinit var diseaseBtn: View
    private lateinit var predictionBtn: View
    private lateinit var communityBtn: View
    private lateinit var logoutBtn: View

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(MainRepository())
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        var selectedLanguage: String = "en"
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        TranslationHelper.initDictionary(this)

        tvLocation = findViewById(R.id.tvLocation)
        tvWeatherToday = findViewById(R.id.tvWeatherToday)
        tvWeatherTomorrow = findViewById(R.id.tvWeatherTomorrow)
        tvSoilData = findViewById(R.id.tvSoildata)
        tvTemp = findViewById(R.id.tvTemp)
        tvLocationTR = findViewById(R.id.tvCurrentLocation)
        diseaseBtn = findViewById(R.id.start2)
        predictionBtn = findViewById(R.id.start1)
        communityBtn = findViewById(R.id.start3)
        logoutBtn = findViewById(R.id.btnLogout)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val prefs = getSharedPreferences("farmapp_prefs", Context.MODE_PRIVATE)
        val langSelected = prefs.getString("selected_lang", "en")
        selectedLanguage = langSelected ?: "en"

        // --- Spinner setup ---
        val spinner: Spinner = findViewById(R.id.spinnerLanguage)
        val languages = mapOf(
            "English" to "en",
            "Hindi" to "hi"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages.keys.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Pre-select current language
        val langIndex = languages.values.indexOf(selectedLanguage)
        if (langIndex >= 0) spinner.setSelection(langIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val langCode = languages.values.toList()[position]
                if (langCode != selectedLanguage) {
                    selectedLanguage = langCode
                    prefs.edit().putString("selected_lang", langCode).apply()
                    LanguageHelper.setLocale(this@MainActivity, langCode)
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        observeWeather()
        observeUnifiedSoil()
        fetchLocationAndData()

        communityBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_OPEN_COMMUNITY, true)
            }
            startActivity(intent)
        }

        predictionBtn.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, Class.forName("com.example.farmapp.ui.view.CropInputActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Crop Prediction feature is coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        diseaseBtn.setOnClickListener {
            try {
                val intent = Intent(this@MainActivity, Class.forName("com.example.farmapp.ui.view.DiseaseDetectionActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Disease Detection feature is coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            getSharedPreferences("farmapp_prefs", Context.MODE_PRIVATE).edit()
                .remove("username")
                .remove("user_name")
                .apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        updateWelcomeMessage()
    }

    private fun updateWelcomeMessage() {
        val tvWelcome: TextView = findViewById(R.id.tvWelcome) ?: return
        val username = getSharedPreferences("farmapp_prefs", Context.MODE_PRIVATE).getString("username", null)
            ?: getSharedPreferences("farmapp_prefs", Context.MODE_PRIVATE).getString("user_name", "Farmer")
        
        tvWelcome.text = "Welcome, $username!"
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("farmapp_prefs", Context.MODE_PRIVATE)
        val langSelected = prefs.getString("selected_lang", "en") ?: "en"
        val context = LanguageHelper.setLocale(newBase, langSelected)
        super.attachBaseContext(context)
    }

    private fun observeWeather() {
        viewModel.weather.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    tvWeatherToday.text = "Loading weather..."
                    tvWeatherTomorrow.text = ""
                }
                is Resource.Success -> {
                    val weather = resource.data
                    tvLocationTR.text = weather?.location?.name ?: "NA"

                    val todayCondition = weather?.forecast?.forecastday?.get(0)?.day?.condition?.text ?: ""
                    val tomorrowCondition = if ((weather?.forecast?.forecastday?.size ?: 0) > 1) {
                        weather?.forecast?.forecastday?.get(1)?.day?.condition?.text ?: ""
                    } else ""
                    
                    val loc = weather?.location?.name ?: ""
                    
                    val targetLang = if (selectedLanguage == "hi") TranslateLanguage.HINDI else null

                    if (targetLang != null && targetLang != TranslateLanguage.ENGLISH) {
                        TranslationHelper.initTranslator(TranslateLanguage.ENGLISH, targetLang, this) {
                            TranslationHelper.translate(todayCondition) { translated -> tvWeatherToday.text = translated }
                            TranslationHelper.translate(loc) { translated -> tvLocation.text = translated }
                            TranslationHelper.translate(tomorrowCondition) { translated -> tvWeatherTomorrow.text = translated }
                        }
                    } else {
                        tvWeatherToday.text = todayCondition
                        tvWeatherTomorrow.text = tomorrowCondition
                        tvLocation.text = loc
                    }

                    tvTemp.text = "${weather?.forecast?.forecastday?.get(0)?.day?.avgtemp_c} C"
                }
                is Resource.Error -> {
                    tvWeatherToday.text = "Offline"
                    Toast.makeText(this, "Weather fetch error: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeUnifiedSoil() {
        viewModel.unifiedSoil.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> tvSoilData.text = "Scanning soil properties..."
                is Resource.Success -> {
                    val data = resource.data
                    val moisture = data?.moisture?.let { String.format("%.1f%%", it) } ?: "--"
                    val ph = data?.ph?.let { String.format("%.1f", it) } ?: "--"
                    val nitrogen = data?.nitrogen?.let { String.format("%.2f g/kg", it) } ?: "--"
                    
                    val rawText = "Moisture: $moisture | pH: $ph | N: $nitrogen"
                    
                    val targetLang = if (selectedLanguage == "hi") TranslateLanguage.HINDI else null
                    if (targetLang != null && targetLang != TranslateLanguage.ENGLISH) {
                        TranslationHelper.initTranslator(TranslateLanguage.ENGLISH, targetLang, this) {
                            TranslationHelper.translate(rawText) { translated -> tvSoilData.text = translated }
                        }
                    } else {
                        tvSoilData.text = rawText
                    }
                }
                is Resource.Error -> {
                    tvSoilData.text = "Soil data unavailable"
                }
            }
        }
    }

    private fun fetchLocationAndData() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        tvLocationTR.text = "Fetching location..."
        tvLocation.text = "Fetching location..."

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    useLocation(location)
                } else {
                    fetchFreshLocation()
                }
            }
            .addOnFailureListener {
                fetchFreshLocation()
            }
    }

    @SuppressLint("MissingPermission")
    private fun fetchFreshLocation() {
        if (!hasLocationPermission()) return

        val priority = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    useLocation(location)
                } else {
                    tvLocationTR.text = "Location unavailable"
                    tvLocation.text = "Location unavailable"
                    Toast.makeText(this, "Could not get location. Please turn on location services.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { error ->
                tvLocationTR.text = "Location unavailable"
                tvLocation.text = "Location unavailable"
                Toast.makeText(this, "Location error: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun useLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        tvLocationTR.text = "Detecting place..."
        tvLocation.text = "Detecting place..."
        updatePlaceName(lat, lon)
        viewModel.fetchWeather(Config.WEATHER_API_KEY, lat, lon)
        viewModel.fetchUnifiedSoil(lat, lon)
    }

    private fun updatePlaceName(lat: Double, lon: Double) {
        Thread {
            val placeName = try {
                val address = Geocoder(this, Locale.getDefault())
                    .getFromLocation(lat, lon, 1)
                    ?.firstOrNull()

                address?.locality
                    ?: address?.subAdminArea
                    ?: address?.adminArea
                    ?: address?.countryName
                    ?: "Location found"
            } catch (error: Exception) {
                "Location found"
            }

            runOnUiThread {
                tvLocationTR.text = placeName
                tvLocation.text = placeName
            }
        }.start()
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                fetchLocationAndData()
            } else {
                tvLocationTR.text = "Location denied"
                tvLocation.text = "Location denied"
                Toast.makeText(this, "Location permission is needed for weather and soil data.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
