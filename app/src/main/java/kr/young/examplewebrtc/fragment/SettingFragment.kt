package kr.young.examplewebrtc.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.RelativeLayout
import kr.young.common.TouchEffect
import kr.young.examplewebrtc.R
import kr.young.examplewebrtc.vm.MyDataViewModel

class SettingFragment : Fragment(), OnTouchListener, OnClickListener {

    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_setting, container, false)
        val signOut = layout.findViewById<RelativeLayout>(R.id.sign_out)
        signOut.setOnClickListener(this)
        signOut.setOnTouchListener(this)
        // Inflate the layout for this fragment
        return layout
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when(v?.id) {
            R.id.sign_out -> {
                TouchEffect.background(
                    v, event,
                    requireContext().getColor(R.color.background_gray),
                    requireContext().getColor(R.color.white))
            }
        }
        return false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.sign_out -> { MyDataViewModel.instance.signOut() }
        }
    }

    companion object {
        private const val TAG = "SettingFragment"
    }
}