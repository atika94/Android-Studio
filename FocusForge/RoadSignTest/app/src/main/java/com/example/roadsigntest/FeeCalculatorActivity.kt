package com.example.roadsigntest

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.roadsigntest.databinding.ActivityFeeCalculatorBinding
import com.google.android.gms.ads.AdRequest
import com.google.firebase.firestore.FirebaseFirestore

class FeeCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeeCalculatorBinding
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FeeCalculatorActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeeCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load Ad
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        setupSpinners()

        binding.btnCalculate.setOnClickListener {
            calculateFee()
        }
    }

    private fun setupSpinners() {
        // License Type Spinner
        val licenseTypes = arrayOf("Learner", "Permanent", "Renewal", "International")
        val licenseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, licenseTypes)
        binding.spinnerLicenseType.adapter = licenseAdapter

        // Duration Spinner
        val durations = arrayOf("1 Year", "3 Years", "5 Years")
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, durations)
        binding.spinnerDuration.adapter = durationAdapter

        // Vehicle Type Spinner (Updated based on Punjab categories)
        val vehicleTypes = arrayOf(
            "Motorcycle", "Car / Jeep", "LTV (Taxi/Minibus)", 
            "HTV (Truck/Bus)", "PSV", "Agricultural Tractor"
        )
        val vehicleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes)
        binding.spinnerVehicleType.adapter = vehicleAdapter

        // Logic handle for Learner: usually Learner doesn't have duration
        binding.spinnerLicenseType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = licenseTypes[position]
                if (selected == "Learner") {
                    binding.spinnerDuration.isEnabled = false
                    binding.spinnerDuration.alpha = 0.5f
                } else {
                    binding.spinnerDuration.isEnabled = true
                    binding.spinnerDuration.alpha = 1.0f
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun calculateFee() {
        // Hardcoded to Punjab as per user request
        val province = "punjab"
        val licenseType = binding.spinnerLicenseType.selectedItem.toString().lowercase()
        val duration = binding.spinnerDuration.selectedItem.toString().replace(" ", "").lowercase().replace("years", "yr").replace("year", "yr")
        val vehicle = getVehicleKey(binding.spinnerVehicleType.selectedItem.toString())

        // Construct key: {type}_{duration}_{vehicle}
        // If Learner, format is: learner_{vehicle}
        val firestoreKey = if (licenseType == "learner") {
            "${licenseType}_${vehicle}"
        } else {
            "${licenseType}_${duration}_${vehicle}"
        }

        Log.d(TAG, "Fetching fee for Province: $province, Key: $firestoreKey")

        db.collection("licence_fees").document(province)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fee = document.get(firestoreKey)
                    if (fee != null) {
                        binding.tvResult.text = getString(R.string.fee_result_format, fee.toString())
                    } else {
                        binding.tvResult.text = ""
                        Toast.makeText(this, R.string.fee_not_found, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.tvResult.text = ""
                    Toast.makeText(this, "Fee data for Punjab not found in Firestore", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching fee", e)
                Toast.makeText(this, "Error fetching fee: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getVehicleKey(selectedVehicle: String): String {
        return when (selectedVehicle) {
            "Motorcycle" -> "bike"
            "Car / Jeep" -> "car"
            "LTV (Taxi/Minibus)" -> "ltv"
            "HTV (Truck/Bus)" -> "htv"
            "PSV" -> "psv"
            "Agricultural Tractor" -> "tractor_agri"
            else -> "car"
        }
    }
}
