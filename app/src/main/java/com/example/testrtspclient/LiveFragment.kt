package com.example.testrtspclient

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alexvas.rtsp.codec.VideoDecodeThread
import com.alexvas.rtsp.widget.RtspDataListener
import com.alexvas.rtsp.widget.RtspImageView
import com.alexvas.rtsp.widget.RtspStatusListener
import com.alexvas.rtsp.widget.toHexString
import com.example.testrtspclient.databinding.FragmentLiveBinding
import kotlin.math.min

class LiveFragment : Fragment() {

    private lateinit var binding: FragmentLiveBinding

    private val rtspDataListener = object : RtspDataListener {
        override fun onRtspDataApplicationDataReceived(data: ByteArray, offset: Int, length: Int, timestamp: Long) {
            val numBytesDump = min(length, 25) // dump max 25 bytes
            Log.i(TAG, "RTSP app data ($length bytes): ${data.toHexString(offset, offset + numBytesDump)}")
        }
    }

    private val rtspStatusImageListener = object : RtspStatusListener {
        override fun onRtspStatusConnecting() {
            if (DEBUG) Log.v(TAG, "onRtspStatusConnecting()")
            binding.apply {
                tvStatusImage.text = "RTSP connecting"
                pbLoadingImage.visibility = View.VISIBLE
                vShutterImage.visibility = View.VISIBLE
            }
        }

        override fun onRtspStatusConnected() {
            if (DEBUG) Log.v(TAG, "onRtspStatusConnected()")
            binding.apply {
                tvStatusImage.text = "RTSP connected"
                bnStartStopImage.text = "Stop RTSP"
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspStatusDisconnecting() {
            if (DEBUG) Log.v(TAG, "onRtspStatusDisconnecting()")
            binding.apply {
                tvStatusImage.text = "RTSP disconnecting"
            }
        }

        override fun onRtspStatusDisconnected() {
            if (DEBUG) Log.v(TAG, "onRtspStatusDisconnected()")
            binding.apply {
                tvStatusImage.text = "RTSP disconnected"
                bnStartStopImage.text = "Start RTSP"
                pbLoadingImage.visibility = View.GONE
                vShutterImage.visibility = View.VISIBLE
                pbLoadingImage.isEnabled = false
            }
        }

        override fun onRtspStatusFailedUnauthorized() {
            if (DEBUG) Log.e(TAG, "onRtspStatusFailedUnauthorized()")
            if (context == null) return
            onRtspStatusDisconnected()
            binding.apply {
                tvStatusImage.text = "RTSP username or password invalid"
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspStatusFailed(message: String?) {
            if (DEBUG) Log.e(TAG, "onRtspStatusFailed(message='$message')")
            if (context == null) return
            onRtspStatusDisconnected()
            binding.apply {
                tvStatusImage.text = "Error: $message"
                pbLoadingImage.visibility = View.GONE
            }
        }

        override fun onRtspFirstFrameRendered() {
            if (DEBUG) Log.v(TAG, "onRtspFirstFrameRendered()")
            binding.apply {
                vShutterImage.visibility = View.GONE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (DEBUG) Log.v(TAG, "onCreateView()")

        binding = FragmentLiveBinding.inflate(inflater, container, false)

        binding.ivVideoImage.setStatusListener(rtspStatusImageListener)
        binding.ivVideoImage.setDataListener(rtspDataListener)

        binding.ivVideoImage.videoRotation = 180
        binding.ivVideoImage.scaleX = -1f
        binding.ivVideoImage.videoDecoderType = VideoDecodeThread.DecoderType.HARDWARE

        binding.bnStartStopImage.setOnClickListener {
            if (binding.ivVideoImage.isStarted()) {
                binding.ivVideoImage.stop()
            } else {
                val uri = Uri.parse("rtsp://192.168.8.131:1935")
                binding.ivVideoImage.apply {
                    init(uri, "", "", "rtsp-client-android")
                    debug = false
                    onRtspImageBitmapListener = object : RtspImageView.RtspImageBitmapListener {
                        override fun onRtspImageBitmapObtained(bitmap: Bitmap) {
                            // TODO: You can send bitmap for processing
                        }
                    }
                    start(requestVideo = true, requestAudio = false, requestApplication = false)
                }
            }
        }
        return binding.root
    }

    override fun onResume() {
        if (DEBUG) Log.v(TAG, "onResume()")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    companion object {
        private val TAG: String = LiveFragment::class.java.simpleName
        private const val DEBUG = true
    }

}