package kr.young.examplewebrtc.util

import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import kr.young.common.PermissionUtil
import kr.young.common.UtilLog

open class BaseActivity: AppCompatActivity() {
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
        UtilLog.i(TAG, "onResume")
        if (!requesting) {
            checkPermission()
        } else {
            requesting = false
        }
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

    private fun checkPermission() {
        if (!PermissionUtil.check(permissions = permissions)) {
            requesting = true
            PermissionUtil.request(this, permissions)
        } else {
            hasPermission = true
        }
    }

    companion object {
        private const val TAG = "BaseActivity"
    }
}