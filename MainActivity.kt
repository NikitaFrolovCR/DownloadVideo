package com.nikita_frolov_cr.downloadvideo

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Pair
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), PlayerControlView.VisibilityListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val downloadTracker = (application as DemoApplication).getDownloadTracker()
        downloadTracker?.toggleDownload(this, "Download",
                Uri.parse("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd"),
                null)


        playerView.setControllerVisibilityListener(this)
        playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView.requestFocus()


    }

    override fun onVisibilityChange(visibility: Int) {
        debugRootView.visibility = visibility
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<ExoPlaybackException> {

        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            var errorString = getString(R.string.error_generic)
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    if (cause.decoderName == null) {
                        if (cause.cause is MediaCodecUtil.DecoderQueryException) {
                            errorString = getString(R.string.error_querying_decoders)
                        } else if (cause.secureDecoderRequired) {
                            errorString = getString(
                                    R.string.error_no_secure_decoder, cause.mimeType)
                        } else {
                            errorString = getString(R.string.error_no_decoder, cause.mimeType)
                        }
                    } else {
                        errorString = getString(
                                R.string.error_instantiating_decoder,
                                cause.decoderName)
                    }
                }
            }
            return Pair.create(0, errorString)
        }
    }


}
