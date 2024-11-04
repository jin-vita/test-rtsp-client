package com.example.testrtspclient

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.alexvas.rtsp.codec.VideoDecodeThread
import com.alexvas.rtsp.widget.RtspDataListener
import com.alexvas.rtsp.widget.RtspImageView
import com.alexvas.rtsp.widget.RtspStatusListener
import com.alexvas.rtsp.widget.toHexString
import com.example.testrtspclient.databinding.FragmentLiveBinding
import java.util.Timer
import kotlin.math.min

class LiveFragment : Fragment() {

    private lateinit var binding: FragmentLiveBinding
    private lateinit var liveViewModel: LiveViewModel

    private var statisticsTimer: Timer? = null

    private val rtspDataListener = object: RtspDataListener {
        override fun onRtspDataApplicationDataReceived(data: ByteArray, offset: Int, length: Int, timestamp: Long) {
            val numBytesDump = min(length, 25) // dump max 25 bytes
            Log.i(TAG, "RTSP app data ($length bytes): ${data.toHexString(offset, offset + numBytesDump)}")
        }
    }

    private val rtspStatusImageListener = object: RtspStatusListener {
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
            setKeepScreenOn(true)
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
            setKeepScreenOn(false)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (DEBUG) Log.v(TAG, "onCreateView()")

        liveViewModel = ViewModelProvider(this)[LiveViewModel::class.java]
        binding = FragmentLiveBinding.inflate(inflater, container, false)

        binding.bnVideoDecoderGroup.check(binding.bnVideoDecoderHardware.id)

        binding.ivVideoImage.setStatusListener(rtspStatusImageListener)
        binding.ivVideoImage.setDataListener(rtspDataListener)

        liveViewModel.initEditTexts(
            binding.llRtspParams.etRtspRequest,
            binding.llRtspParams.etRtspUsername,
            binding.llRtspParams.etRtspPassword
        )

        liveViewModel.rtspRequest.observe(viewLifecycleOwner) {
            if (binding.llRtspParams.etRtspRequest.text.toString() != it)
                binding.llRtspParams.etRtspRequest.setText(it)
        }
        liveViewModel.rtspUsername.observe(viewLifecycleOwner) {
            if (binding.llRtspParams.etRtspUsername.text.toString() != it)
                binding.llRtspParams.etRtspUsername.setText(it)
        }
        liveViewModel.rtspPassword.observe(viewLifecycleOwner) {
            if (binding.llRtspParams.etRtspPassword.text.toString() != it)
                binding.llRtspParams.etRtspPassword.setText(it)
        }

        binding.bnRotate0.setOnClickListener {
            binding.ivVideoImage.videoRotation = 0
        }

        binding.bnRotate90.setOnClickListener {
            binding.ivVideoImage.videoRotation = 90
        }

        binding.bnRotate180.setOnClickListener {
            binding.ivVideoImage.videoRotation = 180
        }

        binding.bnRotate270.setOnClickListener {
            binding.ivVideoImage.videoRotation = 270
        }

        binding.bnRotate0.performClick()

        binding.bnVideoDecoderHardware.setOnClickListener {
            binding.ivVideoImage.videoDecoderType = VideoDecodeThread.DecoderType.HARDWARE
        }

        binding.bnVideoDecoderSoftware.setOnClickListener {
            binding.ivVideoImage.videoDecoderType = VideoDecodeThread.DecoderType.SOFTWARE
        }

        binding.bnStartStopImage.setOnClickListener {
            if (binding.ivVideoImage.isStarted()) {
                binding.ivVideoImage.stop()
                stopStatistics()
            } else {
                val uri = Uri.parse(liveViewModel.rtspRequest.value)
                binding.ivVideoImage.apply {
                    init(uri, liveViewModel.rtspUsername.value, liveViewModel.rtspPassword.value, "rtsp-client-android")
                    debug = binding.llRtspParams.cbDebug.isChecked
                    onRtspImageBitmapListener = object : RtspImageView.RtspImageBitmapListener {
                        override fun onRtspImageBitmapObtained(bitmap: Bitmap) {
                            // TODO: You can send bitmap for processing
                        }
                    }
                    start(
                        requestVideo = binding.llRtspParams.cbVideo.isChecked,
                        requestAudio = binding.llRtspParams.cbAudio.isChecked,
                        requestApplication = binding.llRtspParams.cbApplication.isChecked
                    )
                }
            }
        }
        return binding.root
    }

    override fun onResume() {
        if (DEBUG) Log.v(TAG, "onResume()")
        super.onResume()
        liveViewModel.loadParams(requireContext())
    }

    override fun onPause() {
        super.onPause()
        liveViewModel.saveParams(requireContext())
    }

    private fun stopStatistics() {
        if (DEBUG) Log.v(TAG, "stopStatistics()")
        statisticsTimer?.apply {
            Log.i(TAG, "Stop statistics")
            cancel()
        }
        statisticsTimer = null
    }

    private fun setKeepScreenOn(enable: Boolean) {
        if (DEBUG) Log.v(TAG, "setKeepScreenOn(enable=$enable)")
        if (enable) {
            activity?.apply {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.i(TAG, "Enabled keep screen on")
            }
        } else {
            activity?.apply {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.i(TAG, "Disabled keep screen on")
            }
        }
    }
    companion object {
        private val TAG: String = LiveFragment::class.java.simpleName
        private const val DEBUG = true
    }

}