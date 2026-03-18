package com.example.roadsigntest

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.roadsigntest.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 0)

        // Calculate Percentage
        val percentage = if (total > 0) (score * 100) / total else 0

        // Display Score
        binding.tvScore.text = "Score: $score / $total"
        binding.tvPercentage.text = "$percentage%"

        // Determine Grade & Message
        val (gradeMessage, isPass) = when {
            percentage >= 90 -> "Outstanding! You're a pro." to true
            percentage >= 70 -> "Good Job! You passed." to true
            percentage >= 50 -> "Passed, but keep practicing." to true
            else -> "Failed. Please try again." to false
        }

        binding.tvGradeMessage.text = gradeMessage

        // Change icon color based on pass/fail
        if (isPass) {
            binding.ivResultIcon.setColorFilter(getColor(R.color.greenPrimary))
            binding.ivResultIcon.setImageResource(R.drawable.ic_check_circle)
        } else {
            binding.ivResultIcon.setColorFilter(getColor(android.R.color.holo_red_dark))
            binding.ivResultIcon.setImageResource(android.R.drawable.ic_delete) // Built-in X icon
        }

        binding.btnHome.setOnClickListener {
            // Go back to Main Activity and clear stack so user can't "back" into the result
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
