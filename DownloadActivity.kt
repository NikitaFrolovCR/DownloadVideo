package com.nikita_frolov_cr.downloadvideo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.OfflineLicenseHelper
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.dash.DashUtil
import io.reactivex.Flowable
import kotlinx.android.synthetic.main.activity_download.*

class DownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        bStop.setOnClickListener {
            startService(Intent(this, DemoDownloadService::class.java)
                    .setAction("com.google.android.exoplayer.downloadService.action.STOP_DOWNLOADS")
                    .putExtra(DownloadService.KEY_FOREGROUND, false))
        }

        bStart.setOnClickListener {
            startService(Intent(this, DemoDownloadService::class.java)
                    .setAction("com.google.android.exoplayer.downloadService.action.START_DOWNLOADS")
                    .putExtra(DownloadService.KEY_FOREGROUND, false))
        }

        downloadLicense(MainActivity.URI, MainActivity.LICENSE_URL, MainActivity.REALISE_PID)

    }

    fun downloadLicense(uri: Uri, licenseUrl: String, releasePid: String) {
        val licenseDataSourceFactory = (application as DemoApplication).buildHttpDataSourceFactory(null)

        val callback = MPXHttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory, releasePid)

        val mediaDrm = FrameworkMediaDrm.newInstance(MainActivity.UUID)

        val offlineLicenseHelper = OfflineLicenseHelper(MainActivity.UUID, mediaDrm, callback, null)

        val dataSource = licenseDataSourceFactory.createDataSource()

        Flowable.fromCallable { uri }
                .map { DashUtil.loadManifest(dataSource, it).getPeriod(0) }
                .map { DashUtil.loadDrmInitData(dataSource, it) }
                .map { offlineLicenseHelper.downloadLicense(it) }
                .compose { RxUtils.ioToMain(it) }
                .subscribe {
                    DemoApplication.offlineLicenseKeySetId = it
                    (application as DemoApplication).getDownloadTracker()
                            ?.toggleDownload(this, "Download", uri, null)
                }
    }

}
