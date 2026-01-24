package com.example.roadsigntest

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.example.roadsigntest.databinding.ActivityMcqBinding
import com.example.roadsigntest.model.Question
import com.google.android.gms.ads.AdRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import java.util.Locale


class MCQActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMcqBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val questionsList = mutableListOf<Question>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var currentLanguage = "english"
    
    // Track selected index
    private var selectedOptionIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMcqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLanguage = intent.getStringExtra("LANGUAGE") ?: "english"

        // Force RTL if Urdu
        if (currentLanguage.equals("urdu", ignoreCase = true)) {
             window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        } else {
             window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        // Load Ad
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        setupOptionClicks()
        fetchQuestions()

        binding.btnNext.setOnClickListener {
            checkAnswerAndNext()
        }
    }

    private fun setupOptionClicks() {
        binding.layoutOption1.setOnClickListener { selectOption(0) }
        binding.layoutOption2.setOnClickListener { selectOption(1) }
        binding.layoutOption3.setOnClickListener { selectOption(2) }
    }

    private fun selectOption(index: Int) {
        selectedOptionIndex = index
        updateRadioUI()
    }
    
    private fun updateRadioUI() {
        val radioImages = listOf(binding.ivRadio1, binding.ivRadio2, binding.ivRadio3)
        radioImages.forEachIndexed { i, imageView ->
             if (i == selectedOptionIndex) {
                 imageView.setImageResource(R.drawable.ic_radio_checked)
             } else {
                 imageView.setImageResource(R.drawable.ic_radio_unchecked)
             }
        }
    }

    private fun fetchQuestions() {
        // Show Loading (Simple Toast for now or ProgressBar if added to XML)
        Toast.makeText(this, "Loading questions...", Toast.LENGTH_SHORT).show()

        firestore.collection("questions")
            .whereEqualTo("language", currentLanguage)
            .get()
            .addOnSuccessListener { documents ->
                questionsList.clear()
                val totalDocs = documents.size()
                var parseErrors = 0
                
                for (document in documents) {
                    try {
                        val lang = document.getString("language") ?: ""
                        val qText = document.getString("questionText") ?: ""
                        val qImage = document.getString("questionImage") ?: ""
                        
                        // Robust correctIndex parsing (Handle Number or String)
                        var cIndex = 0
                        try {
                            // Try formatting as number first
                            cIndex = document.getLong("correctIndex")?.toInt() 
                                    ?: document.getString("correctIndex")?.trim()?.toIntOrNull() 
                                    ?: 0
                        } catch (e: Exception) {
                            cIndex = 0
                        }

                        // Smart Option Parsing
                        val rawOptions = document.get("options")
                        val parsedOptions = mutableListOf<com.example.roadsigntest.model.Option>()

                        if (rawOptions is List<*>) {
                            for (item in rawOptions) {
                                if (item is String) {
                                    // Handle simple string list ["A", "B", "http://..."]
                                    if (item.trim().startsWith("http", ignoreCase = true)) {
                                        // It's an Image URL!
                                        parsedOptions.add(com.example.roadsigntest.model.Option(text = "", image = item))
                                    } else {
                                        // It's Text
                                        parsedOptions.add(com.example.roadsigntest.model.Option(text = item, image = ""))
                                    }
                                } else if (item is Map<*, *>) {
                                    // Handle map list [{text="A", image=""}]
                                    val text = item["text"] as? String ?: ""
                                    val image = item["image"] as? String ?: ""
                                    parsedOptions.add(com.example.roadsigntest.model.Option(text = text, image = image))
                                }
                            }
                        }

                        val question = Question(
                            language = lang,
                            questionText = qText,
                            questionImage = qImage,
                            correctIndex = cIndex,
                            options = parsedOptions
                        )
                        questionsList.add(question)
                    } catch (e: Exception) {
                        parseErrors++
                        e.printStackTrace()
                    }
                }
                
                if (questionsList.isEmpty()) {
                    val msg = if (totalDocs == 0) {
                        "Found 0 docs for '$currentLanguage'. Check Firestore: Collection 'questions', field 'language': '$currentLanguage'"
                    } else {
                        "Found $totalDocs docs but failed to read ALL of them ($parseErrors errors). Check Field Names!"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    if (questionsList.size < totalDocs) {
                        Toast.makeText(this, "Warning: Loaded ${questionsList.size} of $totalDocs questions. Some documents had errors.", Toast.LENGTH_LONG).show()
                    }
                    questionsList.shuffle()
                    currentQuestionIndex = 0
                    score = 0
                    showQuestion()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Fetch Error: ${exception.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun showQuestion() {
        if (currentQuestionIndex >= questionsList.size) {
            showResult()
            return
        }
        
        // Reset selection
        selectedOptionIndex = -1
        updateRadioUI()

        val question = questionsList[currentQuestionIndex]
        
        // Update Headers
        binding.tvQuestionNumber.text = getString(R.string.mcq_question_number, currentQuestionIndex + 1, questionsList.size)
        // Keep score updated but maybe hide it until end? Logic says "Track internal", UI showed it. Let's keep it visible.
        binding.tvScore.text = "Score: $score"

        // 1. Question Text
        if (question.questionText.isNotEmpty()) {
            binding.tvQuestionText.visibility = View.VISIBLE
            binding.tvQuestionText.text = question.questionText
        } else {
            binding.tvQuestionText.visibility = View.GONE
        }

        // 2. Question Image
        if (question.questionImage.isNotEmpty()) {
            binding.ivQuestionImage.visibility = View.VISIBLE
            Glide.with(this).load(question.questionImage).into(binding.ivQuestionImage)
        } else {
            binding.ivQuestionImage.visibility = View.GONE
        }

        // 3. Options configuration
        val containers = listOf(binding.layoutOption1, binding.layoutOption2, binding.layoutOption3)
        val textViews = listOf(binding.tvOption1, binding.tvOption2, binding.tvOption3)
        val imageViews = listOf(binding.ivOption1, binding.ivOption2, binding.ivOption3)

        question.options.forEachIndexed { index, option ->
            if (index < containers.size) {
                containers[index].visibility = View.VISIBLE
                
                // Text Option
                if (option.text.isNotEmpty()) {
                    textViews[index].visibility = View.VISIBLE
                    textViews[index].text = option.text
                } else {
                    textViews[index].visibility = View.GONE
                }
                
                // Image Option
                if (option.image.isNotEmpty()) {
                    imageViews[index].visibility = View.VISIBLE
                    Glide.with(this).load(option.image).into(imageViews[index])
                } else {
                    imageViews[index].visibility = View.GONE
                }
            }
        }
        
        // Hide unused options
        for (i in question.options.size until containers.size) {
            containers[i].visibility = View.GONE
        }
    }

    private fun checkAnswerAndNext() {
        if (selectedOptionIndex == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        val question = questionsList[currentQuestionIndex]
        
        if (selectedOptionIndex == question.correctIndex) {
            score++
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
        }

        currentQuestionIndex++
        showQuestion()
    }

    private fun showResult() {
        val intent = android.content.Intent(this, ResultActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL", questionsList.size)
        startActivity(intent)
        finish()
    }
}
