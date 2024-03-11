package kr.young.firertc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_BIOMETRIC_ENROLL
import android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kr.young.common.TouchEffect
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.databinding.ActivitySignBinding
import kr.young.firertc.repo.AppSP
import kr.young.firertc.util.BaseActivity
import kr.young.firertc.util.ResponseCode.Companion.WRONG_PASSWORD
import kr.young.firertc.util.ResponseCode.Companion.WRONG_PASSWORD_TEXT
import kr.young.firertc.vm.MyDataViewModel
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SignActivity : BaseActivity(), OnTouchListener, OnClickListener {
    private lateinit var binding: ActivitySignBinding
    private lateinit var vm: MyDataViewModel

    private lateinit var promptBuilder: BiometricPrompt.PromptInfo.Builder
    private lateinit var biometricPrompt: BiometricPrompt
    private var promptInfo: BiometricPrompt.PromptInfo? = null

    private fun checkAvailableAuth() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                //  생체 인증 가능
                d(TAG, "BIOMETRIC_SUCCESS")
                authRun()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                //  기기에서 생체 인증을 지원하지 않는 경우
                d(TAG, "BIOMETRIC_ERROR_NO_HARDWARE")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                d(TAG, "BIOMETRIC_ERROR_HW_UNAVAILABLE")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                //  생체 인식 정보가 등록되지 않은 경우
                d(TAG, "BIOMETRIC_ERROR_NONE_ENROLLED")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val enrollIntent = Intent(ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                    }
                    authLauncher.launch(enrollIntent)
                }
            }
            else -> {
                //   기타 실패
                d(TAG, "else")
            }
        }
    }

    private fun bioAuthenticate() {
        promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app(수정 가능)")
            .setSubtitle("Log in using your biometric credential(수정 가능)")
            .setNegativeButtonText("Cancel(수정 가능)")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
//            .setConfirmationRequired(false)

        promptInfo =  promptBuilder.build()

        biometricPrompt = BiometricPrompt(this, object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                d(TAG, "error $errorCode $errString")
                Toast.makeText(this@SignActivity, "Fingerprint error $errorCode $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                d(TAG, "succeed")
                binding.etId.setText(AppSP.instance.getUserId())
                binding.etPassword.setText(AppSP.instance.getUserPwd())
                start()
                Toast.makeText(this@SignActivity, "Fingerprint Success", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                d(TAG, "failed")
                Toast.makeText(this@SignActivity, "Fingerprint Failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun authRun() {
        bioAuthenticate()
        promptInfo?.let {
            biometricPrompt.authenticate(it)

//            val cipher = getCipher()
//            getSecretKey()?.let {secretKey ->
//                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
//
//                biometricPrompt.authenticate(it, BiometricPrompt.CryptoObject(cipher))
//            }
        }
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        keyStore.getKey(KEY_NAME, null)?.let {
            return it as SecretKey
        }
        return null
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign)
        vm = MyDataViewModel.instance

        binding.tvStart.setOnTouchListener(this)
        binding.tvStart.setOnClickListener(this)
        binding.ivFinger.setOnTouchListener(this)
        binding.ivFinger.setOnClickListener(this)

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                start()
            }
            false
        }

        if (!AppSP.instance.getUserId().isNullOrEmpty()) {
            binding.etId.setText(AppSP.instance.getUserId())
        }

        d(TAG, "userId ${AppSP.instance.getUserId().isNullOrEmpty()}")
        d(TAG, "userPwd ${AppSP.instance.getUserPwd().isNullOrEmpty()}")
        binding.ivFinger.visibility =
            if (AppSP.instance.getUserId().isNullOrEmpty() || AppSP.instance.getUserPwd().isNullOrEmpty()) {
                INVISIBLE
            } else {
                VISIBLE
            }

        vm.responseCode.observe(this, codeObserver)
        vm.isSigned.observe(this, signedObserver)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (v!!.id) {
            R.id.tv_start, R.id.iv_finger -> { TouchEffect.alpha(v, event) }
        }
        return super.onTouchEvent(event)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_start -> { start() }
            R.id.iv_finger -> checkAvailableAuth()
        }
    }

    private fun start() {
        vm.checkMyData(binding.etId.text.toString(), binding.etPassword.text.toString())
    }

    private val codeObserver = Observer<Int> {
        when (it) {
            WRONG_PASSWORD -> {binding.tvWarning.text = WRONG_PASSWORD_TEXT}
        }
    }

    private val signedObserver = Observer<Boolean> {
        if (it) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            d(TAG, "result ok")
            bioAuthenticate()
        } else if (result.resultCode == RESULT_CANCELED) {
            d(TAG, "result canceled")
        }
    }

    companion object {
        private const val TAG = "SignActivity"
        private const val KEY_NAME = "keyNameTest123123"
    }
}