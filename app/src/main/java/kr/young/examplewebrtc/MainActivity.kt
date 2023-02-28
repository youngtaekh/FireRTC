package kr.young.examplewebrtc

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.i
import kr.young.common.UtilLog.Companion.w
import kr.young.examplewebrtc.databinding.ActivityMainBinding
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.repo.AppSP
import kr.young.examplewebrtc.util.BaseActivity
import kr.young.examplewebrtc.vm.*

class MainActivity : BaseActivity(), OnTouchListener, OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var spaceViewModel: SpaceViewModel
    private val signLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            i(TAG, "result ok")
        } else if (result.resultCode == RESULT_CANCELED) {
            i(TAG, "result canceled")
            finish()
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            i(TAG, "notification permission granted")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        i(TAG, "onCreate()")

        val myViewModel = MyDataViewModel.instance

        binding.tvJoin.setOnTouchListener(this)
        binding.tvJoin.setOnClickListener(this)
        binding.tvUserStart.setOnTouchListener(this)
        binding.tvUserStart.setOnClickListener(this)
        binding.tvStart.setOnTouchListener(this)
        binding.tvStart.setOnClickListener(this)
        binding.tvStop.setOnTouchListener(this)
        binding.tvStop.setOnClickListener(this)

        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == IME_ACTION_DONE) {
                d(TAG, "IME_ACTION_DONE")
                join()
                true
            } else {
                false
            }
        }
        binding.etUserName.setOnEditorActionListener {_, actionId, _ ->
            if (actionId == IME_ACTION_DONE) {
                userStart()
            }
            false
        }

        spaceViewModel = SpaceViewModel.instance
        myViewModel.myData.observe(this) {
            d(TAG, "myData.observe")
            if (it != null) {
                binding.user = it
            }
            if (AppSP.instance.isSignIn()) {
                binding.tvName.visibility = VISIBLE
                binding.rlInput.visibility = GONE
                binding.tvJoin.visibility = VISIBLE
            } else {
                binding.tvName.visibility = GONE
                binding.rlInput.visibility = VISIBLE
                binding.tvJoin.visibility = GONE
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
//            val msg = getString(R.string.msg_token_fmt, token)
            AppSP.instance.setFCMToken(token)
            d(TAG, token)
        })

        Firebase.messaging.subscribeToTopic("topic")
            .addOnCompleteListener { task ->
                var msg = "Subscribed"
                if (!task.isSuccessful) {
                    msg = "Subscribe failed"
                }
                d(TAG, msg)
            }

        if (AppSP.instance.isSignIn()) {
            binding.tvName.visibility = VISIBLE
            binding.rlInput.visibility = GONE
            binding.tvJoin.visibility = VISIBLE
        } else {
            binding.tvName.visibility = GONE
            binding.rlInput.visibility = VISIBLE
            binding.tvJoin.visibility = GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_join, R.id.tv_user_start, R.id.tv_start, R.id.tv_stop -> TouchEffect.alpha(v, event)
        }
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_join -> join()
            R.id.tv_user_start -> userStart()
            R.id.tv_start -> { startService(Intent(this, CallService::class.java)) }
            R.id.tv_stop -> { stopService(Intent(this, CallService::class.java)) }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                i(TAG, "notification permission granted")
            } else if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                i(TAG, "shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)")
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }

    private fun userStart() {
        d(TAG, "userStart")
        if (binding.etUserName.text.isNotEmpty()) {
            val user = User(id = binding.etUserName.text.toString(), fcmToken = AppSP.instance.getFCMToken())
            MyDataViewModel.instance.createUser(user)
        } else {
            binding.tvUserWarn.text = getString(R.string.empty_warn)
        }
    }

    private fun join() {
        d(TAG, "join")
        if (binding.etName.text.toString().isNotEmpty()) {
            spaceViewModel.release()
            CallViewModel.instance.release()
            spaceViewModel.joinSpace(binding.etName.text.toString())
            binding.etName.setText("")
            val intent = Intent(this, CallActivity::class.java)
            startActivity(intent)
        } else {
            binding.tvWarn.text = getString(R.string.empty_warn)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}