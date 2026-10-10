package com.icr.atlas.rp1.utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import org.json.JSONObject;


public class RP1RelayClient {

    private static final RP1RelayClient INSTANCE = new RP1RelayClient();
    private String relayHost ="cc";
    private int relayPort = 8800;
    private String relayKey ="";
    private int connectTimeout =10000;
    private int readTimeout = 60000;
    private int maxResponseBytes =5 * 1024 * 1024;

    private RP1RelayClient() {
        readProperties();
    }

    public static RP1RelayClient getInstance() {
        return INSTANCE;
    }

    private void readProperties() {
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            /*
             * You can either use a separate
             * properties file or move these
             * properties into your existing
             * IGMHIST.PRO.
             */
            fis =new FileInputStream("RP1RELAY.PRO");
            props.load(fis);
            relayHost =props.getProperty("relayHost",relayHost);
            relayPort =Integer.parseInt(props.getProperty("relayPort",String.valueOf(relayPort)));
            relayKey =props.getProperty("relayKey",relayKey);
            connectTimeout = Integer.parseInt(props.getProperty("clientConnectTimeout",String.valueOf(connectTimeout)));
            readTimeout = Integer.parseInt(props.getProperty("clientReadTimeout",String.valueOf(readTimeout)));
            maxResponseBytes =Integer.parseInt(props.getProperty("maxResponseBytes",String.valueOf(maxResponseBytes)));
        } catch (Exception e) {
            System.out.println("Unable to read RP1RELAY.PRO: "+ e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                	//ignore
                }
            }
        }
    }

    public void setRelayHost(String relayHost) {
        this.relayHost = relayHost;
    }

    public void setRelayPort(int relayPort) {
        this.relayPort = relayPort;
    }

    public void setRelayKey(String relayKey) {
        this.relayKey =relayKey == null ? "" : relayKey;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    /*
     * This deliberately resembles:
     *
     * IGMApiClient.postJson()
     *
     * except that only the API path is sent.
     *
     * Example:
     *
     * /api/transaction_update.php
     */
    public ApiResponse postJson(String path,String jsonPayload)
    		throws IOException {
    	return postJson(path,jsonPayload,false);
    }
    
    public ApiResponse postJson(String path,String jsonPayload,boolean skipQueue)
    		throws IOException {

    	if (path == null || path.length() == 0) {
            throw new IOException("Relay API path is empty");
        }

        if (jsonPayload == null) {
            jsonPayload = "";
        }

        Socket socket = new Socket();
        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            socket.connect(new InetSocketAddress(relayHost,relayPort), connectTimeout);
            socket.setSoTimeout(readTimeout);
            out =new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            /*
             * Base64 encode the actual HTTP
             * request body.
             */
            String bodyBase64 =Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
            JSONObject request = new JSONObject();
            request.put("version",1);
            request.put("key",relayKey);
            request.put("path",path);
            request.put("bodyBase64",bodyBase64);
            request.put("skipQueue",skipQueue);
            byte[] requestBytes =request.toString().getBytes(StandardCharsets.UTF_8);

            /*
             * Send:
             *
             * [4 byte length]
             * [JSON]
             */
            out.writeInt(requestBytes.length);
            out.write(requestBytes);
            out.flush();
            /*
             * Read response length.
             */
            int responseLength =in.readInt();
            if (responseLength <= 0 || responseLength >maxResponseBytes) {
                throw new IOException("Invalid relay response length: "+ responseLength);
            }

            byte[] responseBytes =new byte[responseLength];
            in.readFully(responseBytes);
            String responseText =new String(responseBytes,StandardCharsets.UTF_8);
            JSONObject response =new JSONObject(responseText);
            /*
             * relayOk means the relay itself
             * successfully processed the request.
             *
             * It does NOT mean the external
             * igmhistory request returned 200.
             */
            boolean relayOk =response.optBoolean("relayOk",false);
            if (!relayOk) {
                String error =response.optString("error","Unknown relay error");
                throw new IOException("Relay error: "+ error);
            }

            int status =response.optInt("status",0);
            String responseBodyBase64 = response.optString("bodyBase64","");
            byte[] decodedResponse;
            try {
                decodedResponse =Base64.getDecoder().decode(responseBodyBase64);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid Base64 response from relay", e);
            }
            String responseBody =new String(decodedResponse,StandardCharsets.UTF_8);
            return new ApiResponse(status,responseBody);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                	//nothing
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                	//nothing
                }
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            	//nothing
            }
        }
    }
}