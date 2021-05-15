package com.example.chatapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth



class SignInActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 1
    }

    private var googleSignInClient : GoogleSignInClient? = null
    private var fireBaseAuth : FirebaseAuth? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)


        fireBaseAuth = FirebaseAuth.getInstance()


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this@SignInActivity,gso)

        findViewById<Button>(R.id.sign_in_button).setOnClickListener{
            signIn()
        }
    }

    private fun signIn(){
        val signInIntent = googleSignInClient!!.signInIntent

        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
}