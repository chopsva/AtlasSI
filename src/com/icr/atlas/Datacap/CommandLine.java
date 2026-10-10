package com.icr.atlas.Datacap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.icr.atlas.Datacap.DatacapController.DatacapMessage;
import com.icr.atlas.Utils.INIFile;

//it would be nice to use the DatacapController but it attaches to Atlas for the terminal
//which we won't be able to do on the CC

public class CommandLine {
	
	String termIP="127.0.0.1";
	String merchID="NOMERCHID";
	int seq=1;
	
	public CommandLine() {
		// TODO Auto-generated constructor stub
	}
	
	public void processCommand(String cmd) {
		System.out.println("processCommand:"+cmd);
		System.out.println("pinpad ip:"+termIP);
		System.out.println("merchID:"+merchID);
		switch (cmd.toUpperCase()) {
		case "VER":
			processDeviceVersionCmd();
			break;
		case "BATCHSUMM":
			processBatchSummaryCmd();
			break;
		case "SAFSTAT":
			processSAFStatCmd();
			break;
		default:
			System.out.println("UNKNOWN CMD:"+cmd);
			break;
		}
		
	}

	private String startTransactionXML() {
		seq += 1;
		String xml = "<?xml version=\"1.0\"?>";
		xml += "<TStream><Transaction><MerchantID>" + merchID + "</MerchantID>";
		xml += "<POSPackageID>EMVUSCLIENT:1.26</POSPackageID>";
		xml += "<SecureDevice>CloudEMV2</SecureDevice>";
		xml += "<SequenceNo>" + seq + "</SequenceNo>";
		return xml;
	}
	
	private String completeTransactionXML(String xml) {
		xml += "</Transaction></TStream>";
		return xml;
	}

	private String startAdminXML() {
		seq += 1;
		String xml = "<?xml version=\"1.0\"?>";
		xml += "<TStream><Admin><MerchantID>" + merchID + "</MerchantID>";
		xml += "<POSPackageID>EMVUSCLIENT:1.26</POSPackageID>";
		xml += "<SecureDevice>CloudEMV2</SecureDevice>";
		xml += "<SequenceNo>" + seq + "</SequenceNo>";
		return xml;
	}
	private String completeAdminXML(String xml) {
		xml +="</Admin></TStream>";
		return xml;
	}
	
	public void processDeviceVersionCmd() {
		String msg=startTransactionXML();
		msg +="<TranCode>DeviceVersion</TranCode>";
		msg =completeTransactionXML(msg);
		System.out.println(msg);
		sendDatacapMessage(msg);
		
	}
	
	public void processBatchSummaryCmd() {
		String msg=startAdminXML();
		msg+="<TranType>Administrative</TranType><TranCode>BatchSummary</TranCode>";
		msg=completeAdminXML(msg);
		System.out.println(msg);
		sendDatacapMessage(msg);
	}
	public void processSAFStatCmd() {
		String msg=startAdminXML();
		msg+="<TranType>Administrative</TranType><TranCode>SAF_Statistics</TranCode>";
		msg=completeAdminXML(msg);
		System.out.println(msg);
		sendDatacapMessage(msg);
	}
		
	
	
	private void sendDatacapMessage(String msg) {
		//the results are put back in msg
		//connect
		System.out.println("sendDatacapMessage");
		String theURL = "http://"+this.termIP+":8080/ProcessEMVTransction";
		System.out.println("URL:"+theURL);
		URL url;
		HttpURLConnection connection;
		try {
			url = new URL(theURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
			connection.setConnectTimeout(10000); //10sec to connect
			//connection.setReadTimeout((int)((long)msg.timeoutSecs * 1000L)); //15sec timeout code
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//msg.setIsError(true);
			//msg.setErrorText(e.toString());
			e.printStackTrace();
			//updateCommStatus(false);
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			msg.setIsError(true);
//			msg.setErrorText(e.toString());
//			updateCommStatus(false);
			return;
		}
		
		//send
		OutputStream os;
		try {
			os = connection.getOutputStream();
			byte[] input = msg.getBytes("UTF-8");
			os.write(input, 0, input.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			msg.setIsError(true);
//			msg.setErrorText(e.toString());
			e.printStackTrace();
//			updateCommStatus(false);
			return;
		}
		
		//response code and text
		int status;
		try {
			status = connection.getResponseCode();
			System.out.println("Response Code: " + status);
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			msg.setIsE/rror(true);
//			msg.setErrorText(e.toString());
			e.printStackTrace();
//			updateCommStatus(false);
			return;
		}
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			String line;
			StringBuilder response = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				response.append(line).append("\n");
			}
			System.out.println("Response Body:\n" + response.toString());
//			msg.setFromPinPad(response.toString());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			msg.setIsError(true);
//			msg.setErrorText(e.toString());
//			updateCommStatus(false);
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			msg.setIsError(true);
//			msg.setErrorText(e.toString());
//			updateCommStatus(false);
			return;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
//		updateCommStatus(true);
		//done comms
	}
		
	
	
	
	//cmdline: <CMD> <term# or -i <ip>>
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CommandLine cl=new CommandLine();
		try {
			INIFile ini=INIFile.load("c:/ADX_IDT1/datacap.ini");
			if ("-i".equals(args[1])) {
				cl.termIP=args[2];
			} else {
				cl.termIP=ini.get("term"+args[1],"pinPadIP","127.0.0.1");
			}
			cl.merchID=ini.get("datacap","merchID","NOIDFOUND");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cl.processCommand(args[0]);	
		
	}

}
