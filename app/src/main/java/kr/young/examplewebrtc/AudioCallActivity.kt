package kr.young.examplewebrtc

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.databinding.ActivityAudioCallBinding
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.vm.AudioViewModel
import kr.young.examplewebrtc.vm.UserViewModel

class AudioCallActivity : AppCompatActivity(), OnClickListener, OnTouchListener {
    private lateinit var binding: ActivityAudioCallBinding
    private val audioViewModel = AudioViewModel.instance

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_audio_call)
        binding.user = audioViewModel.counterpart

        binding.ivProfile.setImageResource(UserViewModel.instance.selectImage(audioViewModel.counterpart!!.id))
        binding.ivMute.setOnClickListener(this)
        binding.ivMute.setOnTouchListener(this)
        binding.ivSpeaker.setOnClickListener(this)
        binding.ivSpeaker.setOnTouchListener(this)
        binding.ivEnd.setOnClickListener(this)
        binding.ivEnd.setOnTouchListener(this)

        audioViewModel.terminatedCall.observe(this) {
            if (it != null && it) {
                finish()
            }
        }
        audioViewModel.mute.observe(this) {
            if (it != null) {
                if (it) {
                    binding.ivMute.setImageResource(R.drawable.round_mute_off_24)
                } else {
                    binding.ivMute.setImageResource(R.drawable.round_mute_24)
                }
            }
        }
        audioViewModel.speaker.observe(this) {
            if (it != null) {
                if (it) {
                    binding.ivSpeaker.setImageResource(R.drawable.round_speaker_off_24)
                } else {
                    binding.ivSpeaker.setImageResource(R.drawable.round_speaker_24)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_mute -> { mute() }
            R.id.iv_speaker -> { speaker() }
            R.id.iv_end -> { end() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return onTouchEvent(event)
    }

    private fun mute() {
        d(TAG, "mute")
        audioViewModel.mute()
    }

    private fun speaker() {
        d(TAG, "speaker")
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioViewModel.speaker(audioManager)
    }

    private fun end() {
        d(TAG, "end")
        if (audioViewModel.call!!.connected) {
            audioViewModel.end()
        } else {
            audioViewModel.end(SendFCM.FCMType.Cancel)
        }
    }

    companion object {
        private const val TAG = "AudioCallActivity"
    }
}