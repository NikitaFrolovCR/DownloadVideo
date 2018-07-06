package com.nikita_frolov_cr.downloadvideo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {

        val URI_3 = Uri.parse("https://link.theplatform.eu/s/jGxigC/DO_0HEeyJy0D?token=Aypf0xzQ6hiAEwn0ffqK8bDIIED8kGCG")
        val URI_1 = Uri.parse("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd")
        val URI_2 = Uri.parse("https://unified.cdn.bbaws.net/i/410/4/DI_0419_orig.ism/master.m3u8?filter=type!%3D%22audio%22%7C%7Cchannels%3E%3D2")
        val URI = Uri.parse("https://link.theplatform.eu/s/jGxigC/yc5wVCZZ0kv0?token=-0RbPHnQWUQ4GJ3aLeoosaAoAED4oEAQ")

        val UUID = Util.getDrmUuid("widevine")
        val REALISE_PID = "yc5wVCZZ0kv0"

        val LICENSE_URL_3 = "https://widevine.entitlement.theplatform.eu/wv/web/ModularDrm?form=json&schema=1.0&account=http://access.auth.theplatform.com/data/Account/2693468579&token=Aypf0xzQ6hiAEwn0ffqK8bDIIED8kGCG"
        val LICENSE_URL_1 = "https://proxy.uat.widevine.com/proxy?video_id=48fcc369939ac96c&provider=widevine_test"
        val LICENSE_URL_2 = ""
        val LICENSE_URL = "https://widevine.entitlement.theplatform.eu/wv/web/ModularDrm?form=json&schema=1.0&account=http://access.auth.theplatform.com/data/Account/2693468579&token=-0RbPHnQWUQ4GJ3aLeoosaAoAED4oEAQ"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bDownload.setOnClickListener {
            startActivity(Intent(this, DownloadActivity::class.java))
        }

        bPlayback.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
    }

}
