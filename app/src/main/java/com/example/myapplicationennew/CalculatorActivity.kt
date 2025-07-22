package com.example.myapplicationennew

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var currentInput: String = ""
    private var lastOperator: Char? = null
    private var result: Double = 0.0
    private var justEvaluated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        display = findViewById(R.id.calculatorDisplay)
    }

    fun onDigitClick(view: View) {
        val btn = view as Button
        val digit = btn.text.toString()

        if (justEvaluated) {
            currentInput = ""
            justEvaluated = false
        }

        currentInput += digit
        display.text = currentInput
    }

    fun onOperatorClick(view: View) {
        val btn = view as Button
        val operator = btn.text[0]

        evaluatePending()
        lastOperator = operator
        currentInput = ""
    }

    fun onEqualClick(view: View) {
        evaluatePending()
        display.text = result.toString()
        justEvaluated = true
    }

    private fun evaluatePending() {
        val num = currentInput.toDoubleOrNull()
        if (num != null) {
            if (lastOperator == null) {
                result = num
            } else {
                result = when (lastOperator) {
                    '+' -> result + num
                    '-' -> result - num
                    '×' -> result * num
                    '÷' -> if (num != 0.0) result / num else {
                        display.text = "Sıfıra bölünemez"
                        return
                    }
                    '%' -> result % num
                    else -> result
                }
            }
        }
    }

    fun onClearClick(view: View) {
        currentInput = ""
        result = 0.0
        lastOperator = null
        display.text = "0"
    }

    fun onBackspaceClick(view: View) {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            display.text = if (currentInput.isEmpty()) "0" else currentInput
        }
    }
}
