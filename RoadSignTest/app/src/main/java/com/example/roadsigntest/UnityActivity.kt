package com.example.roadsigntest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.roadsigntest.databinding.ActivityUnityBinding

class UnityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Practice logic or Unity integration would go here
    }
}
