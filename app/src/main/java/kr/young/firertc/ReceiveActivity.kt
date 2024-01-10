package kr.young.firertc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.databinding.ActivityReceiveBinding
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call.Type.*
import kr.young.firertc.vm.CallVM
import kr.young.firertc.vm.UserViewModel

class ReceiveActivity : AppCompatActivity(), OnClickListener, OnTouchListener {
    private lateinit var binding: ActivityReceiveBinding
    private lateinit var vm: CallVM

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_receive)
        binding.user = CallVM.instance.counterpart

        d(TAG, "call type ${CallVM.instance.callType}")
        vm = CallVM.instance

        vm.terminatedCall.observe(this) {
            if (it != null && it) {
                finish()
            }
        }

        binding.ivAnswer.setImageResource(when (vm.call!!.type) {
            VIDEO -> R.drawable.round_videocam_24
            SCREEN -> R.drawable.round_mobile_screen_share_24
            else -> R.drawable.round_call_24
        })

        binding.ivProfile.setImageResource(UserViewModel.instance.selectImage(""))
        binding.ivAnswer.setOnClickListener(this)
        binding.ivAnswer.setOnTouchListener(this)
        binding.ivEnd.setOnClickListener(this)
        binding.ivEnd.setOnTouchListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_answer -> { answer() }
            R.id.iv_end -> { end() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun answer() {
        d(TAG, "answer")
        finish()
        when (vm.call!!.type) {
            AUDIO -> {
                startActivity(Intent(this, AudioCallActivity::class.java))
            }
            else -> {
                startActivity(Intent(this, VideoCallActivity::class.java))
            }
        }
    }

    private fun end() {
        d(TAG, "end")
        vm.end(SendFCM.FCMType.Decline)
    }

    companion object {
        private const val TAG = "ReceiveActivity"
    }
}