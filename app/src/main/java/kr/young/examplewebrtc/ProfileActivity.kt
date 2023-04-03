package kr.young.examplewebrtc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.databinding.ActivityProfileBinding
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.vm.AudioViewModel
import kr.young.examplewebrtc.vm.UserViewModel

class ProfileActivity : AppCompatActivity(), OnClickListener, OnTouchListener {

    private lateinit var binding: ActivityProfileBinding
    private val userViewModel = UserViewModel.instance

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.user = userViewModel.selectedProfile

        binding.ivProfile.setImageResource(userViewModel.selectImage(userViewModel.selectedProfile?.id))

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
        val audioVM = AudioViewModel.instance
        audioVM.startOffer(counterpart = userViewModel.selectedProfile!!, type = Call.Type.AUDIO) {
            audioVM.updateCallList()
            audioVM.updateParticipantList()
            val intent = Intent(this, AudioCallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            startForegroundService(Intent(this, CallService::class.java))
        }
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