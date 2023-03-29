package kr.young.examplewebrtc

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kr.young.common.TouchEffect
import kr.young.examplewebrtc.databinding.ActivitySignBinding
import kr.young.examplewebrtc.repo.AppSP
import kr.young.examplewebrtc.util.BaseActivity
import kr.young.examplewebrtc.util.ResponseCode.Companion.OK
import kr.young.examplewebrtc.util.ResponseCode.Companion.WRONG_PASSWORD
import kr.young.examplewebrtc.util.ResponseCode.Companion.WRONG_PASSWORD_TEXT
import kr.young.examplewebrtc.vm.MyDataViewModel

class SignActivity : BaseActivity(), OnTouchListener, OnClickListener {
    private lateinit var binding: ActivitySignBinding
    private lateinit var vm: MyDataViewModel

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign)
        vm = MyDataViewModel.instance

        binding.tvStart.setOnTouchListener(this)
        binding.tvStart.setOnClickListener(this)

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                start()
            }
            false
        }

        if (!AppSP.instance.getUserId().isNullOrEmpty()) {
            binding.etId.setText(AppSP.instance.getUserId())
        }

        vm.responseCode.observe(this, codeObserver)
        vm.isSigned.observe(this, signedObserver)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_start -> { TouchEffect.alpha(v, event) }
        }
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_start -> { start() }
        }
    }

    private fun start() {
        vm.checkMyData(binding.etId.text.toString(), binding.etPassword.text.toString())
    }

    private val codeObserver = Observer<Int> {
        when (it) {
            WRONG_PASSWORD -> {binding.tvWarning.text = WRONG_PASSWORD_TEXT}
        }
    }

    private val signedObserver = Observer<Boolean> {
        if (it) {
            setResult(OK)
            finish()
        }
    }

    companion object {
        private const val TAG = "SignActivity"
    }
}