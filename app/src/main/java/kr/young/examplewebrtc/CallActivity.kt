package kr.young.examplewebrtc

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.databinding.ActivityCallBinding
import kr.young.examplewebrtc.fcm.SendFCM
import kr.young.examplewebrtc.fcm.SendFCM.FCMType
import kr.young.examplewebrtc.model.Space.SpaceStatus
import kr.young.examplewebrtc.util.BaseActivity
import kr.young.examplewebrtc.vm.CallViewModel
import kr.young.examplewebrtc.vm.MyDataViewModel
import kr.young.examplewebrtc.vm.SpaceViewModel
import kr.young.examplewebrtc.vm.UserViewModel
import kr.young.rtp.observer.PCObserver
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

        spaceViewModel = SpaceViewModel.instance
        callViewModel = CallViewModel.instance

        spaceViewModel.isOffer.observe(this) {
            if (it != null) {
                if (it) {
                    spaceViewModel.makeOffer()
                } else {
                    spaceViewModel.updateSpaceStatus(SpaceStatus.ACTIVE)
                    spaceViewModel.makeAnswer()
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
            for (call in it) {
                d(TAG, "calls.observe call id ${call.id.substring(0, 5)} userId ${call.userId}")
                if (!call.terminated) {
                    if (call.userId != MyDataViewModel.instance.getMyId()) {
                        UserViewModel.instance.get(call.userId!!)
                    }
                    count++
                }
            }
            binding.tvCount.text = "$count"
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
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }

    private fun endCall() {
        //update call to terminated
        d(TAG, "endCall")
        callViewModel.endCall()
        var isTerminatedSpace = true
        for (call in spaceViewModel.calls.value!!) {
            d(TAG, "endCall ${call.userId} terminated ${call.terminated}")
            if (!call.terminated) {
                isTerminatedSpace = false
            }
            if (call.userId != MyDataViewModel.instance.getMyId()) {
                SendFCM.sendMessage(
                    to = call.token!!,
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
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_end -> { TouchEffect.alpha(v, event) }
        }
        return super.onTouchEvent(event)
    }

    override fun onLocalDescription(sdp: SessionDescription?) {
        d(TAG, "onLocalDescription")
    }

    override fun onICECandidate(candidate: IceCandidate?) {
        d(TAG, "onICECandidate")
    }

    override fun onICECandidatesRemoved(candidates: Array<out IceCandidate>?) {
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