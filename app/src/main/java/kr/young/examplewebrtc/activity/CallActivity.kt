package kr.young.examplewebrtc.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.CallService
import kr.young.examplewebrtc.R
import kr.young.examplewebrtc.databinding.ActivityCallBinding
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.fcm.SendFCM.FCMType
import kr.young.examplewebrtc.model.Space.SpaceStatus
import kr.young.examplewebrtc.util.BaseActivity
import kr.young.examplewebrtc.vm.CallViewModel
import kr.young.examplewebrtc.vm.MyDataViewModel
import kr.young.examplewebrtc.vm.SpaceViewModel
import kr.young.examplewebrtc.vm.UserViewModel
import kr.young.rtp.RTPManager
import kr.young.rtp.observer.PCObserver
import kr.young.rtp.observer.PCObserverImpl
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import java.util.*

class CallActivity : BaseActivity(), OnClickListener, OnTouchListener, PCObserver.SDP, PCObserver.ICE, PCObserver {
    private lateinit var binding: ActivityCallBinding
    private lateinit var spaceViewModel: SpaceViewModel
    private lateinit var callViewModel: CallViewModel

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)

        binding.tvEnd.setOnClickListener(this)
        binding.tvEnd.setOnTouchListener(this)
        binding.tvMute.setOnClickListener(this)
        binding.tvMute.setOnTouchListener(this)
        binding.tvSpeaker.setOnClickListener(this)
        binding.tvSpeaker.setOnTouchListener(this)

        spaceViewModel = SpaceViewModel.instance
        callViewModel = CallViewModel.instance

        PCObserverImpl.instance.add(this as PCObserver)
        PCObserverImpl.instance.add(this as PCObserver.SDP)
        PCObserverImpl.instance.add(this as PCObserver.ICE)

        spaceViewModel.responseCode.observe(this) {
            if (it != null) {
                d(TAG, "responseCode $it")
            }
        }
        spaceViewModel.isOffer.observe(this) {
            if (it != null) {
                if (it) {
                    spaceViewModel.makeOffer()
                    RTPManager.instance.startRTP(context = this, isOffer = true)
                } else {
                    spaceViewModel.updateSpaceStatus(SpaceStatus.ACTIVE)
                    val sdp = spaceViewModel.makeAnswer()
                    d(TAG, "remote SDP\n${sdp}")
                    val remoteSDP = SessionDescription(SessionDescription.Type.OFFER, sdp)
                    RTPManager.instance.startRTP(context = this, isOffer = false, remoteSdp = remoteSDP)
                }
                spaceViewModel.isOffer.value = null
            }
        }
        spaceViewModel.space.observe(this) {
            if (it != null) {
                d(TAG, "space.observe name ${it.name} id ${it.id.substring(0, 5)}")
                binding.space = it
            }
        }
        spaceViewModel.calls.observe(this) {
            d(TAG, "calls.observe")
            var count = 0
            val userList = mutableListOf<String>()
            for (call in it) {
                d(TAG, "calls.observe call id ${call.id.substring(0, 5)} userId ${call.userId}")
                if (!call.terminated) {
                    if (call.userId != MyDataViewModel.instance.getMyId()) {
                        userList.add(call.userId!!)
                    }
                    count++
                }
            }
            UserViewModel.instance.readUsers(userList)
            binding.tvCount.text = "$count"
        }
        spaceViewModel.newSdp.observe(this) {
            if (it != null) {
                d(TAG, "newSdp.observe")
                val remote = SessionDescription(SessionDescription.Type.OFFER, it)
                RTPManager.instance.setRemoteDescription(remote)
            }
        }
        spaceViewModel.newIce.observe(this) {
            if (it != null) {
                d(TAG, "newIce.observe")
                val remote = IceCandidate("0", 0, it)
                RTPManager.instance.addRemoteIceCandidate(remote)
            }
        }
        spaceViewModel.participants.observe(this) {
            if (it != null) {
                d(TAG, "participants.observe $it")
            }
        }
        callViewModel.myCall.observe(this) {
            if (it != null) {
                d(TAG, "myCall.observe call id ${it.id.substring(0, 5)} userId ${it.userId}")
            }
        }
        callViewModel.mute.observe(this) {
            if (it != null) {
                if (it) {
                    binding.tvMute.text = getString(R.string.mute_off)
                } else {
                    binding.tvMute.text = getString(R.string.mute_on)
                }
            }
        }
        callViewModel.speaker.observe(this) {
            if (it != null) {
                if (it) {
                    binding.tvSpeaker.text = getString(R.string.speaker_off)
                } else {
                    binding.tvSpeaker.text = getString(R.string.speaker_on)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PCObserverImpl.instance.remove(this as PCObserver)
        PCObserverImpl.instance.remove(this as PCObserver.SDP)
        PCObserverImpl.instance.remove(this as PCObserver.ICE)
        endCall()
    }

    private fun endCall() {
        //update call to terminated
        d(TAG, "endCall")
        stopService(Intent(this, CallService::class.java))
        callViewModel.endCall()
        var isTerminatedSpace = true
        for (call in spaceViewModel.getCalls()) {
            d(TAG, "endCall ${call.userId} terminated ${call.terminated}")
            if (!call.terminated && call.userId != MyDataViewModel.instance.getMyId()) {
                isTerminatedSpace = false
            }
            if (call.userId != MyDataViewModel.instance.getMyId()) {
                SendFCM.sendMessage(
                    to = call.fcmToken!!,
                    type = FCMType.Leave,
                    spaceId = call.spaceId,
                    callId = call.id
                )
            }
        }
        if (isTerminatedSpace) {
            spaceViewModel.updateSpaceStatus(SpaceStatus.TERMINATED)
        } else {
            spaceViewModel.updateSpaceStatus(SpaceStatus.INACTIVE)
        }
    }

    companion object {
        private const val TAG = "CallActivity"
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_end -> { finish() }
            R.id.tv_mute -> { mute() }
            R.id.tv_speaker -> { speaker() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_end, R.id.tv_speaker, R.id.tv_mute -> { TouchEffect.alpha(v, event) }
        }
        return super.onTouchEvent(event)
    }

    private fun mute() {
        RTPManager.instance.setMute(!callViewModel.getMute())
        RTPManager.instance.setAudioEnable(callViewModel.getMute())
        callViewModel.setMute()
    }

    private fun speaker() {
        RTPManager.instance.setSpeaker(!callViewModel.getSpeaker())
        callViewModel.setSpeaker()
    }

    override fun onLocalDescription(sdp: SessionDescription?) {
        d(TAG, "onLocalDescription")
        for (call in spaceViewModel.getCalls()) {
            if (!call.terminated && call.userId != MyDataViewModel.instance.getMyId()) {
                SendFCM.sendMessage(
                    to = call.fcmToken!!,
                    type = FCMType.Sdp,
                    spaceId = call.spaceId,
                    callId = call.id,
                    sdp = sdp!!.description
                )
            }
        }
        runOnUiThread { CallViewModel.instance.updateSDP(sdp!!.description) }
    }

    override fun onICECandidate(candidate: IceCandidate?) {
        d(TAG, "onICECandidate")
        for (call in spaceViewModel.getCalls()) {
            if (!call.terminated && call.userId != MyDataViewModel.instance.getMyId()) {
                SendFCM.sendMessage(
                    to = call.fcmToken!!,
                    type = FCMType.Ice,
                    spaceId = call.spaceId,
                    callId = call.id,
                    sdp = candidate!!.sdp
                )
            }
        }
        runOnUiThread { CallViewModel.instance.updateCandidate(candidate!!.sdp) }
    }

    override fun onICECandidatesRemoved(candidates: Array<out IceCandidate?>?) {
        d(TAG, "onICECandidatesRemoved")
    }

    override fun onICEConnected() {
        d(TAG, "onICEConnected")
    }

    override fun onICEDisconnected() {
        d(TAG, "onICEDisconnected")
    }

    override fun onPCConnected() {
        d(TAG, "onPCConnected")
    }

    override fun onPCDisconnected() {
        d(TAG, "onPCDisconnected")
    }

    override fun onPCFailed() {
        d(TAG, "onPCFailed")
    }

    override fun onPCClosed() {
        d(TAG, "onPCClosed")
    }

    override fun onPCStatsReady(reports: Array<StatsReport?>?) {
        d(TAG, "onPCStatsReady")
    }

    override fun onPCError(description: String?) {
        d(TAG, "onPCError")
    }

    override fun onMessage(message: String) {
        d(TAG, "onMessage")
    }
}