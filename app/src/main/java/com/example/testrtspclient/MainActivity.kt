package com.example.testrtspclient

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.alexvas.rtsp.codec.VideoDecodeThread
import com.alexvas.rtsp.widget.RtspDataListener
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
    private var isRendered = false
    private var count = 0

    override fun onResume() {
        super.onResume()
        if (DEBUG) Log.v(TAG, "onResume()")
    }

    override fun onPause() {
        if (DEBUG) Log.v(TAG, "onPause()")
        super.onPause()
    }

    override fun onStop() {
        if (DEBUG) Log.v(TAG, "onStop()")
        stopVideo()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
    }

    private fun initView() = binding.apply {
        ipInput.setText(IP)

        bnStartStopImage.setOnClickListener {
            if (ivVideoImage.isStarted()) stopVideo()
            else {
                it.isEnabled = false
                startVideo()
            }
        }
    }

    private val rtspDataListener = object : RtspDataListener {
        override fun onRtspDataVideoNalUnitReceived(data: ByteArray, offset: Int, length: Int, timestamp: Long) {
            count++
            if (count > 10000000) count = 1
            if (count % 10 == 1) {
                if (isRendered) resetTimeout()
                if (count % 40 == 1)
                    if (DEBUG) Log.e(TAG, "rtspDataListener onRtspDataVideoNalUnitReceived(). isRendered: $isRendered")
            }
        }
    }

    private val rtspStatusImageListener = object : RtspStatusListener {
        override fun onRtspStatusConnecting() {
            if (DEBUG) Log.v(TAG, "rtspStatusImageListener onRtspStatusConnecting()")
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP connecting") }
                pbLoadingImage.visibility = View.VISIBLE
                vShutterImage.visibility = View.VISIBLE
            }
        }

        override fun onRtspStatusConnected() {
            if (DEBUG) Log.v(TAG, "rtspStatusImageListener onRtspStatusConnected()")
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP connected") }
                bnStartStopImage.text = buildString { append("Stop RTSP") }
                bnStartStopImage.isEnabled = true
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspStatusDisconnecting() {
            if (DEBUG) Log.v(TAG, "rtspStatusImageListener onRtspStatusDisconnecting()")
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP disconnecting") }
            }
        }

        override fun onRtspStatusDisconnected() {
            if (DEBUG) Log.v(TAG, "rtspStatusImageListener onRtspStatusDisconnected()")
            isRendered = false
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
            if (DEBUG) Log.e(TAG, "rtspStatusImageListener onRtspStatusFailedUnauthorized()")
            isRendered = false
            binding.apply {
                tvStatusImage.text = buildString { append("RTSP username or password invalid") }
                bnStartStopImage.isEnabled = true
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspStatusFailed(message: String?) {
            if (DEBUG) Log.e(TAG, "rtspStatusImageListener onRtspStatusFailed(message='$message')")
            isRendered = false
            binding.apply {
                tvStatusImage.text = buildString {
                    append("Error: ")
                    append(message)
                }
                bnStartStopImage.text = buildString { append("Start RTSP") }
                bnStartStopImage.isEnabled = true
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspFirstFrameRendered() {
            if (DEBUG) Log.v(TAG, "rtspStatusImageListener onRtspFirstFrameRendered()")
            isRendered = true
            binding.apply {
                vShutterImage.visibility = View.GONE
            }
        }
    }

    private fun startVideo() {
        if (DEBUG) Log.e(TAG, "startVideo called.")

        handler.removeMessages(0)

        val ip = binding.ipInput.text.trim()
        if (ip.split(".").size != 4) {
            toast("IP 입력 필수")
            binding.bnStartStopImage.isEnabled = true
            return
        }

        val url = "rtsp://$ip:1935"
        val uri = Uri.parse(url)

        binding.ivVideoImage.apply {
            this.stop()
            videoDecoderType = VideoDecodeThread.DecoderType.HARDWARE
            setStatusListener(rtspStatusImageListener)
            setDataListener(rtspDataListener)
            init(uri, "", "", "")
            debug = false
            start(requestVideo = true, requestAudio = false, requestApplication = false)
        }

        resetTimeout()
    }

    private fun stopVideo() {
        if (DEBUG) Log.e(TAG, "stopVideo called.")
        handler.removeMessages(0)
        binding.ivVideoImage.stop()
    }

    private fun resetTimeout() {
        handler.removeMessages(0)
        handler.postDelayed({
            if (DEBUG) Log.e(TAG, "${TIMEOUT_DURATION / 1000}초 동안 비트맵 수신 없음")
            startVideo()
        }, TIMEOUT_DURATION)
    }
}