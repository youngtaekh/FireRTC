package kr.young.firertc

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.adapter.MessageAdapter
import kr.young.firertc.databinding.ActivityMessageBinding
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Message
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.util.RecyclerViewNotifier.ModifierCategory.*
import kr.young.firertc.vm.MessageVM
import kr.young.rtp.observer.PCObserver
import kr.young.rtp.observer.PCObserverImpl
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import java.util.*

class MessageActivity : AppCompatActivity(), OnTouchListener, OnClickListener, PCObserver, PCObserver.ICE, PCObserver.SDP {
    private lateinit var binding: ActivityMessageBinding
    private val messageVM = MessageVM.instance

    private var isBottom = true
    private var lastVisiblePosition = -1
    private var isLoading = false
    private var isSending = false

    private var checkFirst = true

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var layoutManager: LinearLayoutManager

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message)
        d(TAG, "onCreate")

        messageAdapter = MessageAdapter()
        messageAdapter.setOnItemClickListener(longClickListener)
        binding.recyclerView.adapter = messageAdapter
        layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        //keep scroll when keyboard opened
        layoutManager.stackFromEnd = true
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.post { binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1) }

        binding.ivBack.setOnTouchListener(this)
        binding.ivBack.setOnClickListener(this)
        binding.ivSend.setOnTouchListener(this)
        binding.ivSend.setOnClickListener(this)
        binding.ivBottom.setOnTouchListener(this)
        binding.ivBottom.setOnClickListener(this)
        binding.tvLastMessage.setOnTouchListener(this)
        binding.tvLastMessage.setOnClickListener(this)

        binding.etMessage.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (start + count == 0) {
                    binding.ivSend.visibility = INVISIBLE
                } else {
                    binding.ivSend.visibility = VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.recyclerView.addOnScrollListener(object: OnScrollListener() {
            var dragging = false
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dragging) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (layoutManager.findLastCompletelyVisibleItemPosition() != lastVisiblePosition) {
                        lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                        if (layoutManager.findLastCompletelyVisibleItemPosition() == messageAdapter.itemCount - 1) {
                            binding.ivBottom.visibility = INVISIBLE
                            binding.tvLastMessage.visibility = INVISIBLE
                            isBottom = true
                        } else {
                            binding.ivBottom.visibility = VISIBLE
                            isBottom = false
                        }

                        if (layoutManager.findLastCompletelyVisibleItemPosition() < 20 &&
                            !isLoading &&
                            !messageVM.isNoAdditionalMessage &&
                            messageVM.messageList.value!!.first().sequence != 0L &&
                            messageVM.firstSequence != messageVM.messageList.value!!.first().sequence
                        ) {
                            isLoading = true
                            messageVM.firstSequence = messageVM.messageList.value!!.first().sequence
                            messageVM.getMessages(isAdditional = true, max = messageVM.messageList.value!!.first().sequence)
                        }
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                dragging = true
            }
        })

        messageAdapter.registerAdapterDataObserver(adapterObserver)

        PCObserverImpl.instance.add(this as PCObserver)
        PCObserverImpl.instance.add(this as PCObserver.SDP)
        PCObserverImpl.instance.add(this as PCObserver.ICE)

        messageVM.receiveMessage.observe(this) {
            if (checkFirst) {
                checkFirst = false
                return@observe
            }
            it?.let {
                d(TAG, "receivedMessage $it")
                messageVM.addDateMessage(listOf(it), false)
            }
        }

        messageVM.chat.observe(this) {
            it?.let {
                binding.tvTitle.text = if (it.isGroup) {
                    it.title
                } else {
                    it.localTitle
                }
            }
        }

        messageVM.messageList.observe(this) {
            d(TAG, "messageList observe isSending $isSending, isBottom $isBottom")
            messageAdapter.submitList(it)
            isLoading = false
        }
    }

    override fun onResume() {
        super.onResume()

        messageVM.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        d(TAG, "onDestroy")
        ChatRepository.removeChatListener()
        messageAdapter.unregisterAdapterDataObserver(adapterObserver)
        if (messageVM.rtpConnected) {
            messageVM.endRTP(SendFCM.FCMType.Bye)
        }
        messageVM.release()
        PCObserverImpl.instance.remove(this as PCObserver)
        PCObserverImpl.instance.remove(this as PCObserver.SDP)
        PCObserverImpl.instance.remove(this as PCObserver.ICE)
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
            R.id.iv_bottom, R.id.tv_last_message -> {
                binding.recyclerView.post {
                    binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }
                binding.ivBottom.visibility = INVISIBLE
                binding.tvLastMessage.visibility = INVISIBLE
            }
        }
    }

    private fun send() {
        isSending = true
        val msg = binding.etMessage.text.toString()

        messageVM.readySendMessage(msg)
        messageVM.addDateMessage(listOf(messageVM.sendMessage!!), false)
        binding.etMessage.setText("")
        binding.ivSend.visibility = INVISIBLE
    }

    private val longClickListener = object: MessageAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v) - ${messageAdapter.currentList[pos].body}")
            val clipBoard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("message", messageAdapter.currentList[pos].body)
            clipBoard.setPrimaryClip(clipData)
            Toast.makeText(this@MessageActivity, "Copy!!!!!", LENGTH_SHORT).show()
        }
    }

    override fun onPCConnected() {
        d(TAG, "onPCConnected")
        messageVM.rtpConnected = true
    }

    override fun onPCDisconnected() {
        d(TAG, "onPCDisconnected")
    }

    override fun onPCFailed() {
        d(TAG, "onPCFailed")
        messageVM.rtpConnected = false
    }

    override fun onPCClosed() {
        d(TAG, "onPCClosed")
        messageVM.rtpConnected = false
    }

    override fun onPCStatsReady(reports: Array<StatsReport?>?) {
        d(TAG, "onPCStatsReady")
    }

    override fun onPCError(description: String?) {
        d(TAG, "onPCError")
    }

    override fun onMessage(msg: String) {
        d(TAG, "onMessage ${messageVM.counterpart!!.id} $msg")
        val message = Message.fromJson(msg)
        messageVM.addDateMessage(listOf(message), false)
    }

    override fun onLocalDescription(sdp: SessionDescription?) {
        d(TAG, "onLocalDescription")
        sdp?.let {
            if (messageVM.isOffer) {
                messageVM.sendFCMMessage(SendFCM.FCMType.Offer, sdp.description)
            } else {
                messageVM.sendFCMMessage(SendFCM.FCMType.Answer, sdp.description)
            }
        }
    }

    override fun onICECandidate(candidate: IceCandidate?) {
        d(TAG, "onICECandidate")
        candidate?.let {
            messageVM.onIceCandidate(candidate.sdp)
        }
    }

    override fun onICECandidatesRemoved(candidates: Array<out IceCandidate?>?) {
        d(TAG, "onICECandidatesRemoved")
    }

    override fun onICEConnected() {
        d(TAG, "onICEConnected")
    }

    override fun onICEDisconnected() {
        d(TAG, "onICEDisconnected")
    }

    private val adapterObserver: AdapterDataObserver = object: AdapterDataObserver() {
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            d(TAG, "onItemRangeChanged($positionStart, $itemCount)")
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            d(TAG, "onItemRangeChanged($positionStart, $itemCount, payload)")
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            d(TAG, "onItemRangeInserted($positionStart, $itemCount)")
            if (isSending || isBottom) {
                binding.recyclerView.post {
                    d(TAG, "itemCount ${messageAdapter.itemCount}")
                    binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }
                isSending = false
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            d(TAG, "onItemRangeRemoved($positionStart, $itemCount)")
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            d(TAG, "onItemRangeMoved($fromPosition, $toPosition, $itemCount)")
        }
    }

    companion object {
        private const val TAG = "MessageActivity"
    }
}