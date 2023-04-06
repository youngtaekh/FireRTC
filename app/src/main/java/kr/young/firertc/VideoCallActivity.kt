package kr.young.firertc

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
import android.os.PowerManager.WakeLock
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.databinding.ActivityVideoCallBinding
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call
import kr.young.firertc.vm.VideoViewModel
import kr.young.rtp.RTPManager

class VideoCallActivity : AppCompatActivity(), OnClickListener, OnTouchListener, SensorEventListener {
    private lateinit var binding: ActivityVideoCallBinding
    private val callVM = VideoViewModel.instance

    private lateinit var powerManager: PowerManager
    private var wakeLock: WakeLock? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var proximity: Sensor
    private var isNear = true
    private var forceCameraMute = false
    private var isScreen = false

    private val rtpManager = RTPManager.instance

    private var screenIntent: Intent? = null
    private var screenCode = 0
    private var visibility = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_call)

        binding.ivEnd.setOnTouchListener(this)
        binding.ivEnd.setOnClickListener(this)
        binding.svrFull.setOnClickListener(this)
        binding.svrPip.setOnClickListener(this)

        binding.ivCamera.setOnTouchListener(this)
        binding.ivCamera.setOnClickListener(this)
        binding.ivSwitch.setOnTouchListener(this)
        binding.ivSwitch.setOnClickListener(this)
        binding.ivMute.setOnTouchListener(this)
        binding.ivMute.setOnClickListener(this)
        binding.ivHd.setOnTouchListener(this)
        binding.ivHd.setOnClickListener(this)
        binding.ivScale.setOnTouchListener(this)
        binding.ivScale.setOnClickListener(this)

        binding.tvName.text = callVM.counterpart!!.name

        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(TYPE_PROXIMITY)

        callVM.terminatedCall.observe(this) {
            if (it != null && it) {
                finish()
            }
        }

        callVM.cameraMute.observe(this) {
            if (it != null) {
                if (it) {
                    binding.ivCamera.setImageResource(R.drawable.round_videocam_24)
                } else {
                    binding.ivCamera.setImageResource(R.drawable.round_videocam_off_24)
                }
            }
        }
        callVM.mute.observe(this) {
            if (it != null) {
                if (it) {
                    binding.ivMute.setImageResource(R.drawable.round_mute_off_24)
                } else {
                    binding.ivMute.setImageResource(R.drawable.round_mute_24)
                }
            }
        }
        callVM.sd.observe(this) {
            if (it) {
                binding.ivHd.setImageResource(R.drawable.round_hd_24)
            } else {
                binding.ivHd.setImageResource(R.drawable.round_sd_24)
            }
        }

        this.isScreen = callVM.call!!.type == Call.Type.SCREEN
        if (callVM.call != null) {
            if (isScreen && callVM.call!!.direction == Call.Direction.Offer) {
                binding.ivCamera.visibility = GONE
                binding.svrPip.visibility = GONE
                startScreenCapture()
            } else {
                startCall()
                if (isScreen && callVM.call!!.direction == Call.Direction.Answer) {
                    binding.ivCamera.visibility = GONE
                    binding.ivSwitch.visibility = GONE
                    binding.ivHd.visibility = GONE
                    binding.svrPip.visibility = GONE
                    d(TAG, "screen answer")
                    cameraMute(true)
                }
            }
        }

        callVM.speaker(getSystemService(AUDIO_SERVICE) as AudioManager, true)
    }

    override fun onStart() {
        super.onStart()
        d(TAG, "onStart forceCameraMute $forceCameraMute")
        if (!forceCameraMute && !isScreen) {
            cameraMute(false)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, proximity, SENSOR_DELAY_NORMAL)
        activateSensor()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        deactivateSensor()
    }

    override fun onStop() {
        super.onStop()
        d(TAG, "onStop forceCameraMute $forceCameraMute")
        if (!forceCameraMute && !isScreen) {
            cameraMute(true)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_end -> { end() }
            R.id.svr_full -> { toggleVisibility() }
            R.id.svr_pip -> { callVM.setSwappedFeeds(!callVM.swapScreen.value!!) }
            R.id.iv_camera -> {
                forceCameraMute = !forceCameraMute
                d(TAG, "onClick forceCameraMute $forceCameraMute")
                cameraMute()
            }
            R.id.iv_switch -> { cameraSwitch() }
            R.id.iv_mute -> { mute() }
            R.id.iv_hd -> { changeDefinition() }
            R.id.iv_scale -> { changeScalingType() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == TYPE_PROXIMITY) {
            val distance = event.values[0]
            if ((distance == 0.0f) != isNear) {
                d(TAG, "onSensorChanged distance - $distance")
                isNear = distance == 0.0f
                callVM.speaker(getSystemService(AUDIO_SERVICE) as AudioManager, !isNear)
                if (!forceCameraMute && !isScreen) {
                    d(TAG, "sensor")
                    cameraMute(isNear)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        d(TAG, "onAccuracyChanged")
    }

    private fun activateSensor() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "FireRTC:video-call")
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

    private fun startScreenCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun startCall() {
        val isScreen = callVM.call!!.type == Call.Type.SCREEN
        rtpManager.init(this,
            isAudio = true,
            isVideo = true,
            isScreen = isScreen,
            isDataChannel = false,
            enableStat = false,
            recordAudio = false
        )

        rtpManager.initVideoView(binding.svrFull, binding.svrPip)

        val isOffer = callVM.call!!.direction == Call.Direction.Offer
        rtpManager.startRTP(context = this, data = screenIntent, isOffer = isOffer, remoteSdp = callVM.remoteSDP, remoteICE = callVM.remoteIce)
    }

    private fun end() {
        d(TAG, "end()")
        if (callVM.call!!.connected) {
            callVM.end()
        } else {
            callVM.end(SendFCM.FCMType.Cancel)
        }
    }

    private fun toggleVisibility() {
        if (visibility) {
            visibility = false
            binding.llBtns.visibility = INVISIBLE
            binding.tvName.visibility = INVISIBLE
        } else {
            visibility = true
            binding.llBtns.visibility = VISIBLE
            binding.tvName.visibility = VISIBLE
        }
    }

    private fun cameraMute(value: Boolean = !callVM.cameraMute.value!!) {
        if (callVM.call != null) {
            d(TAG, "cameraMute $value")
            callVM.cameraMute(value)
        }
    }

    private fun cameraSwitch() {
        if (callVM.call!!.type == Call.Type.VIDEO) {
            d(TAG, "cameraSwitch")
            callVM.cameraSwitch()
        }
    }

    private fun mute() {
        d(TAG, "mute")
        callVM.mute()
    }

    private fun changeDefinition() {
        d(TAG, "changeDefinition")
        callVM.changeDefinition()
    }

    private fun changeScalingType() {
        d(TAG, "changeScalingType")
        callVM.changeScalingType()
    }

    private val screenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        screenCode = result.resultCode
        screenIntent = result.data
        startCall()
    }

    companion object {
        private const val TAG = "VideoCallActivity"
    }
}