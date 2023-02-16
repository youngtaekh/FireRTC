package kr.young.examplewebrtc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.i
import kr.young.examplewebrtc.databinding.ActivityMainBinding
import kr.young.examplewebrtc.vm.CallViewModel

class MainActivity : AppCompatActivity(), OnTouchListener, OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var callViewModel: CallViewModel

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        i(TAG, "onCreate()")

        binding.tvJoin.setOnTouchListener(this)
        binding.tvJoin.setOnClickListener(this)

        binding.etName.setOnEditorActionListener { v, actionId, event ->
            if (actionId == IME_ACTION_DONE) {
                d(TAG, "DONE")
                join()
                true
            } else {
                false
            }
        }

        callViewModel = CallViewModel.instance
        callViewModel.hasExistSpace.observe(this) {
            d(TAG, "hasExistSpace.observe - $it")
            if (it!!) {
                binding.etName.setText("")
                val intent = Intent(this, CallActivity::class.java)
                startActivity(intent)
            } else {
                callViewModel.createSpace(binding.etName.text.toString())
            }
        }

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                i(TAG, "result ok")
            } else if (result.resultCode == RESULT_CANCELED) {
                i(TAG, "result canceled")
                finish()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_join -> TouchEffect.alpha(v, event)
        }
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_join -> join()
        }
    }

    private fun join() {
        d(TAG, "join")
        if (binding.etName.text.toString().isNotEmpty()) {
            callViewModel.checkExistSpace(binding.etName.text.toString())
        } else {
            binding.tvWarn.text = getString(R.string.empty_warn)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}