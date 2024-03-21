package kr.young.firertc

import android.annotation.SuppressLint
import android.media.AudioManager
import android.os.Bundle
import android.os.PowerManager
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.databinding.ActivityAudioCallBinding
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call
import kr.young.firertc.util.ImageUtil
import kr.young.firertc.util.ImageUtil.Companion.selectImage
import kr.young.firertc.vm.AudioViewModel
import kr.young.firertc.vm.CallVM
import kr.young.firertc.vm.UserViewModel
import kr.young.rtp.RTPManager

class AudioCallActivity : AppCompatActivity(), OnClickListener, OnTouchListener {
    private lateinit var binding: ActivityAudioCallBinding
    private val audioViewModel = AudioViewModel.instance

    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val rtpManager = RTPManager.instance

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_audio_call)
        binding.user = audioViewModel.counterpart

        Glide.with(this)
            .load(ImageUtil.selectImageFromWeb(audioViewModel.counterpart!!.id))
            .placeholder(R.drawable.profile_placeholder)
            .error(R.drawable.outline_mood_24)
            .circleCrop()
            .into(binding.ivProfile)

//        Glide.with(this)
//            .load(ImageUtil.selectBackground(audioViewModel.counterpart!!.id))
//            .placeholder(R.drawable.profile_placeholder)
//            .centerCrop()
//            .into(binding.ivBackground)

        binding.ivMute.setOnClickListener(this)
        binding.ivMute.setOnTouchListener(this)
        binding.ivSpeaker.setOnClickListener(this)
        binding.ivSpeaker.setOnTouchListener(this)
        binding.ivEnd.setOnClickListener(this)
        binding.ivEnd.setOnTouchListener(this)

        powerManager = getSystemService(POWER_SERVICE) as PowerManager

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
        CallVM.instance.status.observe(this) {
            if (it.isNotEmpty()) {
                binding.tvStatus.text = it
            }
        }

        if (audioViewModel.call != null) {
            startCall()
        }
    }

    override fun onResume() {
        super.onResume()
        activateSensor()
    }

    override fun onPause() {
        super.onPause()
        deactivateSensor()
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

    private fun startCall() {
        rtpManager.init(this,
            isAudio = true,
            isVideo = false,
            isDataChannel = false,
            enableStat = false,
            recordAudio = false
        )

        val isOffer = audioViewModel.call!!.direction == Call.Direction.Offer
        rtpManager.startRTP(context = this, data = null, isOffer = isOffer, audioViewModel.remoteSDP, audioViewModel.remoteIce)
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

    private fun activateSensor() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "FireRTC:audio-call")
        }
        if (!wakeLock!!.isHeld) {
            wakeLock!!.acquire(10*60*1000L /*10 minutes*/)
            d(TAG, "activate not null and isHeld")
        } else {
            d(TAG, "activate null or not Held")
        }
    }

    private fun deactivateSensor() {
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
            d(TAG, "deactivate not null and isHeld")
        } else {
            d(TAG, "deactivate null or not Held")
        }
    }

    companion object {
        private const val TAG = "AudioCallActivity"
    }
}