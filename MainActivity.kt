package com.nikita_frolov_cr.downloadvideo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Pair
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.offline.FilteringManifestParser
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DashUtil
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
import com.nikita_frolov_cr.downloadvideo.RxUtils.ioToMain
import io.reactivex.Flowable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), PlayerControlView.VisibilityListener, PlaybackPreparer {

    private var trackSelector: DefaultTrackSelector? = null
    private var player: SimpleExoPlayer? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var mediaDataSourceFactory: DataSource.Factory? = null
    private var mediaSource: MediaSource? = null

    companion object {

        private val URI = Uri.parse("https://link.theplatform.eu/s/jGxigC/DO_0HEeyJy0D?token=Aypf0xzQ6hiAEwn0ffqK8bDIIED8kGCG")
        private val URI_1 = Uri.parse("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd")
        private val URI_2 = Uri.parse("https://unified.cdn.bbaws.net/i/410/4/DI_0419_orig.ism/master.m3u8?filter=type!%3D%22audio%22%7C%7Cchannels%3E%3D2")

        private val UUID = Util.getDrmUuid("widevine")

        private val LICENSE_URL = "https://widevine.entitlement.theplatform.eu/wv/web/ModularDrm?form=json&schema=1.0&account=http://access.auth.theplatform.com/data/Account/2693468579&token=Aypf0xzQ6hiAEwn0ffqK8bDIIED8kGCG"
        private val LICENSE_URL_1 = "https://proxy.uat.widevine.com/proxy?video_id=48fcc369939ac96c&provider=widevine_test"
        private val LICENSE_URL_2 = ""

    }

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

    private fun initializePlayer() {

        val licenseDataSourceFactory = (application as DemoApplication).buildHttpDataSourceFactory(null)
//        val callback = HttpMediaDrmCallback(LICENSE_URL, licenseDataSourceFactory)


        val callback = MPXHttpMediaDrmCallback(LICENSE_URL, licenseDataSourceFactory, "DO_0HEeyJy0D")

        val mediaDrm = FrameworkMediaDrm.newInstance(UUID)
        val drmSessionManager = DefaultDrmSessionManager(
                UUID,
                mediaDrm,
                callback,
                null, false)

        val offlineLicenseHelper = OfflineLicenseHelper(UUID, mediaDrm, callback, null)

        val dataSource = licenseDataSourceFactory.createDataSource()

        Flowable.fromCallable { URI }
                .map { DashUtil.loadManifest(dataSource, it).getPeriod(0) }
                .map { DashUtil.loadDrmInitData(dataSource, it) }
                .map { offlineLicenseHelper.downloadLicense(it) }
                .compose { ioToMain(it) }
                .subscribe {

                    drmSessionManager.setMode(DefaultDrmSessionManager.MODE_DOWNLOAD, it)


                    trackSelector = DefaultTrackSelector(AdaptiveTrackSelection.Factory(DefaultBandwidthMeter()))
                    trackSelector?.parameters = DefaultTrackSelector.ParametersBuilder().build()

                    player = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this,
                            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
                            trackSelector, drmSessionManager)
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
                                            DashManifestParser(), getOfflineStreamKeys(URI) as List<RepresentationKey>))
                            .createMediaSource(URI)



                    player?.prepare(mediaSource)

                }


//
//        val drmSessionManager = OfflineDrmSessionManager.newFrameworkInstance(
//                UUID,
//                callback,
//                null,
//                Handler(),
//                null,
//                true,
//                getExternalFilesDir(null).toString() + "/files"
//                )


    }

//    val dataSource = licenseDataSourceFactory.createDataSource()

//    Flowable.fromCallable { URI }
//    .map { DashUtil.loadManifest(dataSource, it).getPeriod(0) }
//    .map { DashUtil.loadDrmInitData(dataSource, it) }
//    .map { offlineLicenseHelper.downloadLicense(it) }
//    .compose { ioToMain(it) }
//    .subscribe {
//        drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, it)
//
//        trackSelector = DefaultTrackSelector(AdaptiveTrackSelection.Factory(DefaultBandwidthMeter()))
//        trackSelector?.parameters = DefaultTrackSelector.ParametersBuilder().build()
//
//        player = ExoPlayerFactory.newSimpleInstance(
//                DefaultRenderersFactory(this, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
//                trackSelector,
//                drmSessionManager)
//        player?.addAnalyticsListener(EventLogger(trackSelector))
//        playerView.player = player
//        playerView.setPlaybackPreparer(this)
//        debugViewHelper = DebugTextViewHelper(player, debugTextView)
//        debugViewHelper?.start()
//
//
//        mediaSource = DashMediaSource.Factory(
//                DefaultDashChunkSource.Factory(mediaDataSourceFactory),
//                buildDataSourceFactory(false))
//                .setManifestParser(
//                        FilteringManifestParser<DashManifest, RepresentationKey>(
//                                DashManifestParser(), getOfflineStreamKeys(URI) as List<RepresentationKey>))
//                .createMediaSource(URI)
//
//
//        player?.prepare(mediaSource, false, false)
//    }

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
