package kr.young.examplewebrtc

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import kr.young.common.UtilLog
import kr.young.common.UtilLog.Companion.d
import kr.young.rtp.RTPManager
import kr.young.rtp.observer.PCObserver
import kr.young.rtp.observer.PCObserverImpl
import org.webrtc.IceCandidate
import org.webrtc.NetworkMonitor
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import java.io.FileDescriptor
import java.io.PrintWriter

class CallService : Service(), PCObserver, PCObserver.ICE, PCObserver.SDP {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        d(TAG, "attachBaseContext")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        d(TAG, "onConfigurationChanged")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        d(TAG, "onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        d(TAG, "onTrimMemory")
    }

    override fun onCreate() {
        super.onCreate()
        d(TAG, "onCreate")
        PCObserverImpl.instance.add(this as PCObserver)
        PCObserverImpl.instance.add(this as PCObserver.SDP)
        PCObserverImpl.instance.add(this as PCObserver.ICE)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        d(TAG, "onStart")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        d(TAG, "onStartCommand")
        RTPManager.instance.init(this,
            isAudio = true,
            isVideo = false,
            isDataChannel = false,
            enableStat = false,
            recordAudio = false
        )

        RTPManager.instance.startRTP(this, null, true, null, null)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
//        NetworkMonitor.getInstance().stopMonitoring()
        RTPManager.instance.release()
        d(TAG, "onDestroy")
        PCObserverImpl.instance.remove(this as PCObserver)
        PCObserverImpl.instance.remove(this as PCObserver.SDP)
        PCObserverImpl.instance.remove(this as PCObserver.ICE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        d(TAG, "onBind")
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        d(TAG, "onRebind")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        d(TAG, "onTaskRemoved")
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        super.dump(fd, writer, args)
        d(TAG, "dump")
    }

    override fun onLocalDescription(sdp: SessionDescription?) {
        d(TAG, "onLocalDescription")
    }

    override fun onICECandidate(candidate: IceCandidate?) {
        d(TAG, "onICECandidate")
    }

    override fun onICECandidatesRemoved(candidates: Array<out IceCandidate>?) {
        d(TAG, "onICECandidatesRemoved")
    }

    override fun onICEConnected() {
        d(TAG, "onICEConnected")
    }

    override fun onICEDisconnected() {
        d(TAG, "onICEDisconnected")
    }

    override fun onPCConnected() {
        d(TAG, "onPCConnected")
    }

    override fun onPCDisconnected() {
        d(TAG, "onPCDisconnected")
    }

    override fun onPCFailed() {
        d(TAG, "onPCFailed")
    }

    override fun onPCClosed() {
        d(TAG, "onPCClosed")
    }

    override fun onPCStatsReady(reports: Array<StatsReport?>?) {
        d(TAG, "onPCStatsReady")
    }

    override fun onPCError(description: String?) {
        d(TAG, "onPCError")
    }

    override fun onMessage(message: String) {
        d(TAG, "onMessage")
    }
    
    companion object {
        private const val TAG = "CallService"
    }
}