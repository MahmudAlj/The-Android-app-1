package com.example.myapplicationennew

class RegisterActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = DatabaseHelper(this)

        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        val btn = findViewById<Button>(R.id.registerBtn)

        btn.setOnClickListener {
            val emailText = email.text.toString()
            val passText = password.text.toString()

            if (emailText.isEmpty() || passText.isEmpty()) {
                Toast.makeText(this, "Boş bırakma!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = db.registerUser(emailText, passText)

            if (success) {
                Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Bu email zaten kayıtlı!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
