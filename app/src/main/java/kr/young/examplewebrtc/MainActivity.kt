package kr.young.examplewebrtc

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.databinding.DataBindingUtil
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.young.examplewebrtc.databinding.ActivityMainBinding
import kr.young.examplewebrtc.model.User

class MainActivity : AppCompatActivity(), OnTouchListener, OnClickListener {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.tvPost.setOnTouchListener(this)
        binding.tvPost.setOnClickListener(this)
        binding.tvGet.setOnTouchListener(this)
        binding.tvGet.setOnClickListener(this)
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_post -> {}
            R.id.tv_get -> {}
        }
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_post -> {
                val user = User(binding.etId.text.toString(), binding.etName.toString())
//                binding.user = user

                val db = Firebase.firestore
                db.collection(User.COLLECTION).document(user.id)
                    .set(user.toMap())
                    .addOnSuccessListener {
                        Log.d(TAG, "DocumentSnapshot added with ID")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }
            }
            R.id.tv_get -> {
                val db = Firebase.firestore
                db.collection(User.COLLECTION)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            Log.d(TAG, "${document.id} => ${document.data}")
                            val user = User(document.id, document.data)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents.", exception)
                    }
            }
        }
    }
}