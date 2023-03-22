package kr.young.examplewebrtc.activity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.R
import kr.young.examplewebrtc.databinding.ActivityProfileBinding
import kr.young.examplewebrtc.vm.UserViewModel

class ProfileActivity : AppCompatActivity(), OnClickListener, OnTouchListener {

    private lateinit var binding: ActivityProfileBinding
    private val userViewModel = UserViewModel.instance

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.user = userViewModel.selectedProfile

        binding.ivCall.setOnTouchListener(this)
        binding.ivCall.setOnClickListener(this)
        binding.ivVideo.setOnTouchListener(this)
        binding.ivVideo.setOnClickListener(this)
        binding.ivChat.setOnTouchListener(this)
        binding.ivChat.setOnClickListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_call -> { call() }
            R.id.iv_video -> { video() }
            R.id.iv_chat -> { chat() }
        }
    }

    private fun call() {
        d(TAG, "audio call")
    }

    private fun video() {
        d(TAG, "video call")
    }

    private fun chat() {
        d(TAG, "chat")
    }

    companion object {
        private const val TAG = "ProfileActivity"
    }
}