package com.example.chatapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    companion object {
        const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
        const val MESSAGE_CHILD = "messages"
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

    lateinit var linearLayoutManager: LinearLayoutManager

    private var firebaseDatabaseReference : DatabaseReference? = null
    private var firebaseAdapter : FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        linearLayoutManager = LinearLayoutManager(this@MainActivity)
        linearLayoutManager.stackFromEnd = true

        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference

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

        val parser = SnapshotParser<Message>{
            snapshot : DataSnapshot ->

            val chatMessage = snapshot.getValue(Message::class.java)
            if (chatMessage != null){
                chatMessage.id = snapshot.key
            }
            chatMessage!!
        }

        val messageRefs = firebaseDatabaseReference!!.child(MESSAGE_CHILD)

        val options = FirebaseRecyclerOptions.Builder<Message>()
            .setQuery(messageRefs, parser)
            .build()

        firebaseAdapter = object : FirebaseRecyclerAdapter<Message, MessageViewHolder>(options){
            override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): MessageViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false))
            }

            override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: Message) {
                findViewById<ProgressBar>(R.id.progress_bar).visibility = ProgressBar.INVISIBLE
                holder.bind(model)

            }

        }

        firebaseAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver(){
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val messageCount = firebaseAdapter!!.itemCount
                val lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

                if (lastVisiblePosition == -1 || positionStart >= messageCount - 1 && lastVisiblePosition == positionStart - 1){
                    findViewById<RecyclerView>(R.id.recycler_view)!!.scrollToPosition(positionStart)
                }
            }

        })

        findViewById<RecyclerView>(R.id.recycler_view)!!.layoutManager = linearLayoutManager
        findViewById<RecyclerView>(R.id.recycler_view)!!.adapter = firebaseAdapter

    }

    class MessageViewHolder(v : View) : RecyclerView.ViewHolder(v){

        lateinit var message : Message

        var messageTextView : TextView
        var messageImageView : ImageView
        var nameTextView : TextView
        var userImage : CircleImageView

        init {
            messageTextView = itemView.findViewById(R.id.message_text_view)
            messageImageView = itemView.findViewById(R.id.message_image_view)
            nameTextView = itemView.findViewById(R.id.name_text_view)
            userImage = itemView.findViewById(R.id.name_text_view)

        }

        fun bind(message : Message){
            this.message = message

            if(message.text != null){
                messageTextView.text = message.text

                messageTextView.visibility = View.VISIBLE
                messageImageView.visibility = View.GONE

            }else if (message.imageUrl != null){
                messageTextView.visibility = View.GONE
                messageImageView.visibility = View.VISIBLE

                val imageUrl = message.imageUrl

                if (imageUrl!!.startsWith("gs://")){
                    val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)

                    storageReference.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful){
                            val downloadUrl = task.result!!. toString()

                            Glide.with(messageImageView.context)
                                .load(downloadUrl)
                                .into(messageImageView)

                        }else{
                            Log.e(TAG, "Getting Download url was not successful ${task.exception}")
                        }
                    }
                }else{
                    Glide.with(messageImageView.context)
                        .load(Uri.parse(message.imageUrl))
                        .into(messageImageView)
                }
            }

            nameTextView.text = message.name

            if(message.photoUrl == null){
                userImage.setImageDrawable(ContextCompat.getDrawable(userImage.context, R.drawable.ic_account_circle))
            }else{
                Glide.with(userImage.context)
                    .load(Uri.parse(message.photoUrl))
                    .into(userImage)
            }
        }


    }


}