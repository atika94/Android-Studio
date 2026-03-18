package com.example.roadsigntest.model

data class Question(
    val language: String = "",
    val questionText: String = "",
    val questionImage: String = "", // URL or empty
    val options: List<Option> = emptyList(), // Size should be 3
    val correctIndex: Int = 0
)

data class Option(
    val text: String = "",
    val image: String = "" // URL or empty
)
