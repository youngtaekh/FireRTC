package kr.young.examplewebrtc.util

import android.Manifest
import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kr.young.common.PermissionUtil
import kr.young.common.UtilLog.Companion.d

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
        d(TAG, "onResume")
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

    companion object {
        private const val TAG = "BaseActivity"
    }
}