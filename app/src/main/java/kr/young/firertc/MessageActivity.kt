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
import kr.young.firertc.repo.MessageRepository.Companion.MESSAGE_READ_SUCCESS
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.firertc.util.RecyclerViewNotifier.ModifierCategory.*
import kr.young.firertc.vm.ChatViewModel
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

    private var isBottom = true
    private var lastVisiblePosition = -1

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var layoutManager: LinearLayoutManager

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message)
        d(TAG, "onCreate")

        messageAdapter = MessageAdapter(viewModel.messageList)
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
//                    d(TAG, "onScrolled($dx, $dy)")
//                    d(TAG, "last item ${layoutManager.findLastCompletelyVisibleItemPosition()}")
//                    d(TAG, "first item ${layoutManager.findFirstCompletelyVisibleItemPosition()}")
                    if (layoutManager.findLastCompletelyVisibleItemPosition() != lastVisiblePosition) {
                        lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                        if (layoutManager.findLastCompletelyVisibleItemPosition() == viewModel.messageList.size - 1) {
                            binding.ivBottom.visibility = INVISIBLE
                            isBottom = true
                        } else {
                            binding.ivBottom.visibility = VISIBLE
                            isBottom = false
                        }

                        if (layoutManager.findLastCompletelyVisibleItemPosition() < 20 &&
                            !viewModel.isEndReload &&
                            viewModel.messageList.first().sequence != 0L &&
                            viewModel.firstSequence != viewModel.messageList.first().sequence
                        ) {
                            d(TAG, "reload messages")
                            viewModel.firstSequence = viewModel.messageList.first().sequence
                            viewModel.getAdditionalMessages(max = viewModel.messageList.first().sequence)
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
                } else if (it == MESSAGE_READ_SUCCESS) {
                    d(TAG, "MESSAGE_READ_SUCCESS ${viewModel.messageList.size} ${viewModel.size}")
                    messageAdapter.notifyItemRangeInserted(0, viewModel.size)
                }
            }
        }

        viewModel.recyclerViewNotifier.observe(this) {
            if (it != null) {
                runOnUiThread {
                    when (it.modifierCategory) {
                        Insert -> {
                            messageAdapter.notifyItemInserted(it.position)
                        }
                        Changed -> {
                            messageAdapter.notifyItemChanged(it.position)
                        }
                        Removed -> {
                            messageAdapter.notifyItemRemoved(it.position)
                        }
                    }
                    if (it.modifierCategory == Insert && (viewModel.messageList.last().from == MyDataViewModel.instance.getMyId() || isBottom)) {
                        binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                    }
                }
            }
        }

        binding.tvTitle.text = counterpart.name
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
            R.id.iv_bottom -> {
                binding.recyclerView.post {
                    binding.recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }
                binding.ivBottom.visibility = INVISIBLE
            }
        }
    }

    private fun send() {
        val msg = binding.etMessage.text.toString()

        val message = viewModel.sendData(msg)
        viewModel.addMessage(message)
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
            d(TAG, "onLongClick($pos, v) - ${viewModel.messageList[pos].body}")
            val clipBoard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("message", viewModel.messageList[pos].body)
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
        viewModel.addMessage(message)
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