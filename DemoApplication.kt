package com.nikita_frolov_cr.downloadvideo

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.facebook.stetho.Stetho
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.source.dash.offline.DashDownloadAction
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.util.*

public class DemoApplication : Application() {

    companion object {

        lateinit var instance: DemoApplication
            private set

        private val DOWNLOAD_ACTION_FILE = "actions"
        private val DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions"
        val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
        private val MAX_SIMULTANEOUS_DOWNLOADS = 2
        private const val USER_AGENT = "BlockBuster - Android"
        private const val USER_AGENT_1 = "ExoPlayerDemo"
        private const val PREF_NAME = "pq_prefs.xml"
        private const val OFFLINE_LICENSE_KEY_SET_ID = "OFFLINE_LICENSE_KEY_SET_ID"
        private const val OFFLINE_LICENSE = "OFFLINE_LICENSE"

        lateinit var prefs: SharedPreferences

        var offlineLicenseKeySetId: ByteArray
            get() = prefs.getString(OFFLINE_LICENSE_KEY_SET_ID, null)?.let {
                val split = it.substring(1, it.length - 1).split(", ")
                val array = ByteArray(split.size)
                for (i in split.indices) {
                    array[i] = java.lang.Byte.parseByte(split[i])
                }
                array
            } ?: ByteArray(0)
            set(array) {
                prefs.edit().putString(OFFLINE_LICENSE_KEY_SET_ID, Arrays.toString(array)).apply()
            }

        var offlineLicense: String
            get() = prefs.getString(OFFLINE_LICENSE, "")
            set(license) {
                prefs.edit().putString(OFFLINE_LICENSE, license).apply()
            }
    }

    private var downloadManager: DownloadManager? = null
    private var downloadTracker: DownloadTracker? = null
    private var downloadCache: Cache? = null
    private var downloadDirectory: File? = null
    protected lateinit var userAgent: String

    override fun onCreate() {
        super.onCreate()
        instance = this
        userAgent = Util.getUserAgent(this, USER_AGENT)
        prefs = getSharedPreferences()
        Stetho.initializeWithDefaults(this)
    }

    private fun getSharedPreferences() = applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getDownloadManager(): DownloadManager? {
        if (downloadManager == null) {
            initDownloadManager()
        }
        return downloadManager
    }

    fun getDownloadTracker(): DownloadTracker? {
        if (downloadTracker == null) {
            initDownloadManager()
        }
        return downloadTracker
    }

    @Synchronized
    private fun initDownloadManager() {
        downloadManager = DownloadManager(
                DownloaderConstructorHelper(getDownloadCache(), buildHttpDataSourceFactory(null)),
                MAX_SIMULTANEOUS_DOWNLOADS,
                DownloadManager.DEFAULT_MIN_RETRY_COUNT,
                File(getDownloadDirectory(), DOWNLOAD_ACTION_FILE),
                DashDownloadAction.DESERIALIZER)

        downloadTracker = DownloadTracker(
                /* context= */ this,
                buildDataSourceFactory(/* listener= */null),
                File(getDownloadDirectory(), DOWNLOAD_TRACKER_ACTION_FILE),
                DashDownloadAction.DESERIALIZER)


        downloadManager?.addListener(downloadTracker)
    }

    /** Returns a [DataSource.Factory].  */
    fun buildDataSourceFactory(listener: TransferListener<in DataSource>?): DataSource.Factory {
        val upstreamFactory = DefaultDataSourceFactory(this, listener, buildHttpDataSourceFactory(listener))
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache())
    }

    private fun buildReadOnlyCacheDataSource(
            upstreamFactory: DefaultDataSourceFactory, cache: Cache): CacheDataSourceFactory {
        return CacheDataSourceFactory(
                cache,
                upstreamFactory,
                FileDataSourceFactory(),
                /* eventListener= */ null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null)/* cacheWriteDataSinkFactory= */
    }

    /** Returns a [HttpDataSource.Factory].  */
    fun buildHttpDataSourceFactory(
            listener: TransferListener<in DataSource>?): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(userAgent, listener)
    }

    @Synchronized
    private fun getDownloadCache() = downloadCache
            ?: SimpleCache(File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY), NoOpCacheEvictor()).apply { downloadCache = this }

    private fun getDownloadDirectory() = downloadDirectory ?: run {
        getExternalFilesDir(null) ?: filesDir
    }.apply { downloadDirectory = this }

}