package com.example.chatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class SignInActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignInActivity"
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

        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener{
            signIn()
        }
    }

    private fun signIn(){
        val signInIntent = googleSignInClient!!.signInIntent

        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN){
            if(resultCode == Activity.RESULT_OK){
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)

                try{
                 val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)
                }catch (e : ApiException){
                    Log.e(TAG,"Google Sign in failed $e")
                    Toast.makeText(this@SignInActivity, "Google Sign in failed", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(account : GoogleSignInAccount){
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        fireBaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this@SignInActivity){task ->

                    if (task.isSuccessful){
                        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                        finish()
                    }else{
                        Toast.makeText(this@SignInActivity,"Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                }

    }

}