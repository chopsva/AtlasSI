package com.icr.atlas.AtlasSecurity.Base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.icr.atlas.AtlasSecurity.AtlasSecurityLogger;

/**
 *
 * Protocol assumption (default framing):
 *  - send JSON + '\n'
 *  - read single line response
 *
 * New in this version:
 *  - Optional persistent connection (keep socket open between requests)
 *  - Automatic re-connect when socket is missing/broken
 *  - Optional one-time retry on failure when persistence is enabled
 */
public class AtlasSecuritySocketClient extends AtlasSecurityClientBase {

    private String host = "";
    private int port = -1;
    private boolean useTls = false;
    //private boolean ignoreAck= true; //socket should default to TRUE (stream data to only)
    //moved to BASE

    /** When true, reuse a single socket across calls (until it breaks or you close it). */
    //private boolean keepSocketOpen = false;
    //moved to BASE

    /** Guard for socket lifecycle. */
    private final Object socketLock = new Object();

    /** Current persistent socket (only used when keepSocketOpen=true). */
    private Socket persistentSocket = null;

    /** Endpoint used to create the persistent socket (so we can detect changes). */
    private String persistentEndpoint = "";

    public AtlasSecuritySocketClient(boolean allowInsecureSSL) {
        super(allowInsecureSSL);
        AtlasSecurityLogger.getInstance().info("AtlasSecuritySocketClient started");
    }

    public void setURL(String aURL) {
        super.setURL(aURL);
        parseEndpoint(this.endpoint);
        // If endpoint changed while holding a persistent socket, drop it so we reconnect next call.
        synchronized (socketLock) {
            if (keepSocketOpen && persistentSocket != null && !safeEquals(persistentEndpoint, this.endpoint)) {
                closePersistentSocketNoThrow();
            }
        }
    }

    /**
     * Enable/disable persistent socket behavior.
     * - If disabled, the client will open/close a socket per request (original behavior).
     * - If enabled, the client will keep the socket open and reuse it; it will reconnect if needed.
     */
//    public void setKeepSocketOpen(boolean keepOpen) {
//        synchronized (socketLock) {
//            this.keepSocketOpen = keepOpen;
//            if (!keepOpen) {
//                closePersistentSocketNoThrow();
//            }
//        }
//    }
//
//    /** Returns current persistent mode setting. */
//    public boolean isKeepSocketOpen() {
//        synchronized (socketLock) {
//            return keepSocketOpen;
//        }
//    }
    
    public void setIgnoreAck(boolean ignoreAck) {
        synchronized (socketLock) {
            this.ignoreAck = ignoreAck;
        }
    	
    }

    /**
     * Closes any persistent socket immediately.
     * Safe to call anytime.
     */
    public void close() {
        synchronized (socketLock) {
            closePersistentSocketNoThrow();
        }
    }

    /**
     * Force reconnection on the next request (persistent mode only).
     */
    public void reconnect() {
        synchronized (socketLock) {
            closePersistentSocketNoThrow();
        }
    }

    protected void doSendData(String jsonBody, String endpoint) throws IOException {
        parseEndpoint(endpoint);

        if (host == null || host.length() == 0 || port <= 0) {
            setCommError("Invalid socket endpoint: " + endpoint, null);
            throw new IOException("Invalid socket endpoint: " + endpoint);
        }

        AtlasSecurityLogger.getInstance().debug("Sending:" + jsonBody);

        if (!isKeepSocketOpen()) {
            // Per-request socket lifecycle (original behavior)
            doPostOnceWithFreshSocket(jsonBody);
            return;
        }

        // Persistent socket lifecycle with 1 retry after reconnect
        IOException lastIo = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            Socket s = null;
            try {
                s = ensureConnected(endpoint);
                writeMessage(s, jsonBody);
                String response=null;
                if (!this.ignoreAck) {
                	response = readLine(s.getInputStream());
                }

                if (response != null && response.length() > 0) {
                    setCommGood("response:" + response);
                } else {
                    // Treat null/empty as error; likely server closed or protocol mismatch
                	if (!this.ignoreAck) {
                		setCommError("Empty response from " + host + ":" + port, null);
                	}
                }
                return; // success path
            } catch (IOException ioe) {
                lastIo = ioe;
                setCommError("Socket I/O error (attempt " + (attempt + 1) + ")", ioe);

                // If the socket broke, drop it and retry once.
                synchronized (socketLock) {
                	AtlasSecurityLogger.getInstance().debug("closing socket:"+ioe);
                    closePersistentSocketNoThrow();
                }

                if (attempt == 0) {
                    // retry once
                    continue;
                }
                throw lastIo;
            } catch (Exception e) {
                setCommError("Socket error", e);
                synchronized (socketLock) {
                	AtlasSecurityLogger.getInstance().debug("closing socket:"+e);
                    closePersistentSocketNoThrow();
                }
                if (e instanceof IOException) throw (IOException) e;
                throw new IOException(e);
            }
        }

        // Defensive; should never reach.
        if (lastIo != null) throw lastIo;
    }

    private void doPostOnceWithFreshSocket(String jsonBody) throws IOException {
        SocketFactory factory = createSocketFactory(useTls);
        Socket socket = factory.createSocket();
        try {
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            socket.setSoTimeout(readTimeout);

            writeMessage(socket, jsonBody);

            String response = readLine(socket.getInputStream());

            if (response != null && response.length() > 0) {
                setCommGood("response:" + response);
            } else {
                setCommError("Empty response from " + host + ":" + port, null);
            }
        } catch (Exception e) {
            setCommError("Socket error", e);
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(e);
        } finally {
            try { socket.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * Ensures a persistent socket is open and connected, and matches the endpoint.
     * If socket is missing/closed/broken, it will create a new one.
     */
    private Socket ensureConnected(String endpoint) throws IOException {
        synchronized (socketLock) {
            // If endpoint changed since last connect, drop and recreate.
            if (persistentSocket != null && !safeEquals(persistentEndpoint, endpoint)) {
                closePersistentSocketNoThrow();
            }

            if (persistentSocket != null && isSocketUsable(persistentSocket)) {
                return persistentSocket;
            }

            closePersistentSocketNoThrow();

            SocketFactory factory = createSocketFactory(useTls);
            Socket s = factory.createSocket();
            try {
                s.connect(new InetSocketAddress(host, port), connectTimeout);
                s.setSoTimeout(readTimeout);

                persistentSocket = s;
                persistentEndpoint = endpoint;
                return persistentSocket;
            } catch (IOException ioe) {
                try { s.close(); } catch (Exception ignore) {}
                persistentSocket = null;
                persistentEndpoint = "";
                throw ioe;
            }
        }
    }

    /**
     * Conservative check for socket usability.
     * Note: Socket#isConnected() remains true after close; so we check multiple flags.
     */
    private boolean isSocketUsable(Socket s) {
        if (s == null) return false;
        if (s.isClosed()) return false;
        if (!s.isConnected()) return false;
        if (s.isInputShutdown()) return false;
        if (s.isOutputShutdown()) return false;
        return true;
    }

    private void closePersistentSocketNoThrow() {
        if (persistentSocket != null) {
            try { persistentSocket.close(); } catch (Exception ignore) {}
        }
        persistentSocket = null;
        persistentEndpoint = "";
    }

    private boolean safeEquals(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private void parseEndpoint(String endpoint) {
        host = "";
        port = -1;
        useTls = false;

        if (endpoint == null) return;

        String ep = endpoint.trim();
        if (ep.length() == 0) return;

        if (ep.indexOf("://") < 0) {
            parseHostPort(ep);
            return;
        }

        try {
            URI uri = new URI(ep);
            String scheme = uri.getScheme();
            String h = uri.getHost();
            int p = uri.getPort();

            host = (h == null) ? "" : h;
            port = p;

            if (scheme != null) {
                String s = scheme.toLowerCase();
                useTls = ("ssl".equals(s) || "tls".equals(s) || "https".equals(s));
            }
        } catch (URISyntaxException e) {
            parseHostPort(ep);
        }
    }

    private void parseHostPort(String hostPort) {
        String hp = hostPort.trim();

        if (hp.startsWith("[")) {
            int close = hp.indexOf(']');
            if (close > 0) {
                host = hp.substring(1, close);
                int colon = hp.indexOf(':', close);
                if (colon >= 0 && colon + 1 < hp.length()) {
                    port = safeParseInt(hp.substring(colon + 1));
                }
            }
            return;
        }

        int idx = hp.lastIndexOf(':');
        if (idx > 0 && idx + 1 < hp.length()) {
            host = hp.substring(0, idx);
            port = safeParseInt(hp.substring(idx + 1));
        } else {
            host = hp;
            port = -1;
        }
    }

    private int safeParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private SocketFactory createSocketFactory(boolean tls) throws IOException {
        if (!tls) return SocketFactory.getDefault();

        if (!allowInsecureSSL) {
            return SSLSocketFactory.getDefault();
        }

        return getInsecureSSLSocketFactory();
    }

    private void writeMessage(Socket socket, String payload) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        // Do not close writer; that would close the socket output stream.
        bw.write(payload == null ? "" : payload);
        bw.write("\n");
        bw.flush();
    }

    private String readLine(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        return br.readLine();
    }
}
