package kr.young.firertc

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.adapter.AddChatAdapter
import kr.young.firertc.adapter.AddedChatAdapter
import kr.young.firertc.databinding.ActivityAddChatBinding
import kr.young.firertc.model.User
import kr.young.firertc.vm.MessageViewModel
import kr.young.firertc.vm.UserViewModel

class AddChatActivity : AppCompatActivity(), OnClickListener, OnTouchListener {
    private lateinit var binding: ActivityAddChatBinding
    private val checkedContacts = mutableListOf<User>()
    private val contacts = mutableListOf<AddChatAdapter.AddUser>()
    private lateinit var addedChatAdapter: AddedChatAdapter
    private lateinit var addChatAdapter: AddChatAdapter

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_chat)
        binding.tvConfirm.visibility = INVISIBLE

        binding.tvConfirm.setOnClickListener(this)
        binding.tvConfirm.setOnTouchListener(this)

        for (user in UserViewModel.instance.contacts) {
            contacts.add(AddChatAdapter.AddUser(user, false))
        }
        if (contacts.isEmpty()) {
            binding.tvEmpty.visibility = VISIBLE
        } else {
            binding.tvEmpty.visibility = INVISIBLE
        }

        addChatAdapter = AddChatAdapter(contacts)
        addChatAdapter.setOnItemClickListener {
            val contact = contacts[it]
            if (contact.checked) {
                for (i in checkedContacts.indices) {
                    if (checkedContacts[i].id == contact.user.id) {
                        checkedContacts.removeAt(i)
                        addedChatAdapter.notifyItemRemoved(i)
                        break
                    }
                }
                if (checkedContacts.isEmpty()) {
                    binding.tvConfirm.visibility = INVISIBLE
                }
            } else {
                checkedContacts.add(contact.user)
                addedChatAdapter.notifyItemInserted(checkedContacts.size - 1)
                binding.checked.post {
                    d(TAG, "scroll to position - ${checkedContacts.size - 1}")
                    binding.checked.scrollToPosition(checkedContacts.size - 1)
                }
                if (checkedContacts.isNotEmpty()) {
                    binding.tvConfirm.visibility = VISIBLE
                }
            }
            contacts[it].checked = !contacts[it].checked
            addChatAdapter.notifyItemChanged(it)
        }
        binding.recyclerView.adapter = addChatAdapter

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        binding.recyclerView.layoutManager = layoutManager

        addedChatAdapter = AddedChatAdapter(checkedContacts)
        addedChatAdapter.setOnItemClickListener {
            for (i in contacts.indices) {
                if (contacts[i].user.id == checkedContacts[it].id) {
                    contacts[i].checked = !contacts[i].checked
                    addChatAdapter.notifyItemChanged(i)
                    break
                }
            }
            checkedContacts.removeAt(it)
            addedChatAdapter.notifyItemRemoved(it)
            if (checkedContacts.isEmpty()) {
                binding.tvConfirm.visibility = INVISIBLE
            }
        }
        binding.checked.adapter = addedChatAdapter

        val checkedLayoutManager = LinearLayoutManager(this)
        checkedLayoutManager.orientation = RecyclerView.HORIZONTAL
        binding.checked.layoutManager = checkedLayoutManager
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_confirm -> { createChat() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return false
    }

    private fun createChat() {
        if (checkedContacts.size == 1) {
            UserViewModel.instance.readUser(checkedContacts[0].id) {
                val user = it.toObject<User>()
                if (user != null) {
                    MessageViewModel.instance.startOffer(user) {
                        val intent = Intent(this, MessageActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "No User", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Next...", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "AddChatActivity"
    }
}