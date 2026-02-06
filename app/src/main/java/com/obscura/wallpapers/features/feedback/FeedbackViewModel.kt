package com.obscura.wallpapers.features.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "FeedbackViewModel"
        private const val APP_PACKAGE_NAME = "com.obscura.wallpapers"
        private const val FEEDBACK_EMAIL = "support@obscura.com"
        private const val FIRESTORE_COLLECTION = "feedback"
    }

    private val firestore = FirebaseFirestore.getInstance()

    private val _feedbackText = MutableStateFlow("")
    val feedbackText: StateFlow<String> = _feedbackText.asStateFlow()

    private val _contactInfo = MutableStateFlow("")
    val contactInfo: StateFlow<String> = _contactInfo.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitResult = MutableStateFlow<SubmitResult?>(null)
    val submitResult: StateFlow<SubmitResult?> = _submitResult.asStateFlow()

    private val _shouldNavigateBack = MutableStateFlow(false)
    val shouldNavigateBack: StateFlow<Boolean> = _shouldNavigateBack.asStateFlow()

    fun updateFeedbackText(text: String) {
        _feedbackText.value = text
    }

    fun updateContactInfo(info: String) {
        _contactInfo.value = info
    }

    fun submitFeedback() {
        if (_feedbackText.value.isBlank()) {
            _submitResult.value = SubmitResult.Error("请输入反馈内容")
            return
        }

        viewModelScope.launch {
            try {
                _isSubmitting.value = true

                val feedback = hashMapOf(
                    "content" to _feedbackText.value,
                    "contactInfo" to _contactInfo.value,
                    "timestamp" to Date(),
                    "deviceInfo" to android.os.Build.MODEL,
                    "appVersion" to context.packageManager.getPackageInfo(context.packageName, 0).versionName
                )

                firestore.collection(FIRESTORE_COLLECTION)
                    .add(feedback)
                    .await()

                Log.d(TAG, "Feedback submitted to Firestore successfully")

                _submitResult.value = SubmitResult.Success("反馈提交成功，感谢您的宝贵意见！")
                _feedbackText.value = ""
                _contactInfo.value = ""
                _shouldNavigateBack.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting feedback to Firestore: ${e.message}")
                _submitResult.value = SubmitResult.Error("提交失败，请稍后再试")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearSubmitResult() {
        _submitResult.value = null
    }

    fun resetNavigationState() {
        _shouldNavigateBack.value = false
    }

    fun openAppRating(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$APP_PACKAGE_NAME")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$APP_PACKAGE_NAME")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error opening app rating: ${e.message}")
                false
            }
        }
    }

    fun sendEmailFeedback(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$FEEDBACK_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "Vistara壁纸应用反馈")
                putExtra(Intent.EXTRA_TEXT, "我想反馈以下问题：\n\n")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email feedback: ${e.message}")
            false
        }
    }
}

sealed class SubmitResult {
    data class Success(val message: String) : SubmitResult()
    data class Error(val message: String) : SubmitResult()
}
