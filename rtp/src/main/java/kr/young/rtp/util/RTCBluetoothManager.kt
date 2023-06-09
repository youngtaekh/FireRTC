package kr.young.rtp.util

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.i
import kr.young.common.UtilLog.Companion.w
import org.webrtc.ThreadUtils

@SuppressLint("MissingPermission")
class RTCBluetoothManager private constructor(
    private val context: Context,
    private val rtcAudioManager: RTCAudioManager
) {

    enum class State {
        UNINITIALIZED,
        ERROR,
        HEADSET_UNAVAILABLE,
        HEADSET_AVAILABLE,
        SCO_DISCONNECTING,
        SCO_CONNECTING,
        SCO_CONNECTED
    }

    private val audioManager: AudioManager
    private val handler: Handler

    var scoConnectionAttempts: Int = 0
    private var bluetoothState: State
    private val bluetoothServiceListener: BluetoothProfile.ServiceListener
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private val bluetoothHeadsetReceiver: BroadcastReceiver

    private val bluetoothTimeoutRunnable = Runnable { bluetoothTimeout() }

    private inner class BluetoothServiceListener: BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
                return
            }
            d(TAG, "BluetoothServiceListener.onServiceConnected: BT state=$bluetoothState")
            bluetoothHeadset = proxy as BluetoothHeadset
            updateAudioDeviceState()
            d(TAG, "onServiceConnected done: BT state=$bluetoothState")
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
                return
            }
            d(TAG, "BluetoothServiceListener.onServiceDisconnected: BT state=$bluetoothState")
            stopScoAudio()
            bluetoothHeadset = null
            bluetoothDevice = null
            bluetoothState = State.HEADSET_UNAVAILABLE
            updateAudioDeviceState()
            d(TAG, "onServiceDisconnected done: BT state=$bluetoothState")
        }

    }

    private inner class BluetoothHeadsetBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (bluetoothState == State.UNINITIALIZED) {
                return
            }
            val action = intent?.action

            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                d(TAG, "BluetoothHeadsetBroadcastReceiver.onReceive: " +
                        "a=ACTION_CONNECTION_STATE_CHANGED, " +
                        "s=${stateToString(state)}, " +
                        "sb=$isInitialStickyBroadcast, " +
                        "BT state: $bluetoothState")
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    scoConnectionAttempts = 0
                    updateAudioDeviceState()
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    stopScoAudio()
                    updateAudioDeviceState()
                }
            } else if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
                d(TAG, "BluetoothHeadsetBroadcastReceiver.onReceive: " +
                        "a=ACTION_AUDIO_STATE_CHANGED, " +
                        "s=${stateToString(state)}, " +
                        "sb=$isInitialStickyBroadcast, " +
                        "BT state: $bluetoothState")
                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    cancelTimer()
                    if (bluetoothState == State.SCO_CONNECTING) {
                        d(TAG, "+++ Bluetooth audio SCO is now connected")
                        bluetoothState = State.SCO_CONNECTED
                        scoConnectionAttempts = 0
                        updateAudioDeviceState()
                    } else {
                        w(TAG, "Unexpected state BluetoothHeadset.STATE_AUDIO_CONNECTED")
                    }
                } else if (state == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
                    d(TAG, "+++ Bluetooth audio SCO is now connecting...")
                } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                    d(TAG, "+++ Bluetooth audio SCO is now disconnected")
                    if (isInitialStickyBroadcast) {
                        d(TAG, "Ignore STATE_AUDIO_DISCONNECTED initial sticky broadcast.")
                        return
                    }
                    updateAudioDeviceState()
                }
            }
            d(TAG, "onReceive done: BT state=$bluetoothState")
        }
    }

    init {
        i(TAG, "init")
        ThreadUtils.checkIsOnMainThread()
        audioManager = getAudioManager(context)
        bluetoothState = State.UNINITIALIZED
        bluetoothServiceListener = BluetoothServiceListener()
        bluetoothHeadsetReceiver = BluetoothHeadsetBroadcastReceiver()
        handler = Handler(Looper.getMainLooper())
    }

    fun getState(): State {
        ThreadUtils.checkIsOnMainThread()
        return bluetoothState
    }

    fun start() {
        ThreadUtils.checkIsOnMainThread()
        d(TAG, "start")
        if (!hasPermission(android.Manifest.permission.BLUETOOTH)) {
            w(TAG, "Process (pid=${Process.myPid()}) lacks BLUETOOTH permission")
            return
        }
        if (bluetoothState != State.UNINITIALIZED) {
            w(TAG, "Invalid BT state")
            return
        }
        bluetoothHeadset = null
        bluetoothDevice = null
        scoConnectionAttempts = 0
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            w(TAG, "Device does not support Bluetooth")
            return
        }
        if (!audioManager.isBluetoothScoAvailableOffCall) {
            e(TAG, "Bluetooth SCO audio is not available off call")
            return
        }
        logBluetoothAdapterInfo(bluetoothAdapter!!)
        if (!getBluetoothProfileProxy(BluetoothProfile.HEADSET)) {
            e(TAG, "BluetoothAdapter.getProfileProxy(HEADSET) failed")
            return
        }

        val bluetoothHeadsetFilter = IntentFilter()
        bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
        registerReceiver(bluetoothHeadsetReceiver, bluetoothHeadsetFilter)
        d(TAG, "HEADSET profile state:" +
                stateToString(bluetoothAdapter!!.getProfileConnectionState(BluetoothProfile.HEADSET)))
        d(TAG, "Bluetooth proxy for headset profile has started")
        bluetoothState = State.HEADSET_UNAVAILABLE
        d(TAG, "start done: BT state=$bluetoothState")
    }

    fun stop() {
        ThreadUtils.checkIsOnMainThread()
        d(TAG, "stop: BT state=$bluetoothState")
        if (bluetoothAdapter == null) {
            return
        }
        stopScoAudio()
        if (bluetoothState == State.UNINITIALIZED) {
            return
        }
        unregisterReceiver(bluetoothHeadsetReceiver)
        cancelTimer()
        if (bluetoothHeadset != null) {
            bluetoothAdapter!!.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
            bluetoothHeadset = null
        }
        bluetoothAdapter = null
        bluetoothDevice = null
        bluetoothState = State.UNINITIALIZED
        d(TAG, "stop done: BT state=$bluetoothState")
    }

    fun startScoAudio(): Boolean {
        ThreadUtils.checkIsOnMainThread()
        d(TAG, "startSco: BT state= $bluetoothState, " +
                "attempts: $scoConnectionAttempts, " +
                "SCO is on: ${isScoOn()}")
        if (scoConnectionAttempts >= MAX_SCO_CONNECTION_ATTEMPTS) {
            e(TAG, "BT SCO connection fails - no more attempts")
            return false
        }
        if (bluetoothState != State.HEADSET_UNAVAILABLE) {
            e(TAG, "BT SCO connection fails - no headset available")
            return false
        }

        d(TAG, "Starting Bluetooth SCO and waits for ACTION_AUDIO_STATE_CHANGED...")
        bluetoothState = State.SCO_CONNECTING
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        scoConnectionAttempts++
        startTimer()
        d(TAG, "startScoAudio done: BT state=$bluetoothState," +
                "SCO is on:${isScoOn()}")
        return true
    }

    fun stopScoAudio() {
        ThreadUtils.checkIsOnMainThread()
        d(TAG, "stopScoAudio: BT state=$bluetoothState, SCO is on: ${isScoOn()}")
        if (bluetoothState != State.SCO_CONNECTING && bluetoothState != State.SCO_CONNECTED) {
            return
        }
        cancelTimer()
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        bluetoothState = State.SCO_DISCONNECTING
        d(TAG, "stopScoAudio done: BT state=$bluetoothState," +
                "SCO is on: ${isScoOn()}")
    }

    fun updateDevice() {
        if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
            return
        }
        d(TAG, "updateDevices")
        val devices = bluetoothHeadset!!.connectedDevices
        if (devices.isEmpty()) {
            bluetoothDevice = null
            bluetoothState = State.HEADSET_UNAVAILABLE
            d(TAG, "No connected bluetooth headset")
        } else {
            bluetoothDevice = devices[0]
            bluetoothState = State.HEADSET_UNAVAILABLE
            d(TAG, "Connected bluetooth headset: " +
                    "name=${bluetoothDevice!!.name}, " +
                    "state=${stateToString(bluetoothHeadset!!.getConnectionState(bluetoothDevice))}, " +
                    "SCO audio=${bluetoothHeadset!!.isAudioConnected(bluetoothDevice)}")
        }
        d(TAG, "updateDevice done: BT state=$bluetoothState")
    }

    private fun getAudioManager(context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        context.registerReceiver(receiver, filter)
    }

    private fun unregisterReceiver(receiver: BroadcastReceiver) {
        context.unregisterReceiver(receiver)
    }

    private fun getBluetoothProfileProxy(profile: Int, ): Boolean {
        return bluetoothAdapter!!.getProfileProxy(context, bluetoothServiceListener, profile)
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkPermission(permission, Process.myPid(), Process.myUid()) ==
                PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("HardwareIds")
    private fun logBluetoothAdapterInfo(localAdapter: BluetoothAdapter) {
        d(TAG, "BluetoothAdapter: enabled=${localAdapter.isEnabled}, " +
                "state=${stateToString(localAdapter.state)}, " +
                "name=${localAdapter.name}, " +
                "address=${localAdapter.address}")
        val pairedDevices = localAdapter.bondedDevices
        if (pairedDevices.isEmpty()) {
            d(TAG, "paired devices:")
            for (device in pairedDevices) {
                d(TAG, "name=${device.name}, address=${device.address}")
            }
        }
    }

    private fun updateAudioDeviceState() {
        ThreadUtils.checkIsOnMainThread()
        d(TAG, "updateAudioDeviceState")
        rtcAudioManager.updateAudioDeviceState()
    }

    private fun startTimer() {
        ThreadUtils.checkIsOnMainThread()
        d(TAG, "startTimer")
        handler.postDelayed(bluetoothTimeoutRunnable, BLUETOOTH_SCO_TIMEOUT_MS)
    }

    private fun cancelTimer() {
        ThreadUtils.checkIsOnMainThread()
        d(TAG, "cancelTimer")
        handler.removeCallbacks(bluetoothTimeoutRunnable)
    }

    private fun bluetoothTimeout() {
        ThreadUtils.checkIsOnMainThread()
        if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
            return
        }
        d(TAG, "bluetoothTimeout: BT state=$bluetoothState, " +
                "attempts: $scoConnectionAttempts, " +
                "SCO is on: ${isScoOn()}")
        if (bluetoothState != State.SCO_CONNECTING) {
            return
        }
        var scoConnected = false
        val devices = bluetoothHeadset!!.connectedDevices
        if (devices.size > 0) {
            bluetoothDevice = devices[0]
            if (bluetoothHeadset!!.isAudioConnected(bluetoothDevice)) {
                d(TAG, "SCO connected with ${bluetoothDevice!!.name}")
                scoConnected = true
            } else {
                d(TAG, "SCO is not connected with ${bluetoothDevice!!.name}")
            }
        }

        if (scoConnected) {
            bluetoothState = State.SCO_CONNECTED
            scoConnectionAttempts = 0
        } else {
            w(TAG, "BT failed to connect after timeout")
            stopScoAudio()
        }
        updateAudioDeviceState()
        d(TAG, "bluetoothTimeout done: BT state=$bluetoothState")
    }

    private fun isScoOn(): Boolean {
        return audioManager.isBluetoothScoOn
    }

    private fun stateToString(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_DISCONNECTED -> "DISCONNECTED"
            BluetoothAdapter.STATE_CONNECTED -> "CONNECTED"
            BluetoothAdapter.STATE_CONNECTING -> "CONNECTING"
            BluetoothAdapter.STATE_DISCONNECTING -> "DISCONNECTING"
            BluetoothAdapter.STATE_OFF -> "OFF"
            BluetoothAdapter.STATE_ON -> "ON"
            BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
            BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
            else -> "INVALID"
        }
    }

    companion object {
        private const val TAG = "RTCBluetoothManager"
        private const val BLUETOOTH_SCO_TIMEOUT_MS = 4000L
        private const val MAX_SCO_CONNECTION_ATTEMPTS = 2

        fun create(context: Context, audioManager: RTCAudioManager): RTCBluetoothManager {
            i(TAG, "create ${RTCUtils.getThreadInfo()}")
            return RTCBluetoothManager(context, audioManager)
        }
    }
}
