package kr.young.examplewebrtc

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.databinding.ActivityCallBinding
import kr.young.examplewebrtc.model.Space
import kr.young.examplewebrtc.vm.CallViewModel
import java.text.SimpleDateFormat
import java.util.*

class CallActivity : AppCompatActivity(), OnClickListener, OnTouchListener {
    private lateinit var binding: ActivityCallBinding
    private lateinit var callViewModel: CallViewModel

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)

        binding.tvEnd.setOnClickListener(this)
        binding.tvEnd.setOnTouchListener(this)

        callViewModel = CallViewModel.instance
        binding.space = callViewModel.space.value!!

        callViewModel.space.observe(this) {
            d(TAG, "space.observe $it")
            binding.tvCount.text = "${it.calls.size}"
        }
        callViewModel.myCall.observe(this) {
            d(TAG, "myCall.observe $it")
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val date = inputFormat.parse(it.createdAt)
            d(TAG, "parse date = ${outputFormat.format(date!!)}")
        }
        callViewModel.remoteCall.observe(this) {
            if (it != null) {
                d(TAG, "call.observe.size ${it.size}")
                for (call in it) {
                    d(TAG, "call.observe $call")
                }
            }
        }

        if (callViewModel.space.value!!.calls.isEmpty()) {
            //make call
            callViewModel.makeCall()
        } else {
            //getRemoteCall
            callViewModel.updateSpaceStatus(Space.SpaceStatus.ACTIVE)
            callViewModel.answerCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callViewModel.terminateSpace()
        callViewModel.endCall()
    }

    private fun end() {
        finish()
    }

    companion object {
        private const val TAG = "CallActivity"
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_end -> { end() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_end -> { TouchEffect.alpha(v, event) }
        }
        return super.onTouchEvent(event)
    }
}