package kr.young.examplewebrtc.activity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import androidx.databinding.DataBindingUtil
import kr.young.common.TouchEffect
import kr.young.examplewebrtc.R
import kr.young.examplewebrtc.databinding.ActivityAddContactBinding
import kr.young.examplewebrtc.repo.RelationRepository.Companion.RELATION_CREATE_SUCCESS
import kr.young.examplewebrtc.util.ResponseCode.Companion.NO_USER
import kr.young.examplewebrtc.vm.MyDataViewModel
import kr.young.examplewebrtc.vm.UserViewModel

class AddContactActivity : AppCompatActivity(), OnClickListener, OnTouchListener {

    private lateinit var binding: ActivityAddContactBinding
    private val userViewModel = UserViewModel.instance

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_contact)

        binding.tvFind.setOnTouchListener(this)
        binding.tvFind.setOnClickListener(this)
        binding.tvAdd.setOnTouchListener(this)
        binding.tvAdd.setOnClickListener(this)

        userViewModel.responseCode.observe(this) {
            when (it) {
                NO_USER -> {
                    binding.tvName.visibility = INVISIBLE
                    binding.tvAdd.visibility = INVISIBLE
                    binding.tvWarning.text = getString(R.string.no_user_warn)
                }
                RELATION_CREATE_SUCCESS -> {
                    binding.tvAdd.visibility = INVISIBLE
                }
            }
        }

        userViewModel.foundUser.observe(this) {
            if (it != null) {
                var already = false
                for (contact in userViewModel.contacts) {
                    if (contact.id == it.id) {
                        already = true
                        break
                    }
                }
                if (already) {
                    binding.tvWarning.text = getString(R.string.already_warn)
                } else {
                    binding.tvName.visibility = VISIBLE
                    binding.tvAdd.visibility = VISIBLE
                    binding.tvName.text = it.name
                }
            } else {
                binding.tvName.visibility = INVISIBLE
                binding.tvAdd.visibility = INVISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userViewModel.setFoundUser(null)
    }

    private fun findUser() {
        val userId = binding.etId.text.toString().lowercase()
        if (userId.isEmpty()) {
            binding.tvWarning.text = String.format(getString(R.string.empty_warn), "ID")
        } else if (userId == MyDataViewModel.instance.getMyId()) {
            binding.tvWarning.text = getString(R.string.mine_warn)
        } else {
            userViewModel.readUser(userId)
        }
    }

    private fun addUser() {
        userViewModel.createRelation(userViewModel.foundUser.value!!.id)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_find -> { findUser() }
            R.id.tv_add -> { addUser() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v?.id) {
            R.id.tv_find -> { TouchEffect.alpha(v, event) }
            R.id.tv_add -> { TouchEffect.alpha(v, event) }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val TAG = "AddContactActivity"
    }
}