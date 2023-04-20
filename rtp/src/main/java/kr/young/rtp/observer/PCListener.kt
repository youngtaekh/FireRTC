package kr.young.rtp.observer

import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.rtp.observer.PCObserverImpl.Companion.instance
import org.webrtc.*
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PCListener(private val pcObserverImpl: PCObserverImpl): PeerConnection.Observer {
    private var executor: ExecutorService? = null
    init {
        this.executor = Executors.newSingleThreadExecutor()
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        this.executor!!.execute { pcObserverImpl.onICECandidateObserver(p0) }
    }

    override fun onDataChannel(dc: DataChannel?) {
        dc!!.registerObserver(object: DataChannel.Observer {
            override fun onBufferedAmountChange(p0: Long) {
                i(TAG, "onDataChannel.onBufferedAmountChange" +
                        "label: ${dc.label()}, state: ${dc.state()}")
            }

            override fun onStateChange() {
                i(TAG, "onDataChannel.onStateChange" +
                        "label: ${dc.label()}, state: ${dc.state()}")
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                i(TAG, "onDataChannel.onMessage" +
                        "label: ${dc.label()}, state: ${dc.state()}")
                if (buffer == null || buffer.binary) {
                    i(TAG, "Received binary msg over $dc")
                    return
                }
                val data = buffer.data
                val bytes = ByteArray(data.capacity())
                data[bytes]
                val strData = String(bytes, Charset.forName("UTF-8"))
                i(TAG, "Got msg: $strData over $dc")
                instance.onMessageObserver(strData)
            }
        })
    }

    /** State for offer/answer */
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        i(TAG, "onSignalingChange($p0)")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        d(
            TAG,
            "IceConnectionReceiving changed to $p0"
        )
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        d(TAG, "IceConnectionState: $p0")
        this.executor!!.execute {
            when (p0) {
                PeerConnection.IceConnectionState.CONNECTED -> {
                    pcObserverImpl.onICEConnectedObserver()
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    pcObserverImpl.onICEDisconnectedObserver()
                }
                PeerConnection.IceConnectionState.FAILED -> {}
                PeerConnection.IceConnectionState.NEW -> {}
                PeerConnection.IceConnectionState.CHECKING -> {}
                PeerConnection.IceConnectionState.COMPLETED -> {}
                PeerConnection.IceConnectionState.CLOSED -> {}
                else -> { i(TAG, "ICE connection else.") }
            }
        }
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        d(TAG, "IceGatheringState: $p0")
    }

    override fun onIceCandidatesRemoved(p0: Array<IceCandidate?>?) {
        i(TAG, "onIceCandidatesRemoved()")
        this.executor!!.execute {
            pcObserverImpl.onICECandidatesRemovedObserver(p0)
        }
    }

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        i(TAG, "PeerConnectionState: $newState")
        this.executor!!.execute {
            when (newState) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    pcObserverImpl.onPCConnectedObserver()
                }
                PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    pcObserverImpl.onPCDisconnectedObserver()
                }
                PeerConnection.PeerConnectionState.FAILED -> {
                    pcObserverImpl.onPCFailedObserver()
                }
                PeerConnection.PeerConnectionState.NEW -> {
                }
                PeerConnection.PeerConnectionState.CONNECTING -> {
                }
                PeerConnection.PeerConnectionState.CLOSED -> {
                    pcObserverImpl.onPCClosedObserver()
                }
                else -> { i(TAG, "ICE Connection else") }
            }
        }
    }

    override fun onAddStream(p0: MediaStream?) {
        i(TAG, "onAddStream($p0)")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        i(TAG, "onRemoveStream($p0)")
    }

    override fun onRenegotiationNeeded() {
        i(TAG, "onRenegotiationNeeded()")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        i(TAG, "onAddTrack($p0, $p1)")
    }

    companion object {
        private const val TAG = "PCListener"
    }
}
