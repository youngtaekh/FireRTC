package kr.young.firertc

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.databinding.ActivityProfileBinding
import kr.young.firertc.model.Call
import kr.young.firertc.util.BaseActivity
import kr.young.firertc.util.ImageUtil.Companion.selectBackground
import kr.young.firertc.util.ImageUtil.Companion.selectImageFromWeb
import kr.young.firertc.vm.*

class ProfileActivity : BaseActivity(), OnClickListener, OnTouchListener {

    private lateinit var binding: ActivityProfileBinding
    private val userViewModel = UserViewModel.instance

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.user = userViewModel.selectedProfile
        d(TAG, "user token ${userViewModel.selectedProfile!!.fcmToken}")

        Glide.with(this)
            .load(selectImageFromWeb(userViewModel.selectedProfile!!.id))
            .placeholder(R.drawable.profile_placeholder)
            .error(R.drawable.outline_mood_24)
            .circleCrop()
            .into(binding.ivProfile)

        Glide.with(this)
            .load(selectBackground(userViewModel.selectedProfile!!.id))
            .placeholder(R.drawable.profile_placeholder)
            .centerCrop()
            .into(binding.ivBackground)

        binding.ivClose.setOnTouchListener(this)
        binding.ivClose.setOnClickListener(this)
        binding.ivCall.setOnTouchListener(this)
        binding.ivCall.setOnClickListener(this)
        binding.ivVideo.setOnTouchListener(this)
        binding.ivVideo.setOnClickListener(this)
        binding.ivScreen.setOnTouchListener(this)
        binding.ivScreen.setOnClickListener(this)
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
            R.id.iv_close -> { finish() }
            R.id.iv_call -> { call() }
            R.id.iv_video -> { video() }
            R.id.iv_screen -> { screen() }
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
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            startForegroundService(Intent(this, CallService::class.java))
        }
    }

    private fun video() {
        d(TAG, "video call")
        val videoVM = VideoViewModel.instance
        videoVM.startOffer(userViewModel.selectedProfile!!, Call.Type.VIDEO) {
            videoVM.updateCallList()
            videoVM.updateParticipantList()
            startForegroundService(Intent(this, CallService::class.java))
            val intent = Intent(this, VideoCallActivity::class.java)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun screen() {
        d(TAG, "screen call")
        val videoVM = VideoViewModel.instance
        videoVM.startOffer(userViewModel.selectedProfile!!, Call.Type.SCREEN) {
            videoVM.updateCallList()
            videoVM.updateParticipantList()
            startForegroundService(Intent(this, CallService::class.java))
            val intent = Intent(this, VideoCallActivity::class.java)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun chat() {
        d(TAG, "chat")
        MessageVM.instance.participantMap[userViewModel.selectedProfile!!.id] = userViewModel.selectedProfile
        val intent = Intent(this, MessageActivity::class.java)
        intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    companion object {
        private const val TAG = "ProfileActivity"
    }
}