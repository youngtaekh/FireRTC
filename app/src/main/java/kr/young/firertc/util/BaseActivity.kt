package kr.young.firertc.util

import android.Manifest
import android.Manifest.permission.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kr.young.common.PermissionUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.ReceiveActivity
import kr.young.firertc.model.Call
import kr.young.firertc.observer.CallSignal
import kr.young.firertc.observer.CallSignalImpl
import kr.young.firertc.vm.CallVM

open class BaseActivity: AppCompatActivity(), CallSignal.Observer {
    private var hasPermission = false
    private var requesting = false
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(BLUETOOTH_CONNECT, CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(BLUETOOTH_CONNECT, CAMERA, RECORD_AUDIO)
    } else {
        arrayOf(CAMERA, RECORD_AUDIO)
    }

    override fun onResume() {
        super.onResume()
        d(TAG, "onResume")
        CallSignalImpl.instance.add(this)
        launchReceiveActivity()
        if (!requesting) {
            checkPermission()
        } else {
            requesting = false
        }
    }

    override fun onPause() {
        super.onPause()
        d(TAG, "onPause")
        CallSignalImpl.instance.remove(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtil.REQUEST_CODE && grantResults.isNotEmpty()) {
            hasPermission = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    hasPermission = false
                    break
                }
            }
        }
    }

    private fun launchReceiveActivity() {
        d(TAG, "launchReceiveActivity")
        val call = CallVM.instance.call
        if (call != null && call.direction == Call.Direction.Answer && !call.connected && !call.terminated) {
            val intent = Intent(this, ReceiveActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun checkPermission() {
        if (!PermissionUtil.check(permissions = permissions)) {
            requesting = true
            PermissionUtil.request(this, permissions)
        } else {
            hasPermission = true
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                d(TAG, "notification permission granted")
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                d(TAG, "shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)")
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            d(TAG, "notification permission granted")
        }
    }

    override fun onIncomingCall() {
        d(TAG, "onIncomingCall")
        launchReceiveActivity()
    }

    override fun onAnswerCall(sdp: String) {
        d(TAG, "onAnswerCall")
    }

    override fun onTerminatedCall() {
        d(TAG, "onTerminatedCall")
    }

    companion object {
        private const val TAG = "BaseActivity"
    }
}