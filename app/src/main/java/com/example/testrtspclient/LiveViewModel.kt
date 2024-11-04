package com.example.testrtspclient

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.apply
import kotlin.jvm.java
import kotlin.toString

private const val RTSP_REQUEST_KEY = "rtsp_request"
private const val RTSP_USERNAME_KEY = "rtsp_username"
private const val RTSP_PASSWORD_KEY = "rtsp_password"

private const val DEFAULT_RTSP_REQUEST = "rtsp://192.168.8.131:1935"
private const val DEFAULT_RTSP_USERNAME = ""
private const val DEFAULT_RTSP_PASSWORD = ""

private const val LIVE_PARAMS_FILENAME = "live_params"

@SuppressLint("LogNotTimber")
class LiveViewModel : ViewModel() {

    val rtspRequest = MutableLiveData<String>().apply {
        value = DEFAULT_RTSP_REQUEST
    }
    val rtspUsername = MutableLiveData<String>().apply {
        value = DEFAULT_RTSP_USERNAME
    }
    val rtspPassword = MutableLiveData<String>().apply {
        value = DEFAULT_RTSP_PASSWORD
    }

    fun loadParams(context: Context) {
        if (DEBUG) Log.v(TAG, "loadParams()")
        val pref = context.getSharedPreferences(LIVE_PARAMS_FILENAME, Context.MODE_PRIVATE)
        try {
            rtspRequest.value = pref.getString(RTSP_REQUEST_KEY, DEFAULT_RTSP_REQUEST)
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
        try {
            rtspUsername.value = pref.getString(RTSP_USERNAME_KEY, DEFAULT_RTSP_USERNAME)
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
        try {
            rtspPassword.value = pref.getString(RTSP_PASSWORD_KEY, DEFAULT_RTSP_PASSWORD)
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    fun saveParams(context: Context) {
        if (DEBUG) Log.v(TAG, "saveParams()")
        context.getSharedPreferences(LIVE_PARAMS_FILENAME, Context.MODE_PRIVATE).edit().apply {
            putString(RTSP_REQUEST_KEY, rtspRequest.value)
            putString(RTSP_USERNAME_KEY, rtspUsername.value)
            putString(RTSP_PASSWORD_KEY, rtspPassword.value)
            apply()
        }
    }

    fun initEditTexts(etRtspRequest: EditText, etRtspUsername: EditText, etRtspPassword: EditText) {
        if (DEBUG) Log.v(TAG, "initEditTexts()")
        etRtspRequest.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text != rtspRequest.value) {
                    rtspRequest.value = text
                }
            }
        })
        etRtspUsername.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text != rtspUsername.value) {
                    rtspUsername.value = text
                }
            }
        })
        etRtspPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text != rtspPassword.value) {
                    rtspPassword.value = text
                }
            }
        })
    }

    companion object {
        private val TAG: String = LiveViewModel::class.java.simpleName
        private const val DEBUG = false


    }

}