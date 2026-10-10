package com.icr.atlas.RYOL;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

import com.icr.atlas.ATLASGuiExtend;

public class RYOLTransactionSender {
	private String TARGET_URL="http://ryol:8888/api/posTrans";
	private int CONNECT_TIMEOUT=5000;
	private int READ_TIMEOUT=10000;
	private int LOOP_TIMER=30000;
	
	private final Queue<String> retryQueue=new LinkedList<>();
	

	public RYOLTransactionSender() {
		// TODO Auto-generated constructor stub
		System.out.println("RYOLTransactionSender init");
		ATLASGuiExtend.getInstance().updateUserData("RYOLTransactionSendStatus","INIT" );
		new Thread(this::retryLoop).start();
	}
	
	public void sendTransaction(String theTrans) {
		String theBody="{\"transactions\":["+theTrans+"]}";
		int rc=sendHTTPTest(theBody);
		if (rc<0) {
			System.err.println("problem connecting");
			synchronized (retryQueue) {
				retryQueue.add(theBody);
			}
		}
	}

	private int sendHTTPTest(String theBody) {
		int responseCode=-1;
		HttpURLConnection conn=null;
        try {
            URL url = new URL(TARGET_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] toSend = theBody.getBytes("UTF-8");
                os.write(toSend, 0, toSend.length);
            }

            responseCode = conn.getResponseCode();
        } catch (Exception e) {
            System.err.println("Send error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }

        System.out.println("ResponseCode: " + responseCode);
        return responseCode;	
		
	}
	
	private void retryLoop() {
		while (true) {
			String msg=null;
			System.out.println("  cycle queueLen:"+retryQueue.size());
			if (retryQueue.size()!=0) {
				//ATLASGuiExtend.getInstance().updateUserData("RYOLTransactionSendStatus","Q:"+retryQueue.size() );
				ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.WARN,"RYOL TransUpdate");
			} else {
				//ATLASGuiExtend.getInstance().updateUserData("RYOLTransactionSendStatus", "OK");
				ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.GOOD,"RYOL TransUpdate");
			}
			synchronized(retryQueue) {
				if (!retryQueue.isEmpty()) {
					msg=retryQueue.peek();
				}
			}
			if (msg!=null) {
				int rc=sendHTTPTest(msg);
				if (rc>0) {
					System.out.println("sent ");
					synchronized(retryQueue) {
						retryQueue.remove();
					}
				} else {
					System.out.println("retry failed");
				}
			}
			
			try{
				Thread.sleep(LOOP_TIMER);
			} catch (InterruptedException ignored) {}
		}
	}
}
