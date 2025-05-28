package com.example.myapplicationennew

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
    }

    fun calculate(view: View) {
        // Temel işlemler: toplama, çıkarma, çarpma, bölme
        val num1 = findViewById<EditText>(R.id.num1).text.toString().toDoubleOrNull()
        val num2 = findViewById<EditText>(R.id.num2).text.toString().toDoubleOrNull()
        val resultTextView = findViewById<TextView>(R.id.resultTextView)

        if (num1 != null && num2 != null) {
            when (view.id) {
                R.id.addButton -> resultTextView.text = "Result: ${num1 + num2}"
                R.id.subtractButton -> resultTextView.text = "Result: ${num1 - num2}"
                R.id.multiplyButton -> resultTextView.text = "Result: ${num1 * num2}"
                R.id.divideButton -> {
                    if (num2 != 0.0) {
                        resultTextView.text = "Result: ${num1 / num2}"
                    } else {
                        resultTextView.text = "Cannot divide by zero"
                    }
                }
            }
        } else {
            resultTextView.text = "Please enter valid numbers"
        }
    }
}