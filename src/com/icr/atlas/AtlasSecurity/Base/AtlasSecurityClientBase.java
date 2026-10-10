package com.icr.atlas.AtlasSecurity.Base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.AtlasSecurity.AtlasSecurityLogger;

/**
 * Common base for AtlasSecurity clients (HTTP, raw sockets, etc.).
 *
 * Provides:
 *  - allowInsecureSSL handling (trust-all TLS)
 *  - endpoint storage (keeps legacy setURL name)
 *  - common postJSON(...) overloads
 *  - readStream(...) helper
 *  - common CommState + logging helpers
 */
public abstract class AtlasSecurityClientBase {

    protected final boolean allowInsecureSSL;

    /** Kept for backward compatibility with existing callers. */
    protected String endpoint = "";

    public int connectTimeout = 1000;
    public int readTimeout = 1000;
    protected boolean keepSocketOpen=false;
    protected boolean ignoreAck=true; //default for "stream" socket
    

    private volatile SSLSocketFactory insecureSSLSocketFactory;

    protected AtlasSecurityClientBase(boolean allowInsecureSSL) {
        this.allowInsecureSSL = allowInsecureSSL;
    }

    /** Backward-compatible config entry point; subclasses interpret as needed. */
    public void setURL(String aURL) {
        this.endpoint = (aURL == null) ? "" : aURL.trim();
    }
    
    public void setKeepSocketOpen(boolean keepOpen) {
            this.keepSocketOpen = keepOpen;
    }

    public boolean isKeepSocketOpen() {
            return this.keepSocketOpen;
    }    
    
    public void setIgnoreAck(boolean ignoreAck) {
    	this.ignoreAck=ignoreAck;
    }
    
    public boolean isIgnoreAck() {
    	return this.ignoreAck;
    }

    public final void sendData(String jsonBody) throws IOException {
        sendData(jsonBody, this.endpoint);
    }

    public final void sendData(String jsonBody, String endpoint) throws IOException {
        doSendData(jsonBody, endpoint);
    }

    /** Subclasses implement transport specifics here. */
    protected abstract void doSendData(String jsonBody, String endpoint) throws IOException;

    protected final void setCommGood(String detail) {
        ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.GOOD);
        if (detail != null && detail.length() > 0) {
            AtlasSecurityLogger.getInstance().debug(detail);
        }
    }

    protected final void setCommError(String detail, Exception e) {
        ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.ERROR);
        if (detail != null && detail.length() > 0) {
            AtlasSecurityLogger.getInstance().error(detail);
        }
        if (e != null) {
            AtlasSecurityLogger.getInstance().error(String.valueOf(e.getMessage()));
        }
    }

    /** Reusable response reader used by the HTTP client; safe for general use too. */
    protected final String readStream(InputStream is) throws IOException {
        if (is == null) return "";

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            try { br.close(); } catch (Exception ignore) {}
        }
        return sb.toString();
    }

    /**
     * Returns a trust-all SSLSocketFactory when allowInsecureSSL is true.
     * If allowInsecureSSL is false, this returns null.
     */
    protected final SSLSocketFactory getInsecureSSLSocketFactory() throws IOException {
        if (!allowInsecureSSL) return null;

        SSLSocketFactory local = insecureSSLSocketFactory;
        if (local != null) return local;

        synchronized (this) {
            local = insecureSSLSocketFactory;
            if (local != null) return local;

            try {
                TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
                };

                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());
                insecureSSLSocketFactory = sc.getSocketFactory();
                return insecureSSLSocketFactory;
            } catch (Exception e) {
                throw new IOException("Unable to init insecure SSL context: " + e.getMessage(), e);
            }
        }
    }

    /** Applies trust-all SSL to an HTTPS connection if allowInsecureSSL is enabled. */
    protected final void applyInsecureTls(HttpsURLConnection https) throws IOException {
        if (https == null) return;
        if (!allowInsecureSSL) return;
        SSLSocketFactory sf = getInsecureSSLSocketFactory();
        if (sf != null) {
            https.setSSLSocketFactory(sf);
        }
    }
}
