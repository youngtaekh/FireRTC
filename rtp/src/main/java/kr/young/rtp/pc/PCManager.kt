package kr.young.rtp.pc

import android.content.Context
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.rtp.RecordedAudioToFileController
import kr.young.rtp.observer.PCListener
import kr.young.rtp.observer.PCObserverImpl
import kr.young.rtp.observer.SDPListener
import kr.young.rtp.util.DefaultValues
import kr.young.rtp.util.DefaultValues.Companion.videoFPS
import kr.young.rtp.util.DefaultValues.Companion.videoHeight
import kr.young.rtp.util.DefaultValues.Companion.videoWidth
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService

class PCManager(
    appContext: Context,
    private val pcParameters: PCParameters
) {
    private var pcObserverImpl: PCObserverImpl? = null
    private var sdpListener: SDPListener? = null

    var factory: PeerConnectionFactory? = null
    var peerConnection: PeerConnection? = null
    private var pcListener: PCListener? = null
    private var sdpMediaConstraints: MediaConstraints? = null
    private var dataChannel: DataChannel? = null
    private var saveRecordedAudioToFile: RecordedAudioToFileController? = null

    /** Options for audio/video */
    private var isFlexFEC: Boolean = DefaultValues.isFlexFEC
    private var isBuiltInAGC: Boolean = DefaultValues.isBuiltInAGC
    private var isLoopback: Boolean = DefaultValues.isLoopback
    private var isHardwareCodec: Boolean = DefaultValues.isHardwareCodec
    private var isAudioToFile = DefaultValues.isAudioToFile
    private var useOpenSLES = DefaultValues.useOpenSLES

    /** timer for stat period */
    private var statTimer: Timer? = null

    @Suppress("DEPRECATION")
    private var timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            peerConnection?.getStats(statsObserver, null)
        }
    }

    private var statsObserver = StatsObserver {
        pcObserverImpl?.onPCStatsReadyObserver(it)
    }

    init {
        pcObserverImpl = PCObserverImpl.instance
        pcListener = PCListener(pcObserverImpl!!)
        statTimer = Timer()

        val fieldTrials = getFieldTrials()
        val options = PeerConnectionFactory.InitializationOptions.builder(appContext)
            .setFieldTrials(fieldTrials)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun getFieldTrials(): String {
        var fieldTrials = ""
        if (isFlexFEC) {
            fieldTrials += VIDEO_FLEX_FEC_FIELD_TRIAL
            d(TAG, "Enable FlexFEC field trial.")
        }
        fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELD_TRIAL
        if (isBuiltInAGC) {
            fieldTrials += DISABLE_WEBRTC_AGC_FIELD_TRIAL
            d(TAG, "Disable WebRTC AGC field trial.")
        }
        return fieldTrials
    }

    /**
     * Create encoder/decoder
     * Set event audio record
     */
    fun createPeerConnectionFactory(
        appContext: Context,
        eglBase: EglBase,
        executor: ExecutorService,
        isAudioToFile: Boolean = true
    ): PeerConnectionFactory? {
        // 하나의 객체만 생성한다.
        if (factory != null) {
            e(TAG, "peerConnectionFactory already created")
            return null
        }
        // 음성을 파일로 저장할 경우 실행되는 코드
        if (isAudioToFile && !useOpenSLES) {
            saveRecordedAudioToFile =
                RecordedAudioToFileController(executor)
        }

        val audioDeviceModule = AudioMedia()
            .createJavaAudioDevice(appContext, saveRecordedAudioToFile)

        val options: PeerConnectionFactory.Options = PeerConnectionFactory.Options()
        if (isLoopback) {
            options.networkIgnoreMask = 0
        }
        // 영상 인코더/디코더 선택
        val encoderFactory: VideoEncoderFactory?
        val decoderFactory: VideoDecoderFactory?

        if (isHardwareCodec) {
            encoderFactory = DefaultVideoEncoderFactory(
                eglBase.eglBaseContext,
                true,
                false)
            decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }
        // 객체 생성
        this.factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        audioDeviceModule?.release()
        // 로그 출력 레벨 설정
        Logging.enableLogToDebugOutput(Logging.Severity.LS_NONE)

        return factory
    }

    /**
     * create peer connection
     * set media constraints
     * set RTC config
     * set peer connection event listener
     */
    fun createPeerConnection(isOffer: Boolean, iceServers: List<PeerConnection.IceServer>?): PeerConnection? {
        //음성, 영상 미디어에 관련된 값 설정
        createMediaConstraintsInternal()
        this.peerConnection = factory!!.createPeerConnection(getRTCConfig(iceServers), pcListener)
        this.sdpListener = SDPListener(
            isOffer,
            peerConnection!!,
            pcObserverImpl!!
        )
        // 음성, 영상 외의 데이터를 전송을 원할 경우 데이터채널 설정 (실시간 채팅에 활용)
        if (pcParameters.isDataChannel) {
            val init = DataChannel.Init()
            init.ordered = DefaultValues.isOrdered
            init.negotiated = DefaultValues.isNegotiated
            init.maxRetransmits = DefaultValues.maxRetransmitPreference
            init.maxRetransmitTimeMs = DefaultValues.maxRetransmitTimeMs
            init.id = DefaultValues.dataId
            init.protocol = DefaultValues.subProtocol
            dataChannel = peerConnection!!.createDataChannel(DATA_CHANNEL_LABEL, init)
        }
        return peerConnection
    }

    fun startRecording() {
        i(TAG, "startRecording(${isAudioToFile && useOpenSLES})")
        saveRecordedAudioToFile?.start()
    }

    fun release() {
        statTimer?.cancel()
        d(TAG, "Dispose dataChannel.")
        dataChannel?.dispose()
        dataChannel = null

        d(TAG, "Dispose peerConnection.")
        peerConnection?.dispose()
        peerConnection = null

        saveRecordedAudioToFile?.stop()
        saveRecordedAudioToFile = null

        d(TAG, "Closing peer connection factory.")
        factory?.dispose()
        factory = null

        d(TAG, "Closing peer connection done.")
//        pcObserverImpl!!.onPCClosedObserver()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }

    /**
     * Set video width/height/fps
     * receive audio/video or not
     */
    private fun createMediaConstraintsInternal() {
        if (pcParameters.isVideo) {
            if (videoWidth == 0 || videoHeight == 0) {
                videoWidth =
                    HD_VIDEO_WIDTH
                videoHeight =
                    HD_VIDEO_HEIGHT
            }

            if (videoFPS == 0) {
                videoFPS = 30
            }
            d(TAG, "Capturing format: $videoWidth x $videoHeight @ $videoFPS")
        }

        // Create SDP constraints.
        sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints!!.mandatory.add(
            MediaConstraints.KeyValuePair("OfferToReceiveAudio", pcParameters.isAudio.toString())
        )
//        val receiveVideo = pcParameters.isVideo && !(pcParameters.isScreen && isOffer)
        val receiveVideo = pcParameters.isVideo
        sdpMediaConstraints!!.mandatory.add(
            MediaConstraints.KeyValuePair("OfferToReceiveVideo", receiveVideo.toString())
        )
    }

    /**
     * tcpCandidatePolicy - Connect RTP using TCP(common UDP)
     * bundlePolicy - ICE gathering for tracks(audio, video, data)
     * rtcpMuxPolicy - RTP and multiplex RTCP
     * continualGatheringPolicy - ICE gathering policy(once, CONTINUALLY)
     * keyType - For SRTP
     * enableDtlsSrtp - true(DTLS-SRTP), false(SDES)
     * sdpSemantics - SDP format
     */
    private fun getRTCConfig(iceServers: List<PeerConnection.IceServer>?): PeerConnection.RTCConfiguration {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy =
            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        rtcConfig.enableDtlsSrtp = !isLoopback
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        return rtcConfig
    }

    /** Add audio/video track to peer connection */
    fun addTrack(track: MediaStreamTrack, label: List<String>) {
        peerConnection!!.addTrack(track, label)
    }

    fun createOffer() {
        if (peerConnection != null) {
            d(TAG, "PC Create OFFER")
            peerConnection!!.createOffer(sdpListener, sdpMediaConstraints)
        }
    }

    fun createAnswer() {
        if (peerConnection != null) {
            d(TAG, "PC create ANSWER")
            // 2. 상대방의 데이터를 토대로 SDP를 생성하여 전송
            peerConnection!!.createAnswer(sdpListener, sdpMediaConstraints)
        }
    }

    fun addRemoteIceCandidate(candidate: IceCandidate?) {
        d(TAG, "addRemoteIceCandidate")
        if (peerConnection != null) {
            if (sdpListener != null) {
                d(TAG, "sdpListener!!.addCandidate")
                sdpListener!!.addCandidate(candidate!!)
            }
            if (peerConnection != null) {
                d(TAG, "peerConnection!!.addIceCandidate")
                peerConnection!!.addIceCandidate(candidate)
            }
        }
    }

    /** Set remote description to peer connection */
    fun setRemoteDescription(sdp: SessionDescription) {
        if (peerConnection != null) {
            d(TAG, "Set remote SDP.")
            peerConnection!!.setRemoteDescription(sdpListener, sdp)
        }
    }

    fun sendData(message: String) {
        if (dataChannel == null) return
        // 메시지를 바이트배열로 변환
        val byteBuffer = ByteBuffer.allocate(message.toByteArray().size)
        byteBuffer.put(message.toByteArray())
        byteBuffer.flip()
        // PeerConnection 객체에서 생성한 데이터채널을 통해 메시지 전송
        val buffer = DataChannel.Buffer(byteBuffer, false)
        dataChannel!!.send(buffer)
    }

    fun enableStatsEvents(periodMs: Long) {
        statTimer?.schedule(timerTask, 0, periodMs)
    }

    companion object {
        private const val TAG = "PCManager"
        private const val HD_VIDEO_WIDTH = 1280
        private const val HD_VIDEO_HEIGHT = 720
        private const val VIDEO_FLEX_FEC_FIELD_TRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/"
        private const val VIDEO_VP8_INTEL_HW_ENCODER_FIELD_TRIAL =
            "WebRTC-IntelVP8/Enabled/"
        private const val DISABLE_WEBRTC_AGC_FIELD_TRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/"
        private const val DATA_CHANNEL_LABEL = "message data"
    }
}
