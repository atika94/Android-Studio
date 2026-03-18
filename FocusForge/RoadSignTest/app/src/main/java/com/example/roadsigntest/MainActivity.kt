package com.example.roadsigntest

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.roadsigntest.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load Ad
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        // Setup Clicks
        binding.btnEnglish.setOnClickListener {
            val intent = Intent(this, MCQActivity::class.java)
            intent.putExtra("LANGUAGE", "english")
            startActivity(intent)
        }

        binding.btnUrdu.setOnClickListener {
            val intent = Intent(this, MCQActivity::class.java)
            intent.putExtra("LANGUAGE", "urdu")
            startActivity(intent)
        }

        binding.btnCalculator.setOnClickListener {
            startActivity(Intent(this, FeeCalculatorActivity::class.java))
        }

        binding.btnPractice.setOnClickListener {
            startActivity(Intent(this, UnityActivity::class.java))
        }
    }
}