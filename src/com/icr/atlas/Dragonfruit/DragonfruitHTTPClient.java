package com.icr.atlas.Dragonfruit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.icr.atlas.ATLASGuiExtend;


//based on ChatGPT code 
public class DragonfruitHTTPClient {

	private final boolean allowInsecureSSL;
	private String theURL="";
	
	public int connectTimeout=1000;
	public int readTimeout=1000;
	
	private static final ExecutorService HTTP_EXECUTOR= Executors.newCachedThreadPool();
	
	public DragonfruitHTTPClient(boolean allowInsecureSSL) {
		this.allowInsecureSSL=allowInsecureSSL;
		System.setProperty("http.keepAlive", "true");
		System.setProperty("http.maxConnections", "20");
		DragonfruitLogger.getInstance().info("DragonfruitHTTPClient started");
	}
	
	public void setURL(String aURL) {
		this.theURL=aURL;
	}
	

	
//	public String postJSON(String jsonBody) throws IOException {
//		return postJSON(jsonBody,this.theURL);
//	}
	public void postJSON(String jsonBody) throws IOException {
		postJSON(jsonBody,this.theURL);
	}
	public void postJSON(String jsonBody, String thisUrl) throws IOException {
		//postJSONAsync(jsonBody,thisUrl);
		doPostJSON(jsonBody,thisUrl);
	}
	
	//TODO- move the send to a thread to prevent pos delays.
	//moving to a thread resulted in out of sequence messages 
	//public String postJSON(String jsonBody,String thisUrl) throws IOException {
	public void doPostJSON(String jsonBody,String thisUrl) throws IOException {
		URL url=new URL(thisUrl);
		HttpURLConnection conn=openConnection(url);
		
		DragonfruitLogger.getInstance().debug("Sending:"+jsonBody);
		
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setConnectTimeout(1000);
		conn.setReadTimeout(1000);
		
		try( OutputStream os=conn.getOutputStream()) {
			os.write(jsonBody.getBytes("UTF-8"));
			os.flush();
		} catch (Exception e) {
			ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.ERROR);
			DragonfruitLogger.getInstance().error("Connect error"+e.getMessage());
		}
		
		int status=conn.getResponseCode();
		InputStream is=(status>=200 && status <300) ? conn.getInputStream() : conn.getErrorStream();
		String response=readStream(is);

		if (status==200) {
			ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.GOOD);
			DragonfruitLogger.getInstance().debug("statusCode"+status);
			DragonfruitLogger.getInstance().debug("response:"+response);
		} else {
			ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.ERROR);
			DragonfruitLogger.getInstance().error("statusCode"+status);
			DragonfruitLogger.getInstance().error("response:"+response);
		}
	}
	
	private HttpURLConnection openConnection(URL url) throws IOException {
		HttpURLConnection conn=(HttpURLConnection)url.openConnection();
		if (conn instanceof HttpsURLConnection && allowInsecureSSL) {
			trustAll((HttpsURLConnection)conn);
		}
		return conn;
	}
	
	private void trustAll(HttpsURLConnection https) {
		try {
			TrustManager[] trustAllCerts=new TrustManager[]{
					new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
						public void checkClientTrusted(X509Certificate[] certs, String authType) {}
						public void checkServerTrusted(X509Certificate[] certs, String authType) {}
						
					}
			};
			
			SSLContext sc=SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			https.setSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception ignore) {
			
		}
	}
	
	private String readStream(InputStream is) throws IOException {
		if (is==null) return "";
		
		StringBuilder sb=new StringBuilder();
		try(BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"))) {
			String line;
			while((line = br.readLine()) != null) {
				sb.append(line);
			}
		}
		return sb.toString();
	}

}
