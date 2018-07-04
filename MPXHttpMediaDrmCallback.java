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


//{"getWidevineLicenseResponse":{"license":"CAIS6wQKSgogQUE4MjdDRTUxOTJBRUUxNzAyMDAwMDAwMDAwMDAwMDASIEFBODI3Q0U1MTkyQUVFMTcwMjAwMDAwMDAwMDAwMDAwGgAgASgAEg0IARABGAEoADCAztoDGmYSEFyalnfZQv28s7q1HRZi2ukaUM+FWXqEuQMcL72odz6SEkNXhzxjFwZlOtb1UhIhM85M4r7TlYfBzGfTxAaJjTw2t0jmgebhy3PurqkGFjzgTSI755mOlf1Dgoo8qP070YUwIAEahAEKEIUMLN9PkFo6ttilP98TsJkSEBhCVGm+60Wurq15hTaMfjAaIFulSGUSxjggu5+0bXzk8xwT781Ja76VunoXttoVcGIKIAIoAUI0CiBZZm2N6/XxtrMoNjHw5BRCqWC6jDwFt7PA+tp3YD8QCxIQZhuWMHNoLTPafTRIyC2gr2ICU0QahwEKELX6LDJupF2VpxQb51YpbJsSENwTRppMXVivGiEIvchB1tEaIGvzws/ETqDWpWvScaLauDzO+zswARyklk1p00r8q+LEIAIoAUI0CiBkVVpi7GJ+oEsARx/uJ1oklir5JJJNmQxZo6r/MnUmvhIQAg8fJO1MBQdxG9VCyFfAPmIFQVVESU8ahAEKEF6U9AgAMFU/kmGGUqapYvkSEJD2wJ4/HDU4io6qu4cwH30aIMG18F3wn3nOgxJt8ITSPP1y/GHf3qN8PZvjof90CP52IAIoAUI0CiDIU07nuWrmv57Mn3JWHaq14tp2lBJdhB6vqaqEMpczdxIQC+YVEQHc3pot/Jm5yXN+YWICSEQgl/Hx2QUoATjj3JWbBlADGiCeO21bHK/+P7IZhw+GuzhD4VY+Bbbmfq63nf2O83TGxiKAAn1ILcDVRDjxzNcIRCvIOX9U89bWeejEkaR8vfY53s5VjFF6Vo3LdI50BgFuN+B29Tl8umcjF3mt5Un2+HBHZGYgpPOBmPkRQjdROcCqyNu+bZqs5VY1vzGlgqEMcK6s5PSYMBIfTEW9RTm3rlG1oJX+OH9MaznwxjeJM7hQK8rLB5BHMK/ThGNxbIlqaItVQOKJ02UTlLT/vlN3wRnoZNcT6b0nU8TXCjGe4TDmSiQlpSD2B2EVVDGEGc/6up+o+tNUU112gAAYeDadf47fsWVeFVIuCy2VZFzGKtFX9aNXXpb+DyxZf123i56Wc+go8Ql2Di1XsozLid6qT4pmJsE="}}
// {"getWidevineLicenseResponse":{"license":"CAIS6wQKSgogQUE4MjdDRTUxOTJBRUUxNzAyMDAwMDAwMDAwMDAwMDASIEFBODI3Q0U1MTkyQUVFMTcwMjAwMDAwMDAwMDAwMDAwGgAgASgAEg0IARABGAEoADCAztoDGmYSEFyalnfZQv28s7q1HRZi2ukaUM+FWXqEuQMcL72odz6SEkNXhzxjFwZlOtb1UhIhM85M4r7TlYfBzGfTxAaJjTw2t0jmgebhy3PurqkGFjzgTSI755mOlf1Dgoo8qP070YUwIAEahAEKEIUMLN9PkFo6ttilP98TsJkSEBhCVGm+60Wurq15hTaMfjAaIFulSGUSxjggu5+0bXzk8xwT781Ja76VunoXttoVcGIKIAIoAUI0CiBZZm2N6/XxtrMoNjHw5BRCqWC6jDwFt7PA+tp3YD8QCxIQZhuWMHNoLTPafTRIyC2gr2ICU0QahwEKELX6LDJupF2VpxQb51YpbJsSENwTRppMXVivGiEIvchB1tEaIGvzws/ETqDWpWvScaLauDzO+zswARyklk1p00r8q+LEIAIoAUI0CiBkVVpi7GJ+oEsARx/uJ1oklir5JJJNmQxZo6r/MnUmvhIQAg8fJO1MBQdxG9VCyFfAPmIFQVVESU8ahAEKEF6U9AgAMFU/kmGGUqapYvkSEJD2wJ4/HDU4io6qu4cwH30aIMG18F3wn3nOgxJt8ITSPP1y/GHf3qN8PZvjof90CP52IAIoAUI0CiDIU07nuWrmv57Mn3JWHaq14tp2lBJdhB6vqaqEMpczdxIQC+YVEQHc3pot/Jm5yXN+YWICSEQgl/Hx2QUoATjj3JWbBlADGiCeO21bHK/+P7IZhw+GuzhD4VY+Bbbmfq63nf2O83TGxiKAAn1ILcDVRDjxzNcIRCvIOX9U89bWeejEkaR8vfY53s5VjFF6Vo3LdI50BgFuN+B29Tl8umcjF3mt5Un2+HBHZGYgpPOBmPkRQjdROcCqyNu+bZqs5VY1vzGlgqEMcK6s5PSYMBIfTEW9RTm3rlG1oJX+OH9MaznwxjeJM7hQK8rLB5BHMK/ThGNxbIlqaItVQOKJ02UTlLT/vlN3wRnoZNcT6b0nU8TXCjGe4TDmSiQlpSD2B2EVVDGEGc/6up+o+tNUU112gAAYeDadf47fsWVeFVIuCy2VZFzGKtFX9aNXXpb+DyxZf123i56Wc+go8Ql2Di1XsozLid6qT4pmJsE="}}
// CAIS6wQKSgogRTAzMjE4ODE0Q0Q5RTY5MzAxMDAwMDAwMDAwMDAwMDASIEUwMzIxODgxNENEOUU2OTMwMTAwMDAwMDAwMDAwMDAwGgAgAigAEg0IARABGAEoADCAztoDGmYSEIXHk2W15lGgRxBa2WTvFmIaUAm0fh06PLrKewqVp5hkZerGB93PqmW16prKByCf7qgv/APhIZlvAH1AW3Z8yTSZ0Gg3oGMTZBr7dv/iXyhLNv+GmG1HqhGuO55SG8iNqWZcIAEahAEKEIUMLN9PkFo6ttilP98TsJkSEFQvTb/5EgL11IG3SIsGVv4aIHy6URhDfnFuiyJoLG09iFGTvB18bAvbuwiJx+ilonLtIAIoAUI0CiD/9Hu0YR0/QVg8NRWIMSUE8BtMZ+eGAc8N/R9sahtqSxIQOLJCWs0XCzJoWoZZP/cQDmICU0QahwEKELX6LDJupF2VpxQb51YpbJsSEHLA/evhc0mMs9dKgmNWP5MaIMZ/IlAsToma+Zv0wyRx9bGwNwy52fMOFKV0m+R3wxHDIAIoAUI0CiAztr0mrN1mxUd80Ct9pcrAEUobjJ6GOt5HcTm0Cm9c6hIQRDa9OPmnCG4OA0uFlA4a9GIFQVVESU8ahAEKEF6U9AgAMFU/kmGGUqapYvkSEJZzqS6JTBwgSG7Bbq4lXsQaIFMIQZIRta4TiMaGFkrDr0sQcpoiMhsIrF/HAZvgCTG0IAIoAUI0CiD1IEI22cZ3yaYFzyIYSr1msjfFi42W8rb6CT3xuRwDNhIQMvGG8W3TlBYNl5J1iUPFbGICSEQgzpry2QUoATjj3JWbBlADGiDdNDw/JZ+XP7Or7nO08t9ARrc+Q8hLpa3MIWxlnPPlxiKAAoon1SK3KsWSuc9lMFkN5hlPck84iOl2YRABKbkB4ZbsNQTDfETEZzqTkPdhSm9f4ijYXXdGKcKYh/8uoqTtnKCCOJ2oAL8jRUC6ARPt9I6/0RFqlpStE1kMqKi5gG2MlO9EHM9DkT0cz9Qj59RM9HP8VfTgYaLo8JICDlsEPRZzuFeJSYRzcslhO3G8XcgSqaVJcPOWK1YMeHkV92sTfs2lrk5tIkf5rdfVaz0Rm5H8LPSCPJmrXH7swkjSiNgd+POlj7xDaHC7pkO8ir0b2GP+AdizlAZleQxNb6gJL7MPaEfFgvTXWuI7djqC/df1ou9HTsIxN//W5Ko4QSXNNZ0=
// CAIS6wQKSgogRTAzMjE4ODE0Q0Q5RTY5MzAxMDAwMDAwMDAwMDAwMDASIEUwMzIxODgxNENEOUU2OTMwMTAwMDAwMDAwMDAwMDAwGgAgAigAEg0IARABGAEoADCAztoDGmYSEIXHk2W15lGgRxBa2WTvFmIaUAm0fh06PLrKewqVp5hkZerGB93PqmW16prKByCf7qgv/APhIZlvAH1AW3Z8yTSZ0Gg3oGMTZBr7dv/iXyhLNv+GmG1HqhGuO55SG8iNqWZcIAEahAEKEIUMLN9PkFo6ttilP98TsJkSEFQvTb/5EgL11IG3SIsGVv4aIHy6URhDfnFuiyJoLG09iFGTvB18bAvbuwiJx+ilonLtIAIoAUI0CiD/9Hu0YR0/QVg8NRWIMSUE8BtMZ+eGAc8N/R9sahtqSxIQOLJCWs0XCzJoWoZZP/cQDmICU0QahwEKELX6LDJupF2VpxQb51YpbJsSEHLA/evhc0mMs9dKgmNWP5MaIMZ/IlAsToma+Zv0wyRx9bGwNwy52fMOFKV0m+R3wxHDIAIoAUI0CiAztr0mrN1mxUd80Ct9pcrAEUobjJ6GOt5HcTm0Cm9c6hIQRDa9OPmnCG4OA0uFlA4a9GIFQVVESU8ahAEKEF6U9AgAMFU/kmGGUqapYvkSEJZzqS6JTBwgSG7Bbq4lXsQaIFMIQZIRta4TiMaGFkrDr0sQcpoiMhsIrF/HAZvgCTG0IAIoAUI0CiD1IEI22cZ3yaYFzyIYSr1msjfFi42W8rb6CT3xuRwDNhIQMvGG8W3TlBYNl5J1iUPFbGICSEQgzpry2QUoATjj3JWbBlADGiDdNDw/JZ+XP7Or7nO08t9ARrc+Q8hLpa3MIWxlnPPlxiKAAoon1SK3KsWSuc9lMFkN5hlPck84iOl2YRABKbkB4ZbsNQTDfETEZzqTkPdhSm9f4ijYXXdGKcKYh/8uoqTtnKCCOJ2oAL8jRUC6ARPt9I6/0RFqlpStE1kMqKi5gG2MlO9EHM9DkT0cz9Qj59RM9HP8VfTgYaLo8JICDlsEPRZzuFeJSYRzcslhO3G8XcgSqaVJcPOWK1YMeHkV92sTfs2lrk5tIkf5rdfVaz0Rm5H8LPSCPJmrXH7swkjSiNgd+POlj7xDaHC7pkO8ir0b2GP+AdizlAZleQxNb6gJL7MPaEfFgvTXWuI7djqC/df1ou9HTsIxN//W5Ko4QSXNNZ0=