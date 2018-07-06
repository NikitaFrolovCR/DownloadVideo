package com.nikita_frolov_cr.downloadvideo

import android.util.Base64
import com.google.android.exoplayer2.drm.ExoMediaDrm
import com.google.android.exoplayer2.drm.MediaDrmCallback
import java.util.*

class OfflineMediaDrmCallback(private val license: String) : MediaDrmCallback {

    override fun executeProvisionRequest(uuid: UUID?, request: ExoMediaDrm.ProvisionRequest?) = null

    override fun executeKeyRequest(uuid: UUID?, request: ExoMediaDrm.KeyRequest?) = Base64.decode(license, Base64.DEFAULT)

}

//"CAISewpKCiA2Nzk0OEYyMDY4REI4OTZEMDEwMDAwMDAwMDAwMDAwMBIgNjc5NDhGMjA2OERCODk2RDAxMDAwMDAwMDAwMDAwMDAaACACKAcSDQgBEAEYASgAMIDO2gMaFiADQhIKEGtjdGwAdqcAZ9sjPwAAAAAgrLv32QVQABog35ZVEU7rK0r3YueFzdBRMBLgHjrrJQasFStIoUaNF0w="