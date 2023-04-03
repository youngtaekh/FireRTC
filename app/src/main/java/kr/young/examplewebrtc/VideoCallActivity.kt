package kr.young.examplewebrtc

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.examplewebrtc.databinding.ActivityVideoCallBinding

class VideoCallActivity : AppCompatActivity(), OnClickListener, OnTouchListener {
    private lateinit var binding: ActivityVideoCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_call)
    }

    override fun onClick(v: View?) {
        when (v?.id) {

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return false
    }
}