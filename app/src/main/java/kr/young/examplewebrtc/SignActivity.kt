package kr.young.examplewebrtc

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import kr.young.examplewebrtc.databinding.ActivitySignBinding
import kr.young.examplewebrtc.util.BaseActivity

class SignActivity : BaseActivity(), OnTouchListener, OnClickListener {
    private lateinit var binding: ActivitySignBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {

        }
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {

        }
    }
}