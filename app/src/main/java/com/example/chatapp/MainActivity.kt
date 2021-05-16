package com.example.chatapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    companion object {
        const val TAG = "MainActivity"
        const val ANONYMOUS = "anonymous"
        const val MESSAGE_CHILD = "messages"
        const val REQUEST_IMAGE = 1
        const val LOADING_IMAGE_URL = "https://upload.wikimedia.org/wikipedia/commons/b/b1/Loading_icon.gif"
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed $connectionResult ")

        Toast.makeText(this, "Google Play Services error", Toast.LENGTH_SHORT).show()
    }
    private var userName : String? = null
    private var userPhotoUrl : String? = null

    private var fireBaseAuth : FirebaseAuth? = null
    private var firebaseUser : FirebaseUser? = null

    private var googleApiClient : GoogleApiClient? = null

    lateinit var linearLayoutManager: LinearLayoutManager

    private var firebaseDatabaseReference : DatabaseReference? = null
    private var firebaseAdapter : FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null

    private var googleSignInClient : GoogleSignInClient? = null

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
        fireBaseAuth = FirebaseAuth.getInstance()
        firebaseUser = fireBaseAuth!!.currentUser

        if (firebaseUser == null) {
            startActivity(Intent(this@MainActivity, SignInActivity::class.java))
            finish()
        } else {
            userName = firebaseUser!!.displayName

            if (firebaseUser!!.photoUrl != null) {
                userPhotoUrl = firebaseUser!!.photoUrl!!.toString()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this@MainActivity, gso)

        val parser = SnapshotParser<Message> { snapshot: DataSnapshot ->

            val chatMessage = snapshot.getValue(Message::class.java)
            if (chatMessage != null) {
                chatMessage.id = snapshot.key
            }
            chatMessage!!
        }

        val messageRefs = firebaseDatabaseReference!!.child(MESSAGE_CHILD)

        val options = FirebaseRecyclerOptions.Builder<Message>()
                .setQuery(messageRefs, parser)
                .build()

        firebaseAdapter = object : FirebaseRecyclerAdapter<Message, MessageViewHolder>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): MessageViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false))
            }

            override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: Message) {
                findViewById<ProgressBar>(R.id.progress_bar).visibility = ProgressBar.INVISIBLE
                holder.bind(model)

            }

        }

        firebaseAdapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val messageCount = firebaseAdapter!!.itemCount
                val lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

                if (lastVisiblePosition == -1 || positionStart >= messageCount - 1 && lastVisiblePosition == positionStart - 1) {
                    findViewById<RecyclerView>(R.id.recycler_view)!!.scrollToPosition(positionStart)
                }
            }

        })

        findViewById<RecyclerView>(R.id.recycler_view)!!.layoutManager = linearLayoutManager
        findViewById<RecyclerView>(R.id.recycler_view)!!.adapter = firebaseAdapter

        findViewById<Button>(R.id.send_button).setOnClickListener {
            val message = Message(findViewById<EditText>(R.id.text_message_edit_text)!!.text.toString(), userName!!, userPhotoUrl, null)

            firebaseDatabaseReference!!.child(MESSAGE_CHILD).push().setValue(message)

            (findViewById<EditText>(R.id.text_message_edit_text)!!.setText(""))
        }

        findViewById<ImageView>(R.id.add_image_image_view).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE)
        }
    }

       override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

           if (requestCode == REQUEST_IMAGE){
               if(resultCode == Activity.RESULT_OK){
                   if(data != null){
                       val uri = data.data

                       val tempMessage = Message(null, userName, userPhotoUrl, LOADING_IMAGE_URL)
                       firebaseDatabaseReference!!.child(MESSAGE_CHILD).push().setValue(tempMessage){
                           databaseError, databaseReference ->
                           if (databaseError == null){
                               val key = databaseReference.key
                               val storageReference = FirebaseStorage.getInstance()
                                       .getReference(firebaseUser!!.uid)
                                       .child(key!!)
                                       .child(uri!!.lastPathSegment!!)



                               putImageInStorage(storageReference,uri,key)
                           }else{
                               Log.e(TAG, "Unable to write message to database ${databaseError.toException()}")
                           }
                   }
               }
           }
        }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri : Uri?, key : String?){
        val uploadTask = storageReference.putFile(uri!!)
        uploadTask.continueWithTask{
            task ->
            if (!task.isSuccessful){
                throw task.exception!!
            }
            storageReference.downloadUrl
        }.addOnCompleteListener{
            task ->
            if (task.isSuccessful){
                val downloadUrl = task.result!!.toString()
                val message = Message(null,userName, userPhotoUrl, downloadUrl)

                firebaseDatabaseReference!!.child(MESSAGE_CHILD).child(key!!).setValue(message)
            }else{
                Log.e(TAG, "Image upload task was not successful ${task.exception}")
            }
        }
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
            userImage = itemView.findViewById(R.id.messenger_image_view)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.sign_out_item ->{
                fireBaseAuth!!.signOut()
                fireBaseAuth = null
                userName = ANONYMOUS
                userPhotoUrl = null

                googleSignInClient!!.revokeAccess().addOnCompleteListener(this@MainActivity){
                    startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                    finish()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        firebaseAdapter!!.stopListening()
    }

    override fun onResume() {
        super.onResume()
        firebaseAdapter!!.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.overflow_menu, menu)
        return true
    }


}