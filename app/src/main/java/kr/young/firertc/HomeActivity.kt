package kr.young.firertc

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kr.young.common.TouchEffect
import kr.young.common.UtilLog
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.databinding.ActivityHomeBinding
import kr.young.firertc.fragment.*
import kr.young.firertc.repo.AppSP
import kr.young.firertc.util.BaseActivity
import kr.young.firertc.util.Config.Companion.CHAT_ID
import kr.young.firertc.vm.*

class HomeActivity : BaseActivity(), OnClickListener, OnTouchListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var myViewModel: MyDataViewModel

    private var currentFragment = AppSP.instance.getFragmentIndex()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        myViewModel = MyDataViewModel.instance

        replaceFragment(currentFragment)
        switchIcon(currentFragment)

        binding.ivMenu.setOnTouchListener(this)
        binding.ivMenu.setOnClickListener(this)
        binding.rlContact.setOnClickListener(this)
        binding.rlChat.setOnClickListener(this)
        binding.rlConference.setOnClickListener(this)
        binding.rlHistory.setOnClickListener(this)
        binding.rlSetting.setOnClickListener(this)

        myViewModel.isSigned.observe(this) {
            if (!it) {
                val intent = Intent(this, SignActivity::class.java)
                intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
                signLauncher.launch(intent)
            }
        }

        myViewModel.myData.observe(this) {
            it?.let {
                binding.tvTitle.text = it.name
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                UtilLog.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
//            val msg = getString(R.string.msg_token_fmt, token)
            if (token != AppSP.instance.getFCMToken()) {
                AppSP.instance.setFCMToken(token)
                myViewModel.updateFCMToken(token)
            }
            d(TAG, "token $token")
        })

//        Firebase.messaging.subscribeToTopic("topic")
//            .addOnCompleteListener { task ->
//                var msg = "Subscribed"
//                if (!task.isSuccessful) {
//                    msg = "Subscribe failed"
//                }
//                d(TAG, msg)
//            }

        if (AppSP.instance.isSigned()) {

            if (currentFragment != CONTACT) {
                UserViewModel.instance.getContacts()
            }
        }

        notificationEvent(intent.getStringExtra(CHAT_ID))
    }

    override fun onResume() {
        super.onResume()

        if (AppSP.instance.isSigned()) {
            if (intent == null || intent.getStringExtra(CHAT_ID) == null) {
                myViewModel.getMyData()
                when (currentFragment) {
                    CONTACT -> UserViewModel.instance.getContacts()
                    CHAT -> ChatViewModel.instance.getChats()
                    CONFERENCE -> {}
                    HISTORY -> HistoryVM.getInstance()!!.getHistory()
                }
            }
        } else {
            val intent = Intent(this, SignActivity::class.java)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            signLauncher.launch(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        notificationEvent(intent!!.getStringExtra(CHAT_ID))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_menu -> {
                when (currentFragment) {
                    CONTACT -> { startActivity(Intent(this, AddContactActivity::class.java)) }
                }
            }
            R.id.rl_contact -> {
                replaceFragment(CONTACT)
                switchIcon(CONTACT)
                UserViewModel.instance.getContacts()
            }
            R.id.rl_chat -> {
                replaceFragment(CHAT)
                switchIcon(CHAT)
                ChatViewModel.instance.getChats()
            }
            R.id.rl_conference -> {
                replaceFragment(CONFERENCE)
                switchIcon(CONFERENCE)
            }
            R.id.rl_history -> {
                replaceFragment(HISTORY)
                switchIcon(HISTORY)
                HistoryVM.getInstance()!!.getHistory()
            }
            R.id.rl_setting -> {
                replaceFragment(SETTING)
                switchIcon(SETTING)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v?.id) {
            R.id.iv_menu -> { TouchEffect.iv(v, event) }
        }
        return super.onTouchEvent(event)
    }

    private fun notificationEvent(chatId: String?) {
        chatId?.let {
            MessageVM.instance.chatId = chatId
            val intent = Intent(this, MessageActivity::class.java)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun replaceFragment(fragmentNumber: Int) {
        AppSP.instance.setFragmentIndex(fragmentNumber)
        val fragment = when (fragmentNumber) {
            CHAT -> { ChatFragment() }
            CONFERENCE -> { ConferenceFragment() }
            HISTORY -> { HistoryFragment() }
            SETTING -> { SettingFragment() }
            else -> { ContactFragment() }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment, fragment)
            .commitAllowingStateLoss()
    }

    private fun switchIcon(fragment: Int) {
        when (fragment) {
            CONTACT -> {
                binding.ivContact.setColorFilter(getColor(R.color.black))
                binding.ivChat.setColorFilter(getColor(R.color.white))
                binding.ivConference.setColorFilter(getColor(R.color.white))
                binding.ivHistory.setColorFilter(getColor(R.color.white))
                binding.ivSetting.setColorFilter(getColor(R.color.white))

                binding.ivMenu.setImageResource(R.drawable.outline_person_add_24)
                binding.ivMenu.visibility = VISIBLE
            }
            CHAT -> {
                binding.ivContact.setColorFilter(getColor(R.color.white))
                binding.ivChat.setColorFilter(getColor(R.color.black))
                binding.ivConference.setColorFilter(getColor(R.color.white))
                binding.ivHistory.setColorFilter(getColor(R.color.white))
                binding.ivSetting.setColorFilter(getColor(R.color.white))

                binding.ivMenu.setImageResource(R.drawable.round_add_24)
                binding.ivMenu.visibility = GONE
            }
            CONFERENCE -> {
                binding.ivContact.setColorFilter(getColor(R.color.white))
                binding.ivChat.setColorFilter(getColor(R.color.white))
                binding.ivConference.setColorFilter(getColor(R.color.black))
                binding.ivHistory.setColorFilter(getColor(R.color.white))
                binding.ivSetting.setColorFilter(getColor(R.color.white))

                binding.ivMenu.visibility = GONE
            }
            HISTORY -> {
                binding.ivContact.setColorFilter(getColor(R.color.white))
                binding.ivChat.setColorFilter(getColor(R.color.white))
                binding.ivConference.setColorFilter(getColor(R.color.white))
                binding.ivHistory.setColorFilter(getColor(R.color.black))
                binding.ivSetting.setColorFilter(getColor(R.color.white))

                binding.ivMenu.visibility = GONE
            }
            SETTING -> {
                binding.ivContact.setColorFilter(getColor(R.color.white))
                binding.ivChat.setColorFilter(getColor(R.color.white))
                binding.ivConference.setColorFilter(getColor(R.color.white))
                binding.ivHistory.setColorFilter(getColor(R.color.white))
                binding.ivSetting.setColorFilter(getColor(R.color.black))

                binding.ivMenu.visibility = GONE
            }
        }
    }

    private val signLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            d(TAG, "result ok")
        } else if (result.resultCode == RESULT_CANCELED) {
            d(TAG, "result canceled")
            finish()
        }
    }

    companion object {
        private const val TAG = "HomeActivity"
        private const val CONTACT = 0
        private const val CHAT = 1
        private const val CONFERENCE = 2
        private const val HISTORY = 3
        private const val SETTING = 4
    }
}