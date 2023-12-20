package kr.young.firertc.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.widget.EditText
import android.widget.TextView
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.CallActivity
import kr.young.firertc.CallService
import kr.young.firertc.R
import kr.young.firertc.vm.CallViewModel
import kr.young.firertc.vm.SpaceViewModel

class ConferenceFragment : Fragment() {
    private lateinit var etName: EditText
    private lateinit var tvWarn: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_conference, container, false)
        etName = layout.findViewById(R.id.et_name)
        tvWarn = layout.findViewById(R.id.tv_warn)

        etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == IME_ACTION_DONE) {
                join()
                true
            } else {
                false
            }
        }

        etName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

            override fun afterTextChanged(s: Editable?) { }

        })

        return layout
    }

    private fun join() {
        if (etName.text.toString().isNotEmpty()) {
            SpaceViewModel.instance.release()
            CallViewModel.instance.release()
            SpaceViewModel.instance.joinSpace(etName.text.toString())
            etName.setText("")
            val intent = Intent(requireContext(), CallActivity::class.java)
            startActivity(intent)
            requireContext().startService(Intent(context, CallService::class.java))
        } else {
            tvWarn.text = String.format(getString(R.string.empty_warn), "Name")
        }
    }

    companion object {
        private const val TAG = "ConferenceFragment"
    }
}