package com.nikita_frolov_cr.downloadvideo

import android.annotation.SuppressLint
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Pair
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.offline.FilteringManifestParser
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.source.dash.manifest.RepresentationKey
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.DebugTextViewHelper
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity() {

    private var trackSelector: DefaultTrackSelector? = null
    private var player: SimpleExoPlayer? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var mediaSource: MediaSource? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView.setControllerVisibilityListener { debugRootView.visibility = it }
        playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView.requestFocus()
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {

        val drmSessionManager = DefaultDrmSessionManager(
                MainActivity.UUID,
                FrameworkMediaDrm.newInstance(MainActivity.UUID),
                OfflineMediaDrmCallback(DemoApplication.offlineLicense),
                null, false)

        drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, DemoApplication.offlineLicenseKeySetId)

        trackSelector = DefaultTrackSelector(AdaptiveTrackSelection.Factory(DefaultBandwidthMeter()))
        trackSelector?.parameters = DefaultTrackSelector.ParametersBuilder().build()

        player = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this,
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
                trackSelector, drmSessionManager)
        player?.addAnalyticsListener(EventLogger(trackSelector))
        playerView.player = player

        debugViewHelper = DebugTextViewHelper(player, debugTextView)
        debugViewHelper?.start()

        mediaSource = DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(buildDataSourceFactory(true)),
                buildDataSourceFactory(false))
                .setManifestParser(
                        FilteringManifestParser(
                                DashManifestParser(), getOfflineStreamKeys(MainActivity.URI) as List<RepresentationKey>))
                .createMediaSource(MainActivity.URI)

        player?.prepare(mediaSource)

    }

    private fun releasePlayer() {
        if (player != null) {
            debugViewHelper?.stop()
            debugViewHelper = null
            player?.release()
            player = null
            mediaSource = null
            trackSelector = null
        }
    }

    private fun getOfflineStreamKeys(uri: Uri): List<RepresentationKey>? {
        return (application as DemoApplication).getDownloadTracker()?.getOfflineStreamKeys(uri)
    }

    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        return (application as DemoApplication)
                .buildDataSourceFactory(if (useBandwidthMeter) DefaultBandwidthMeter() else null)
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<ExoPlaybackException> {

        @SuppressLint("StringFormatInvalid")
        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            var errorString = getString(R.string.error_generic)
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    errorString = if (cause.decoderName == null) {
                        when {
                            cause.cause is MediaCodecUtil.DecoderQueryException -> getString(R.string.error_querying_decoders)
                            cause.secureDecoderRequired -> getString(R.string.error_no_secure_decoder, cause.mimeType)
                            else -> getString(R.string.error_no_decoder, cause.mimeType)
                        }
                    } else {
                        getString(R.string.error_instantiating_decoder, cause.decoderName)
                    }
                }
            }
            return Pair.create(0, errorString)
        }
    }
}
