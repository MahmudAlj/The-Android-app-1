package com.example.myapplicationennew

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var currentInput: String = ""
    private var lastOperator: Char? = null
    private var result: Double = 0.0
    private var justEvaluated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        display = findViewById(R.id.calculatorDisplay)

        val btnBack: Button = findViewById(R.id.btnBackCalc)
        btnBack.setOnClickListener { finish() }
    }

    fun onDigitClick(view: View) {
        val text = (view as Button).text.toString()

        if (justEvaluated) {
            currentInput = ""
            justEvaluated = false
        }

        if (text == "." && currentInput.contains(".")) return
        currentInput += text
        display.text = currentInput
    }

    fun onOperatorClick(view: View) {
        val op = (view as Button).text.first()

        if (currentInput.isNotEmpty()) {
            val value = currentInput.toDouble()
            if (lastOperator == null) {
                result = value
            } else {
                result = applyOperator(result, value, lastOperator!!)
            }
            currentInput = ""
        }

        lastOperator = op
        justEvaluated = false
        display.text = result.toString()
    }

    fun onEqualClick(view: View) {
        if (currentInput.isEmpty() && lastOperator == null) return

        val value = if (currentInput.isEmpty()) 0.0 else currentInput.toDouble()
        result = if (lastOperator == null) {
            value
        } else {
            applyOperator(result, value, lastOperator!!)
        }

        display.text = result.toString()
        currentInput = ""
        lastOperator = null
        justEvaluated = true
    }

    fun onClearClick(view: View) {
        currentInput = ""
        result = 0.0
        lastOperator = null
        justEvaluated = false
        display.text = "0"
    }

    fun onBackspaceClick(view: View) {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            display.text = if (currentInput.isEmpty()) "0" else currentInput
        }
    }

    private fun applyOperator(a: Double, b: Double, op: Char): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            'x', '×', '*' -> a * b
            '÷', '/' -> if (b == 0.0) 0.0 else a / b
            '%' -> a % b
            else -> b
        }
    }
}
