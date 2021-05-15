package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    companion object {
        const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed $connectionResult ")

        Toast.makeText(this, "Google Play Services error", Toast.LENGTH_SHORT).show()
    }
    private var userName : String? = null
    private var userPhotoUrl : String? = null

    private var fireBashAuth : FirebaseAuth? = null
    private var fireBaseUser : FirebaseUser? = null

    private var googleApiClient : GoogleApiClient? = null

    private var firebaseDatabaseReference : DatabaseReference? = null
    private var firebaseAdapter : FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        googleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API)
            .build()

        userName = ANONYMOUS
        fireBashAuth = FirebaseAuth.getInstance()
        fireBaseUser = fireBashAuth!!.currentUser

        if (fireBaseUser == null) {
            startActivity(Intent(this@MainActivity, SignInActivity::class.java))
            finish()
        }else{
            userName = fireBaseUser!!.displayName

            if (fireBaseUser!!.photoUrl != null){
                userPhotoUrl = fireBaseUser!!.photoUrl!!.toString()
            }
        }
    }


}