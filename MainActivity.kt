package com.nikita_frolov_cr.downloadvideo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Pair
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.offline.FilteringManifestParser
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.source.dash.manifest.RepresentationKey
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.DebugTextViewHelper
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), PlayerControlView.VisibilityListener, PlaybackPreparer {

    companion object {
    }

    private var trackSelector: DefaultTrackSelector? = null
    private var player: SimpleExoPlayer? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var mediaDataSourceFactory: DataSource.Factory? = null
    private var mediaSource: MediaSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        val downloadTracker = (application as DemoApplication).getDownloadTracker()
//        downloadTracker?.toggleDownload(this, "Download",
//                Uri.parse("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd"),
//                null)

        mediaDataSourceFactory = buildDataSourceFactory(true)


        playerView.setControllerVisibilityListener(this)
        playerView.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView.requestFocus()


    }

    override fun onVisibilityChange(visibility: Int) {
        debugRootView.visibility = visibility
    }

    override fun preparePlayback() {
        initializePlayer()
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

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<ExoPlaybackException> {

        @SuppressLint("StringFormatInvalid")
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
                            errorString = getString(R.string.error_no_secure_decoder, cause.mimeType)
                        } else {
                            errorString = getString(R.string.error_no_decoder, cause.mimeType)
                        }
                    } else {
                        errorString = getString(R.string.error_instantiating_decoder, cause.decoderName)
                    }
                }
            }
            return Pair.create(0, errorString)
        }
    }


    private fun initializePlayer() {

        val uri = Uri.parse("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears.mpd")

        if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
            // The player will be reinitialized if the permission is granted.
            return
        }

        val drmSessionManager = buildDrmSessionManagerV18(
                Util.getDrmUuid("widevine"),
                "", null, false)


        val trackSelectionFactory = AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())


        val renderersFactory = DefaultRenderersFactory(this, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        trackSelector = DefaultTrackSelector(trackSelectionFactory)
        trackSelector?.parameters = DefaultTrackSelector.ParametersBuilder().build()

        player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, drmSessionManager)
        player?.addAnalyticsListener(EventLogger(trackSelector))
        playerView.player = player
        playerView.setPlaybackPreparer(this)
        debugViewHelper = DebugTextViewHelper(player, debugTextView)
        debugViewHelper?.start()


        mediaSource = DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                buildDataSourceFactory(false))
                .setManifestParser(
                        FilteringManifestParser<DashManifest, RepresentationKey>(
                                DashManifestParser(), getOfflineStreamKeys(uri) as List<RepresentationKey>))
                .createMediaSource(uri)


        player?.prepare(mediaSource, false, false)
    }

    @Throws(UnsupportedDrmException::class)
    private fun buildDrmSessionManagerV18(
            uuid: UUID, licenseUrl: String, keyRequestPropertiesArray: Array<String>?, multiSession: Boolean): DefaultDrmSessionManager<FrameworkMediaCrypto> {
        val licenseDataSourceFactory = (application as DemoApplication).buildHttpDataSourceFactory(/* listener= */null)
        val drmCallback = HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory)
        if (keyRequestPropertiesArray != null) {
            var i = 0
            while (i < keyRequestPropertiesArray.size - 1) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                        keyRequestPropertiesArray[i + 1])
                i += 2
            }
        }
        return DefaultDrmSessionManager(
                uuid, FrameworkMediaDrm.newInstance(uuid), drmCallback, null, multiSession)
    }

    private fun getOfflineStreamKeys(uri: Uri): List<RepresentationKey>? {
        return (application as DemoApplication).getDownloadTracker()?.getOfflineStreamKeys(uri)
    }

    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        return (application as DemoApplication)
                .buildDataSourceFactory(if (useBandwidthMeter) DefaultBandwidthMeter() else null)
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

}
