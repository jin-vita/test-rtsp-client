package com.example.testrtspclient

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.alexvas.rtsp.codec.VideoDecodeThread
import com.alexvas.rtsp.widget.RtspImageView
import com.alexvas.rtsp.widget.RtspStatusListener
import com.example.testrtspclient.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
        private const val DEBUG = true
        private const val TIMEOUT_DURATION = 5000L

        private const val IP = "192.168."
    }

    private val handler by lazy { Handler(mainLooper) }
    private val handlerTimeOut by lazy { Handler(mainLooper) }

    override fun onResume() {
        if (DEBUG) Log.v(TAG, "onResume()")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
    }

    private fun startVideo() {
        val ip = binding.ipInput.text
        if (ip.split(".").size != 4) {
            toast("IP 입력 필수")
            return
        }
        val url = "rtsp://$ip:1935"
        val uri = Uri.parse(url)
        binding.ivVideoImage.apply {
            init(uri, "", "", "")
            debug = false
            onRtspImageBitmapListener = object : RtspImageView.RtspImageBitmapListener {
                override fun onRtspImageBitmapObtained(bitmap: Bitmap) {
                    resetTimeout() // 비트맵이 수신될 때마다 타이머 리셋
                }
            }
            start(requestVideo = true, requestAudio = false, requestApplication = false)
        }
    }

    private fun stopVideo() {
        if (DEBUG) Log.e(TAG, "stopVideo called.")
        if (binding.ivVideoImage.isStarted()) {
            handlerTimeOut.removeCallbacksAndMessages(0)
            binding.ivVideoImage.stop()
        }
    }

    private fun resetTimeout() {
        if (binding.ivVideoImage.isStarted()) {
            handlerTimeOut.removeCallbacksAndMessages(0)
            handlerTimeOut.postDelayed({
                if (binding.ivVideoImage.isStarted()) {
                    if (DEBUG) Log.e(TAG, "${TIMEOUT_DURATION / 1000}초 동안 비트맵 수신 없음")
                    stopVideo()
                    startVideo()
                }
            }, TIMEOUT_DURATION)
        }
    }

    private fun initView() = binding.apply {
        ipInput.setText(IP)

        ivVideoImage.setStatusListener(rtspStatusImageListener)
        ivVideoImage.videoDecoderType = VideoDecodeThread.DecoderType.SOFTWARE
        ivVideoImage.videoRotation = 270

        bnStartStopImage.setOnClickListener {
            if (ivVideoImage.isStarted()) ivVideoImage.stop()
            else startVideo()
            it.isEnabled = false
        }
    }

    private val rtspStatusImageListener = object : RtspStatusListener {
        override fun onRtspStatusConnecting() {
            if (DEBUG) Log.v(TAG, "onRtspStatusConnecting()")
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP connecting") }
                pbLoadingImage.visibility = View.VISIBLE
                vShutterImage.visibility = View.VISIBLE
            }
        }

        override fun onRtspStatusConnected() {
            if (DEBUG) Log.v(TAG, "onRtspStatusConnected()")
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP connected") }
                bnStartStopImage.text = buildString { append("Stop RTSP") }
                bnStartStopImage.isEnabled = true
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspStatusDisconnecting() {
            if (DEBUG) Log.v(TAG, "onRtspStatusDisconnecting()")
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP disconnecting") }
            }
        }

        override fun onRtspStatusDisconnected() {
            if (DEBUG) Log.v(TAG, "onRtspStatusDisconnected()")
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP disconnected") }
                bnStartStopImage.text = buildString { append("Start RTSP") }
                bnStartStopImage.isEnabled = true
                pbLoadingImage.visibility = View.GONE
                vShutterImage.visibility = View.VISIBLE
                pbLoadingImage.isEnabled = false
            }
        }

        override fun onRtspStatusFailedUnauthorized() {
            if (DEBUG) Log.e(TAG, "onRtspStatusFailedUnauthorized()")
            onRtspStatusDisconnected()
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP username or password invalid") }
                bnStartStopImage.isEnabled = true
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspStatusFailed(message: String?) {
            if (DEBUG) Log.e(TAG, "onRtspStatusFailed(message='$message')")
            onRtspStatusDisconnected()
            binding.apply {
                tvStatusImage.text = buildString {
                    append("Error: ")
                    append(message)
                }
                bnStartStopImage.text = buildString { append("Start RTSP") }
                bnStartStopImage.isEnabled = true
                pbLoadingImage.visibility = View.GONE
            }

            message?.apply {
                handler.removeCallbacksAndMessages(0)
                handler.postDelayed({
                    stopVideo()
                    startVideo()
                }, TIMEOUT_DURATION)
            }
        }

        override fun onRtspFirstFrameRendered() {
            if (DEBUG) Log.v(TAG, "onRtspFirstFrameRendered()")
            binding.apply {
                vShutterImage.visibility = View.GONE
            }
        }
    }
}