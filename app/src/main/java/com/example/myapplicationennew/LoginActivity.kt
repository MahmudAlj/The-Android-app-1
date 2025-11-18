package com.example.myapplicationennew

class LoginActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = DatabaseHelper(this)

        val email = findViewById<EditText>(R.id.loginEmail)
        val password = findViewById<EditText>(R.id.loginPassword)
        val btn = findViewById<Button>(R.id.loginBtn)

        btn.setOnClickListener {
            val emailText = email.text.toString()
            val passText = password.text.toString()

            if (db.loginUser(emailText, passText)) {
                Toast.makeText(this, "Giriş başarılı!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Bilgiler yanlış!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
