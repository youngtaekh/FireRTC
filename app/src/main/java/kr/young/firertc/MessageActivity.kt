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
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.adapter.MessageAdapter
import kr.young.firertc.databinding.ActivityMessageBinding
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Message
import kr.young.firertc.model.User
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.firertc.util.RecyclerViewNotifier.ModifierCategory.*
import kr.young.firertc.vm.MessageViewModel
import kr.young.firertc.vm.MyDataViewModel
import kr.young.rtp.RTPManager
import kr.young.rtp.observer.PCObserver
import kr.young.rtp.observer.PCObserverImpl
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import java.util.*

class MessageActivity : AppCompatActivity(), OnTouchListener, OnClickListener, PCObserver, PCObserver.ICE, PCObserver.SDP {
    private lateinit var binding: ActivityMessageBinding
    private val viewModel = MessageViewModel.instance
    private val rtpManager = RTPManager.instance

    private val counterpart: User get() {
        return viewModel.counterpart!!
    }
    private val messageList = mutableListOf<Message>()

    private var isBottom = true
    private var lastVisiblePosition = -1
    private var isLoading = false

    private var checkFirst = true

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var layoutManager: LinearLayoutManager

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message)
        d(TAG, "onCreate")

        messageAdapter = MessageAdapter(messageList)
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

        viewModel.setReceivedMessage(null)

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
                        if (layoutManager.findLastCompletelyVisibleItemPosition() == messageList.size - 1) {
                            binding.ivBottom.visibility = INVISIBLE
                            binding.tvLastMessage.visibility = INVISIBLE
                            isBottom = true
                        } else {
                            binding.ivBottom.visibility = VISIBLE
                            isBottom = false
                        }

                        if (layoutManager.findLastCompletelyVisibleItemPosition() < 20 &&
                            !isLoading &&
                            !viewModel.isEndReload &&
                            messageList.first().sequence != 0L &&
                            viewModel.firstSequence != messageList.first().sequence
                        ) {
                            isLoading = true
                            viewModel.firstSequence = messageList.first().sequence
                            d(TAG, "notifier ${messageList.size}")
                            viewModel.getAdditionalMessages(list = messageList, max = messageList.first().sequence)
                        }
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                dragging = true
            }
        })

        PCObserverImpl.instance.add(this as PCObserver)
        PCObserverImpl.instance.add(this as PCObserver.SDP)
        PCObserverImpl.instance.add(this as PCObserver.ICE)

        viewModel.responseCode.observe(this) {
            if (it != null && it != 0) {
                if (it == USER_READ_SUCCESS) {
                    startCall()
                }
            }
        }

        viewModel.recyclerViewNotifier.observe(this) {
            if (it == null || it.list.isEmpty()) {
                return@observe
            }
//            runOnUiThread {
                d(TAG, "notifier ${messageList.size}")
                messageList.addAll(it.position, it.list)
                d(TAG, "notifier ${messageList.size}")
                when (it.modifierCategory) {
                    Insert -> messageAdapter.notifyItemRangeInserted(it.position, it.count)
                    Changed -> messageAdapter.notifyItemRangeChanged(it.position, it.count-1)
                    Removed -> messageAdapter.notifyItemRangeRemoved(it.position, it.count)
                }
                if (it.isBottom && messageList.last().from == MyDataViewModel.instance.getMyId() || isBottom) {
                    binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                } else if (it.position != 0 && messageList.last().from != MyDataViewModel.instance.getMyId() && !isBottom) {
                    binding.tvLastMessage.text = messageList.last().body
                    binding.tvLastMessage.visibility = VISIBLE
                }
                isLoading = false
//            }
        }

        viewModel.receivedMessage.observe(this) {
            if (checkFirst) {
                checkFirst = false
                return@observe
            }
            if (it != null) {
                d(TAG, "receivedMessage $it")
                viewModel.addMessage(messageList, it)
            }
        }

        binding.tvTitle.text = counterpart.name
    }

    override fun onResume() {
        super.onResume()

        viewModel.getChatMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        d(TAG, "onDestroy")
        ChatRepository.removeChatListener()
        if (viewModel.rtpConnected) {
            viewModel.end(SendFCM.FCMType.Bye)
        }
        viewModel.release()
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
        val msg = binding.etMessage.text.toString()

        val message = viewModel.sendData(msg)
        viewModel.addMessage(messageList, message)
        binding.etMessage.setText("")
        binding.ivSend.visibility = INVISIBLE
    }

    private fun startCall() {
        rtpManager.init(this,
            isAudio = false,
            isVideo = false,
            isDataChannel = true,
            enableStat = false,
            recordAudio = false
        )

        rtpManager.startRTP(context = this, data = null, isOffer = viewModel.isOffer, viewModel.remoteSDP, viewModel.remoteIce)
    }

    private val longClickListener = object: MessageAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v) - ${messageList[pos].body}")
            val clipBoard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("message", messageList[pos].body)
            clipBoard.setPrimaryClip(clipData)
            Toast.makeText(this@MessageActivity, "Copy!!!!!", LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "MessageActivity"
    }

    override fun onPCConnected() {
        d(TAG, "onPCConnected")
        viewModel.onPCConnected()
    }

    override fun onPCDisconnected() {
        d(TAG, "onPCDisconnected")
    }

    override fun onPCFailed() {
        d(TAG, "onPCFailed")
    }

    override fun onPCClosed() {
        d(TAG, "onPCClosed")
        viewModel.onPCClosed()
    }

    override fun onPCStatsReady(reports: Array<StatsReport?>?) {
        d(TAG, "onPCStatsReady")
    }

    override fun onPCError(description: String?) {
        d(TAG, "onPCError")
    }

    override fun onMessage(msg: String) {
        d(TAG, "onMessage ${counterpart.id} $msg")
        val message = Message.fromJson(msg)
        viewModel.addMessage(messageList, message)
    }

    override fun onLocalDescription(sdp: SessionDescription?) {
        d(TAG, "onLocalDescription")
        if (viewModel.isOffer) {
            viewModel.sendOffer(sdp!!.description)
        } else {
            viewModel.sendAnswer(sdp!!.description)
        }
    }

    override fun onICECandidate(candidate: IceCandidate?) {
        d(TAG, "onICECandidate")
        viewModel.onIceCandidate(candidate!!.sdp)
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
}