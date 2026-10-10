package com.icr.atlas.AtlasSecurity.Base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import com.icr.atlas.AtlasSecurity.AtlasSecurityLogger;

// Based on original implementation, refactored to extend AtlasSecurityClientBase
public class AtlasSecurityHTTPClient extends AtlasSecurityClientBase {

    public AtlasSecurityHTTPClient(boolean allowInsecureSSL) {
        super(allowInsecureSSL);
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "20");
        AtlasSecurityLogger.getInstance().info("AtlasSecurityHTTPClient started");
    }

    protected void doSendData(String jsonBody, String thisUrl) throws IOException {
        URL url = new URL(thisUrl);
        HttpURLConnection conn = openConnection(url);

        AtlasSecurityLogger.getInstance().debug("Sending:" + jsonBody);

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);

        try {
            OutputStream os = conn.getOutputStream();
            try {
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
            } finally {
                try { os.close(); } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            setCommError("Connect error", e);
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        String response = readStream(is);

        if (status == 200) {
            setCommGood("statusCode" + status + " response:" + response);
        } else {
            setCommError("statusCode" + status + " response:" + response, null);
        }
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (conn instanceof HttpsURLConnection) {
            applyInsecureTls((HttpsURLConnection) conn);
        }
        return conn;
    }
}
