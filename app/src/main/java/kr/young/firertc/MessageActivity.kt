package kr.young.firertc

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.young.common.DateUtil
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.adapter.MessageAdapter
import kr.young.firertc.databinding.ActivityMessageBinding
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call
import kr.young.firertc.model.Message
import kr.young.firertc.vm.MessageViewModel
import kr.young.firertc.vm.MyDataViewModel
import kr.young.rtp.RTPManager
import kr.young.rtp.observer.PCObserver
import kr.young.rtp.observer.PCObserverImpl
import org.webrtc.StatsReport
import java.lang.System.currentTimeMillis
import java.util.*

class MessageActivity : AppCompatActivity(), OnTouchListener, OnClickListener, PCObserver {
    private lateinit var binding: ActivityMessageBinding
    private val viewModel = MessageViewModel.instance
    private val rtpManager = RTPManager.instance

    private val counterpart = viewModel.counterpart!!

    private val messageList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    private var rtpConnected = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message)

        messageAdapter = MessageAdapter(messageList)
        binding.recyclerView.adapter = messageAdapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        binding.recyclerView.layoutManager = layoutManager

        binding.ivBack.setOnTouchListener(this)
        binding.ivBack.setOnClickListener(this)
        binding.ivSend.setOnTouchListener(this)
        binding.ivSend.setOnClickListener(this)

        binding.etMessage.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                d(TAG, "onTextChanged($start, $before, $count)")
                if (count == 0) {
                    binding.ivSend.visibility = INVISIBLE
                } else {
                    binding.ivSend.visibility = VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val participants = mutableListOf(MyDataViewModel.instance.getMyId())
        participants.add(counterpart.id)
        participants.sort()

        binding.tvTitle.text = counterpart.name

        startCall()

        PCObserverImpl.instance.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        d(TAG, "onDestroy")
        if (viewModel.call != null) {
            if (viewModel.call!!.connected) {
                viewModel.end(SendFCM.FCMType.Bye)
            } else {
                viewModel.end(SendFCM.FCMType.Cancel)
            }
        }
        PCObserverImpl.instance.remove(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TouchEffect.alpha(v!!, event)
        return false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_back -> { finish() }
            R.id.iv_send -> { send() }
        }
    }

    private fun send() {
        val msg = binding.etMessage.text.toString()
        setMessage(msg, MyDataViewModel.instance.getMyId())
        if (rtpConnected) {
            rtpManager.sendData(msg)
        } else {
            SendFCM.sendMessage(counterpart.fcmToken!!, SendFCM.FCMType.Message, message = msg)
        }
        binding.etMessage.setText("")
        binding.ivSend.visibility = INVISIBLE
    }

    private fun setMessage(msg: String, sender: String) {
        runOnUiThread {
            val lastMsg = messageList.last()
            val lastDate = DateUtil.toFormattedString(lastMsg.createdAt!!, "aa hh:mm")
            val newMsg = Message(sender, "", body = msg, createdAt = Date(currentTimeMillis()))
            if (
                lastDate == DateUtil.toFormattedString(newMsg.createdAt!!, "aa hh:mm") &&
                lastMsg.from == newMsg.from
            ) {
                messageList.last().timeFlag = false
                messageAdapter.notifyItemChanged(messageList.size - 1)
            }
            messageList.add(newMsg)
            messageAdapter.notifyItemInserted(messageList.size - 1)
            binding.recyclerView.post {
                binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
            }
        }
    }

    private fun startCall() {
        if (viewModel.call == null) return
        rtpManager.init(this,
            isAudio = false,
            isVideo = false,
            isDataChannel = true,
            enableStat = false,
            recordAudio = false
        )

        val isOffer =viewModel.call!!.direction == Call.Direction.Offer
        rtpManager.startRTP(context = this, data = null, isOffer = isOffer, viewModel.remoteSDP, viewModel.remoteIce)
    }

    companion object {
        private const val TAG = "MessageActivity"
    }

    override fun onPCConnected() {
        d(TAG, "onPCConnected")
        rtpConnected = true
    }

    override fun onPCDisconnected() {
        d(TAG, "onPCDisconnected")
    }

    override fun onPCFailed() {
        d(TAG, "onPCFailed")
    }

    override fun onPCClosed() {
        d(TAG, "onPCClosed")
        rtpConnected = false
    }

    override fun onPCStatsReady(reports: Array<StatsReport?>?) {
        d(TAG, "onPCStatsReady")
    }

    override fun onPCError(description: String?) {
        d(TAG, "onPCError")
    }

    override fun onMessage(message: String) {
        d(TAG, "onMessage $message")
        setMessage(message, counterpart.id)
    }
}