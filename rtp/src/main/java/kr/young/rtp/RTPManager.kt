package kr.young.rtp

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.i
import kr.young.rtp.observer.PCObserver
import kr.young.rtp.observer.PCObserverImpl
import kr.young.rtp.pc.*
import kr.young.rtp.util.DefaultValues
import kr.young.rtp.util.DefaultValues.Companion.isAudioProcessing
import kr.young.rtp.util.DefaultValues.Companion.saveRemoteVideoToFile
import kr.young.rtp.util.DefaultValues.Companion.videoHeight
import kr.young.rtp.util.DefaultValues.Companion.videoWidth
import kr.young.rtp.util.PCState
import kr.young.rtp.util.RTCAudioManager
import org.webrtc.*
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * WebRTC init order
 * create peer connection factory
 * create peer connection
 * create video/audio track
 * set remote/local description
 */
class RTPManager: PCObserver, PCObserver.SDP, PCObserver.ICE {
    /** set SurfaceViewRenderer */
    private class ProxyVideoSink : VideoSink {
        private var target: VideoSink? = null

        @Synchronized
        override fun onFrame(frame: VideoFrame) {
            if (target == null) {
                return
            }
            target!!.onFrame(frame)
        }

        @Synchronized
        fun setTarget(target: VideoSink?) {
            this.target = target
        }
    }

    private var executor: ExecutorService? = null

    var isInit = false
    var isCreatedPCFactory = false
    /** Options related RTP(PeerConnection) */
    private var isOffer = false
    private var isAudio = true
    private var isVideo = DefaultValues.isVideo
    private var isScreen = DefaultValues.isScreen
    private var isDataChannel = DefaultValues.isDataChannel
    private var enableStat = true
    private var recordAudio = true

    private val remoteRenderer = ProxyVideoSink()
    private var remoteSinks: ArrayList<VideoSink>? = null
    private var localVideoSink: ProxyVideoSink? = null

    private var eglBase: EglBase? = null

    private var pcManager: PCManager? = null
    private var pcParameters: PCParameters? = null
    private var pcState: PCState? = null
    private var iceServers: List<PeerConnection.IceServer>? = null

    private var audioMedia: AudioMedia? = null
    private var videoMedia: VideoMedia? = null

    private var isSwappedFeeds = false
    private var fullRenderer: SurfaceViewRenderer? = null
    private var pipRenderer: SurfaceViewRenderer? = null
    private var videoFileRenderer: VideoFileRenderer? = null

    private var rtcAudioManager: RTCAudioManager? = null

    private var statParser: StatParser? = null

    /**
     * Set options for RTP
     * isScreen - screen share
     * isDataChannel - send/receive data
     * enableStat - connection status
     */
    fun init(context: Context,
             isAudio: Boolean = this.isAudio,
             isVideo: Boolean = this.isVideo,
             isScreen: Boolean = this.isScreen,
             isDataChannel: Boolean = this.isDataChannel,
             enableStat: Boolean = this.enableStat,
             recordAudio: Boolean = this.recordAudio) {
        i(TAG, "init")
        this.isInit = true
        this.remoteSinks = arrayListOf()
        this.localVideoSink = ProxyVideoSink()
        this.statParser = StatParser()
        this.pcState = PCState()

        this.isAudio = isAudio
        this.isVideo = isVideo
        this.isScreen = isScreen
        this.isDataChannel = isDataChannel
        this.enableStat = enableStat
        this.recordAudio = recordAudio
        this.executor = Executors.newSingleThreadExecutor()

        this.iceServers = ICE().getIceServers()

        PCObserverImpl.instance.add(this as PCObserver)
        PCObserverImpl.instance.add(this as PCObserver.SDP)
        PCObserverImpl.instance.add(this as PCObserver.ICE)

        this.eglBase = EglBase.create()
        this.remoteSinks!!.add(remoteRenderer)

        if (saveRemoteVideoToFile != null) {
            try {
                this.videoFileRenderer = VideoFileRenderer(saveRemoteVideoToFile, videoWidth,
                    videoHeight, eglBase!!.eglBaseContext)
                this.remoteSinks!!.add(videoFileRenderer!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        audioManagerStart(context)

        setPCParameters()
        createPeerConnectionFactory(context)
    }

    /** Release resources */
    fun release() {
        i(TAG, "release()")

        PCObserverImpl.instance.remove(this as PCObserver)
        PCObserverImpl.instance.remove(this as PCObserver.SDP)
        PCObserverImpl.instance.remove(this as PCObserver.ICE)

        this.remoteRenderer.setTarget(null)
        this.localVideoSink?.setTarget(null)
        this.pipRenderer?.release()
        this.pipRenderer = null
        this.fullRenderer?.release()
        this.fullRenderer = null

        this.executor?.execute {
            d(TAG, "Release audio.")
            this.audioMedia?.release()
            d(TAG, "Release video.")
            this.videoMedia?.release()

            this.localVideoSink = null
            this.remoteSinks = null
            this.pcManager?.release()
            this.pcManager = null
            this.eglBase?.release()
            this.eglBase = null
            this.isCreatedPCFactory = false
        }

        Handler(Looper.getMainLooper()).post { this.rtcAudioManager?.stop() }
        this.rtcAudioManager = null
    }

    /**
     * Create PeerConnection
     * Create Offer or Answer
     */
    fun startRTP(
        context: Context,
        data: Intent? = null,
        isOffer: Boolean,
        remoteSdp: SessionDescription? = null,
        remoteICE: String? = null
    ) {
        i(
            TAG, "startRTP(context, isOffer $isOffer, " +
                    "remoteSdp is null ${remoteSdp==null})")
        this.isOffer = isOffer
        createPeerConnection(context, data)

        if (isOffer) {
            createOffer()
        } else if (remoteSdp!=null) {
            setRemoteDescription(remoteSdp)
            if (remoteICE != null) {
                val tokenizer = StringTokenizer(remoteICE, ";")
                while (tokenizer.hasMoreTokens()) {
                    addRemoteIceCandidate(IceCandidate("audio", 0, tokenizer.nextToken()))
                }
            }
            createAnswer()
        }
    }

    /** Send data through Data Channel */
    fun sendData(message: String) {
        this.pcManager!!.sendData(message)
    }

    /** Set TURN/STUN Server for ice candidates */
    fun setIceServers(iceServers: List<PeerConnection.IceServer>) {
        this.iceServers = iceServers
    }

    fun setIceServers(stunAddress: String, stunPort:String, turnAddress: String, turnPort: String, userName:String, password: String) {
        this.iceServers = ICE().getIceServers(stunAddress, stunPort, turnAddress, turnPort, userName, password)
    }

    /** Manage audio setting(select speaker, audio focus gain/loss) */
    private fun audioManagerStart(context: Context) {
        i(TAG, "audioManagerStart()")
        rtcAudioManager = RTCAudioManager.create(context)

        rtcAudioManager!!.start(object: RTCAudioManager.AudioManagerEvents {
            override fun onAudioDeviceChanged(
                selectedAudioDevice: RTCAudioManager.AudioDevice,
                availableAudioDevices: Set<RTCAudioManager.AudioDevice>
            ) {
                d(
                    TAG,
                    "onAudioManagerDevicesChanged: $availableAudioDevices ," +
                            "selected: $selectedAudioDevice")
            }
        })
    }

    private fun updateEncoderStatistics(reports: Array<StatsReport?>) {
        i(TAG, "updateEncoderStatistics()")
        statParser!!.updateEncoderStatistics(reports, isVideo)
    }

    private fun setPCParameters() {
        i(TAG, "setPCParameters()")
        pcParameters = PCParameters(
            isAudio, isVideo, isScreen, isDataChannel
        )
    }

    private fun createPeerConnectionFactory(context: Context) {
        i(TAG, "createPeerConnectionFactory()")

        executor!!.execute {
            pcManager =
                PCManager(context, pcParameters!!)
            pcManager!!.createPeerConnectionFactory(context, eglBase!!, executor!!, recordAudio)
            this.isCreatedPCFactory = true
        }
    }

    /**
     * Create audio/video track(mic, camera, screen)
     * PeerConnection is object to manager SDP options (ex. RTC config, media constraints)
     */
    private fun createPeerConnection(context: Context,
                                     data: Intent?) {
        i(TAG, "createPeerConnection()")
        i(TAG, "createPeerConnection - $iceServers")

        executor!!.execute {

            pcManager!!.createPeerConnection(isOffer, iceServers)

            val mediaStreamLabels = listOf(MEDIA_STREAM_LABEL)
            if (isAudio) {
                audioMedia = AudioMedia()
                val localAudioTrack = audioMedia!!.createAudioTrack(pcManager!!.factory!!, isAudioProcessing)
                if (localAudioTrack != null) {
                    pcManager!!.addTrack(localAudioTrack, mediaStreamLabels)
                }
            }
            if (isVideo) {
                videoMedia = VideoMedia()
                videoMedia!!.createVideoCapturer(context, isScreen && isOffer, data)
                val localVideoTrack = videoMedia!!.createVideoTrack(
                    context,
                    localVideoSink!!,
                    pcManager!!.factory!!,
                    eglBase!!
                )
                if (localVideoTrack != null) {
                    pcManager!!.addTrack(localVideoTrack, mediaStreamLabels)
                }
                videoMedia!!.getRemoteVideoTrack(pcManager!!.peerConnection!!, remoteSinks!!)
            }

            pcManager!!.startRecording()
            if (isOffer) {
                pcState!!.setPCState(PCState.State.OFFER_PENDING)
            } else {
                pcState!!.setPCState(PCState.State.ANSWER_PENDING)
            }
        }
    }

    /** Create "Caller" SDP and Gather ICE candidates(host, relay, server reflexive) */
    fun createOffer() {
        executor!!.execute {
            i(TAG, "createOffer()")
            pcState!!.setPCState(PCState.State.CREATE_OFFER)
            pcManager!!.createOffer()
        }
    }

    /** Create "Callee" SDP and Gather ICE candidates(host, relay, server reflexive) */
    fun createAnswer() {
        executor!!.execute {
            i(TAG, "createAnswer()")
            pcState!!.setPCState(PCState.State.CREATE_ANSWER)
            pcManager!!.createAnswer()
        }
    }

    fun setRemoteDescription(remoteSdp: SessionDescription) {
        executor!!.execute {
            i(TAG, "setRemoteDescription()")
            if (isOffer) {
                pcState!!.setPCState(PCState.State.CONNECT_PENDING)
            } else {
                pcState!!.setPCState(PCState.State.SET_REMOTE_OFFER)
            }
            pcManager!!.setRemoteDescription(remoteSdp)
        }
    }

    fun addRemoteIceCandidate(iceCandidate: IceCandidate) {
        executor!!.execute {
            i(TAG, "addRemoteIceCandidate()")
            pcManager!!.addRemoteIceCandidate(iceCandidate)
        }
    }

    private fun enableStatsEvents() {
        if (enableStat) {
            pcManager!!.enableStatsEvents(STAT_CALLBACK_PERIOD)
        }
    }

    fun initVideoView(fullRenderer: SurfaceViewRenderer,
                      pipRenderer: SurfaceViewRenderer
    ) {
        this.fullRenderer = fullRenderer
        this.pipRenderer = pipRenderer

        // Create video renderers.
        this.pipRenderer?.init(eglBase!!.eglBaseContext, null)
        this.pipRenderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        this.fullRenderer?.init(eglBase!!.eglBaseContext, null)
        this.fullRenderer?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        this.pipRenderer?.setZOrderMediaOverlay(true)
        this.pipRenderer?.setEnableHardwareScaler(true)
        this.fullRenderer?.setEnableHardwareScaler(false)
        setSwappedFeeds(true)
    }

    fun setSwappedFeeds(isSwappedFeeds: Boolean) {
        d(TAG, "setSwappedFeeds: $isSwappedFeeds")
        this.isSwappedFeeds = isSwappedFeeds
        if (isScreen) {
            if (isOffer) {
                localVideoSink?.setTarget(null)
                remoteRenderer.setTarget(null)
            } else {
                localVideoSink?.setTarget(null)
                remoteRenderer.setTarget(fullRenderer)
            }
            fullRenderer?.setMirror(false)
        } else {
            localVideoSink?.setTarget(if (isSwappedFeeds) fullRenderer else pipRenderer)
            remoteRenderer.setTarget(if (isSwappedFeeds) pipRenderer else fullRenderer)
            fullRenderer?.setMirror(isSwappedFeeds)
            pipRenderer?.setMirror(!isSwappedFeeds)
        }
    }

    fun switchCamera() {
        executor!!.execute {
            i(TAG, "switchCamera()")
            videoMedia?.switchCamera()
        }
    }

    fun captureFormatChange(width: Int, height: Int, frameRate: Int) {
        executor?.execute {
            i(TAG, "captureFormatChange(width $width, height $height, frameRate $frameRate)")
            videoMedia?.changeCaptureFormat(width, height, frameRate)
        }
    }

    fun setScaleType(type: RendererCommon.ScalingType) {
        i(TAG, "setScaleType(type $type)")
        fullRenderer?.setScalingType(type)
    }

    fun setSpeaker(speaker: Boolean) {
        if (speaker) {
            rtcAudioManager!!.setAudioDeviceInternal(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        } else {
            rtcAudioManager!!.setAudioDeviceInternal(RTCAudioManager.AudioDevice.EARPIECE)
        }
    }

    fun setMute(mute: Boolean) {
        rtcAudioManager!!.setMicrophoneMute(mute)
    }

    /** Mute on/off */
    fun setAudioEnable(enable: Boolean) {
        executor!!.execute {
            i(TAG, "setAudioEnable(enable $enable)")
            audioMedia?.setEnable(enable)
        }
    }

    fun setVideoEnable(enable: Boolean) {
        executor?.execute {
            i(TAG, "setVideoEnable(enable $enable)")
            videoMedia?.setEnable(enable)
        }
    }

    fun startVideoSource() {
        executor?.execute {
            videoMedia?.startVideoSource()
        }
    }

    fun stopVideoSource() {
        executor?.execute {
            videoMedia?.stopVideoSource()
        }
    }

    private object Holder {
        val INSTANCE = RTPManager()
    }

    companion object {
        private const val TAG = "RTPManager"
        val instance: RTPManager by lazy { Holder.INSTANCE }
        private const val STAT_CALLBACK_PERIOD = 1000L
        private const val MEDIA_STREAM_LABEL = "ARDAMS"
    }

    /** Event local SDP created */
    override fun onLocalDescription(sdp: SessionDescription?) {
        i(TAG, "onLocalDescription()")
        if (isOffer) {
            pcState!!.setPCState(PCState.State.SET_LOCAL_OFFER)
        } else {
            pcState!!.setPCState(PCState.State.CONNECT_PENDING)
        }
    }

    /** Event ICE candidate gather */
    override fun onICECandidate(candidate: IceCandidate?) {
        i(TAG, "onIceCandidate()")
        i(TAG, "candidate - $candidate")
//        addRemoteIceCandidate(candidate!!)
    }

    override fun onICECandidatesRemoved(candidates: Array<out IceCandidate?>?) {
        i(TAG, "onIceCandidatesRemoved()")
    }

    override fun onICEConnected() {
        i(TAG, "onIceConnected()")
    }

    override fun onICEDisconnected() {
        i(TAG, "onIceDisconnected()")
    }

    override fun onPCConnected() {
        i(TAG, "onPeerConnectionConnected()")
        enableStatsEvents()
        if (isVideo) {
            setSwappedFeeds(false)
        }
    }

    override fun onPCDisconnected() {
        i(TAG, "onPeerConnectionDisconnected()")
    }

    override fun onPCFailed() {
        i(TAG, "onPeerConnectionFailed()")
    }

    override fun onPCClosed() {
        i(TAG, "onPeerConnectionClosed()")
    }

    override fun onPCStatsReady(reports: Array<StatsReport?>?) {
        i(TAG, "onPeerConnectionStatsReady()")
        if (reports != null) {
            updateEncoderStatistics(reports)
        }
    }

    override fun onPCError(description: String?) {
        i(TAG, "onPeerConnectionError()")
    }

    /** Event receive data */
    override fun onMessage(msg: String) {
        i(TAG, "onMessage()")
    }
}
