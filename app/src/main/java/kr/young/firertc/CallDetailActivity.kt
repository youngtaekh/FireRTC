package kr.young.firertc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import androidx.databinding.DataBindingUtil
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.DateUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.databinding.ActivityCallDetailBinding
import kr.young.firertc.model.Call
import kr.young.firertc.model.Space
import kr.young.firertc.model.User
import kr.young.firertc.util.BaseActivity
import kr.young.firertc.vm.*
import java.util.*

class CallDetailActivity : BaseActivity(), OnClickListener, OnTouchListener {
    private lateinit var binding: ActivityCallDetailBinding
    private val callVM = CallVM.instance
    private val call = callVM.selectedCall
    var space: Space? = null
    var counterpartId: String? = null
    var counterpart: User? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call_detail)

        binding.tvTitle.text = call!!.counterpartName
        when (call.type) {
            Call.Type.VIDEO -> {
                binding.ivCategory.setImageResource(R.drawable.round_videocam_24)
                binding.tvCategory.text = Call.Type.VIDEO.toString()
            } Call.Type.SCREEN -> {
                binding.ivCategory.setImageResource(R.drawable.round_mobile_screen_share_24)
                binding.tvCategory.text = Call.Type.SCREEN.toString()
            } Call.Type.MESSAGE -> {
                binding.ivCategory.setImageResource(R.drawable.round_chat_bubble_24)
                binding.tvCategory.text = Call.Type.MESSAGE.toString()
            } Call.Type.CONFERENCE -> {
                binding.ivCategory.setImageResource(R.drawable.round_meeting_room_24)
                binding.tvCategory.text = Call.Type.CONFERENCE.toString()
            } else -> {
                binding.ivCategory.setImageResource(R.drawable.round_call_24)
                binding.tvCategory.text = Call.Type.AUDIO.toString()
            }
        }
        if (call.direction == Call.Direction.Offer) {
            binding.ivDirection.setImageResource(R.drawable.round_call_made_24)
            binding.tvDirection.text = Call.Direction.Offer.toString()
        } else {
            binding.ivDirection.setImageResource(R.drawable.round_call_received_24)
            binding.tvDirection.text = Call.Direction.Answer.toString()
        }
        binding.tvDate.text = DateUtil.toFormattedString(call.createdAt!!, "yyyy-MM-dd hh:mm:ss")
        if (call.connected) {
            val sec = ((call.terminatedAt!!.time - call.createdAt.time) / 1000).toInt()
            val time = if (sec > 3600) {
                "${sec/3600}h ${(sec%3600)/60}m ${(sec%3600)%60}s"
            } else if (sec > 60) {
                "${sec/60}m ${sec%60}s"
            } else {
                "${sec}s"
            }
            binding.tvTime.text = time
        }

        binding.ivClose.setOnClickListener(this)
        binding.ivClose.setOnTouchListener(this)
        binding.ivCall.setOnClickListener(this)
        binding.ivCall.setOnTouchListener(this)
        binding.ivChat.setOnClickListener(this)
        binding.ivChat.setOnTouchListener(this)
        binding.ivVideo.setOnClickListener(this)
        binding.ivVideo.setOnTouchListener(this)
        binding.ivScreen.setOnClickListener(this)
        binding.ivScreen.setOnTouchListener(this)

        callVM.getSpace(this.call.spaceId!!) {
            d(TAG, "get space success")
            this.space = it.toObject()
            d(TAG, "call $call")
            d(TAG, "space $space")
            if (!call.connected) {
                binding.tvTime.text = space!!.terminatedReason
            }
            if (space!!.callType == Call.Type.CONFERENCE) {
                binding.ivCall.visibility = INVISIBLE
                binding.ivVideo.visibility = INVISIBLE
                binding.ivChat.visibility = INVISIBLE
            } else {
                if (space!!.participants.size >= 2) {
                    binding.ivCall.visibility = VISIBLE
                    binding.ivVideo.visibility = VISIBLE
                    binding.ivChat.visibility = VISIBLE
                    for (id in space!!.participants) {
                        if (id != MyDataViewModel.instance.getMyId()) {
                            counterpartId = id
                        }
                    }
                    if (counterpartId != null) {
                        UserViewModel.instance.readUser(counterpartId!!) { user ->
                            counterpart = user.toObject()
                            d(TAG, "read user success $counterpart")
                        }
                    }
                } else {
                    binding.ivCall.visibility = INVISIBLE
                    binding.ivVideo.visibility = INVISIBLE
                    binding.ivChat.visibility = INVISIBLE
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_close -> { finish() }
            R.id.iv_call -> { call() }
            R.id.iv_chat -> { chat() }
            R.id.iv_video -> { video() }
            R.id.iv_screen -> { screen() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return false
    }

    private fun call() {
        d(TAG, "audio call")
        if (counterpart != null) {
            val audioVM = AudioViewModel.instance
            audioVM.startOffer(counterpart = counterpart!!, type = Call.Type.AUDIO) {
                audioVM.updateCallList()
                audioVM.updateParticipantList()
                val intent = Intent(this, AudioCallActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                startForegroundService(Intent(this, CallService::class.java))
            }
        }
    }

    private fun chat() {
        d(TAG, "chat()")
    }

    private fun video() {
        d(TAG, "video call")
        if (counterpart != null) {
            val videoVM = VideoViewModel.instance
            videoVM.startOffer(counterpart!!, Call.Type.VIDEO) {
                videoVM.updateCallList()
                videoVM.updateParticipantList()
                startForegroundService(Intent(this, CallService::class.java))
                val intent = Intent(this, VideoCallActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
    }

    private fun screen() {
        d(TAG, "screen call")
        if (counterpart != null) {
            val videoVM = VideoViewModel.instance
            videoVM.startOffer(counterpart!!, Call.Type.SCREEN) {
                videoVM.updateCallList()
                videoVM.updateParticipantList()
                startForegroundService(Intent(this, CallService::class.java))
                val intent = Intent(this, VideoCallActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val TAG = "CallDetailActivity"
    }
}