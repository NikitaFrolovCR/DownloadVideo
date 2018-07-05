package com.nikita_frolov_cr.downloadvideo;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Hildegardo Noronha on 07/11/2016.
 */

public class MPXHttpMediaDrmCallback implements MediaDrmCallback
{
    private final HttpDataSource.Factory dataSourceFactory;
    private final String defaultUrl, releasePid;

    /**
     * @param defaultUrl The default license URL.
     * @param dataSourceFactory A factory from which to obtain {@link HttpDataSource} instances.
     */
    public MPXHttpMediaDrmCallback(String defaultUrl, HttpDataSource.Factory dataSourceFactory, String releasePid)
    {
        this.dataSourceFactory = dataSourceFactory;
        this.defaultUrl = defaultUrl;
        this.releasePid = releasePid;
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws IOException
    {
        return null;
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception
    {
        String url = request.getDefaultUrl();
        if (TextUtils.isEmpty(url))
            url = defaultUrl;

        return executePost(url, request.getData());
    }

    private byte[] executePost(String url, byte[] data)
            throws IOException
    {
//        return Base64.decode("CAISewpKCiA2Nzk0OEYyMDY4REI4OTZEMDEwMDAwMDAwMDAwMDAwMBIgNjc5NDhGMjA2OERCODk2RDAxMDAwMDAwMDAwMDAwMDAaACACKAcSDQgBEAEYASgAMIDO2gMaFiADQhIKEGtjdGwAdqcAZ9sjPwAAAAAgrLv32QVQABog35ZVEU7rK0r3YueFzdBRMBLgHjrrJQasFStIoUaNF0w=", Base64.DEFAULT);
        JSONObject jsonRoot;
        try
        {
                JSONObject getWidevineLicense = new JSONObject();

                getWidevineLicense.put("releasePid", releasePid);
                getWidevineLicense.put("widevineChallenge", new String(Base64.encodeToString(data, Base64.DEFAULT)));

            jsonRoot = new JSONObject();
            jsonRoot.put("getWidevineLicense", getWidevineLicense);
        }
        catch(JSONException e)
        {
            return null;
        }

        HttpDataSource dataSource = dataSourceFactory.createDataSource();
        dataSource.setRequestProperty("Content-Type", "application/json");

        DataSpec dataSpec = new DataSpec(Uri.parse(url), jsonRoot.toString().getBytes(), 0, 0, C.LENGTH_UNSET, null, DataSpec.FLAG_ALLOW_GZIP);
        DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);

        try
        {
            byte[] bytes = Util.toByteArray(inputStream);
            String string = new String(bytes);

            JSONObject jsonObject = new JSONObject(string);

            jsonObject = jsonObject.getJSONObject("getWidevineLicenseResponse");

            return Base64.decode(jsonObject.getString("license"), Base64.DEFAULT);

        }
        catch(JSONException e)
        {
            Log.e("", e.toString());
            return null;
        }
        catch(IOException e)
        {
            Log.e("", e.toString());
            return null;
        }
        finally
        {
            inputStream.close();
        }
    }
}

