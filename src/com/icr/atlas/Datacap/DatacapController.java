package com.icr.atlas.Datacap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.Datacap.ControllerTest.PinPadController;
import com.icr.atlas.Datacap.ControllerTest.PinPadRequest;
import com.icr.atlas.Utils.INIFile;
import com.icr.atlas.eventPublishers.AtlasSubscriber;
import com.icr.atlas.eventPublishers.AtlasTransactionStatusEventPublisher;

///may want a response class with things like isError, errorText, errorCode, RStream data, etc so that 
///a call can return error or good stuff the same way 

public class DatacapController implements AtlasSubscriber<Object> {
	
	private int seq = 625;
	private String pinPadIP = "192.168.5.190";
	private String merchID = "INTERNORF1GP";
	private Boolean forceOffline=false;
	private String commDescr="DATACAP LINK";
	//private String theURL = "http://192.168.5.190:8080/ProcessEMVTransction";

///////////////////////////////////////////////////
	class DatacapMessage {
		private boolean isError=false;
		private String errorText="";
		//errorType 
		public int timeoutSecs=15;
		public String toPinPad="";
		public String fromPinPad="";
		
		public DatacapMessage() {
			//nothing
		}
		public DatacapMessage(String msg) {
			this.toPinPad=msg;
		}
		public String getToPinPad() {
			return this.toPinPad;
		}
		public void setFromPinPad(String msg) {
			this.fromPinPad=msg;
		}
		public String getFromPinPad() {
			return this.fromPinPad;
		}
		public boolean isError() {
			return this.isError;
		}
		public void setIsError(boolean e) {
			this.isError=e;
		}
		public void setErrorText(String t) {
			this.errorText=t;
		}
		public String getErrorText() {
			return this.errorText;
		}
		
		public String getCmdStatus() {
			String result="";
//			//most calls will want this data
//			String rStream=getXML(this.fromPinPad,"RStream");
//			if (rStream!="") {
//				result=getXML(rStream,"CmdStatus");
//			}
			result=getResponseTag("CmdStatus");
			return result;
		}
		
		public String getResponseTag(String tag) {
			//return a value from the pin pad response
			String result="";
			String rStream=getXML(this.fromPinPad,"RStream");
			if (rStream!="") {
				result=getXML(rStream,tag);
			}
			return result;
		}
		
		public String getXML(String xml, String tag) {
			String result = "";
			String startTag = "<" + tag + ">";
			String endTag = "</" + tag + ">";

			int start = xml.indexOf(startTag);
			int end = xml.indexOf(endTag);

			if (start != -1 && end != -1 && end > start) {
				result = xml.substring(start + startTag.length(), end);
			}

			return result;
		}
		
		@Override
		public String toString() {
			String result="DATACAP MESSAGE:\r\n";
			result +="  isError:"+this.isError+"\r\n";
			result +="  errorText:"+this.errorText+"\r\n";
			result +="  toPinPad:"+this.toPinPad+"\r\n";
			result +="  timeoutSecs:"+this.timeoutSecs+"\r\n";
			result +="  fromPinPad:"+this.fromPinPad+"\r\n";
			return result;
		}
		
	}//end DatacapMessage
///////////////////////////////////////////////////////////////////////////////	
	
	private static DatacapController instance=null;
	public static DatacapController getInstance() {
		if (instance==null) {
			instance=new DatacapController();
		}
		return instance;
	}
	
	private DatacapController() {
		// TODO Auto-generated constructor stub
		System.out.println("Datacap Controller init");
		AtlasTransactionStatusEventPublisher.getInstance().subscribe(this);
		readINI();
		System.out.println("==========================");
		System.out.println("pinPadIP:"+this.pinPadIP);
		System.out.println("merchID:"+this.merchID);
		System.out.println("==========================");
	}
	
	private void readINI() {
		System.out.println("readINI");
		try {
			INIFile ini=INIFile.load("R::ADX_IDT1:datacap.ini");
			this.merchID=ini.get("datacap", "merchID",merchID);
			String thisTerm=ATLASGuiExtend.getInstance().getTermId();
			System.out.println("thisTerm:"+thisTerm);
			
			this.pinPadIP=ini.get("term"+thisTerm,"pinPadIP",pinPadIP);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void update(Object o) {
		// messages from ATLAS
		
	}
	public void transactionStart(TransactionStatusEvent event) {
		System.out.println("TRANSACTION START SEEN");
		DatacapMessage reset=resetPinPad();
		System.out.println("result isError:"+reset.isError());
	}
	public void transactionEnd(TransactionStatusEvent event) {
		System.out.println("TRANSACTION END SEEN");
		DatacapMessage reset=resetPinPad();
		System.out.println("result isError:"+reset.isError());
	}
	public String handlePOSMessage(String msg) {
		System.out.println("handlePOSMessage:"+msg);
		StringBuffer result=new StringBuffer();
		
		if (msg.contains("<ResetPinPad")) {
			DatacapMessage reset=resetPinPad();
			System.out.println("reset result:"+reset.toString());
			//result.append("<ResetPinPadResult>"+ reset+"</ResetPinPadResult>");
			if (reset.isError()) {
				result.append("<ACEPrompt><promptText>DC- CHECK PIN PAD "+reset.getErrorText()+"</promptText></ACEPrompt>");
			}
		}
		
		if(msg.contains("<ProcessPayment>")) {
			System.out.println("ProcessPayment found");
			String tmp=processTransaction(msg);
			System.out.println("tmp:"+tmp);
			result.append(tmp);
		}
		
		return result.toString();
	}
	private void updateCommStatus(boolean status) {
		if (!status) {
			ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.ERROR);
		} else {
			ATLASGuiExtend.getInstance().setCommState(ATLASGuiExtend.CommState.GOOD);
		}
	}
	
	
	public DatacapMessage resetPinPad() {
		String resetXML = startTransactionXML();
		resetXML += "<TranCode>EMVPadReset</TranCode>";
		resetXML = completeTransactionXML(resetXML);
		DatacapMessage msg=new DatacapMessage(resetXML);
		sendDatacapMessage(msg);
		if (!msg.isError()) {
			if (!msg.getCmdStatus().equalsIgnoreCase("Success")) {
				msg.setIsError(true);
				msg.setErrorText("not success!");
			}
 		}
		
		return msg;
	}
	
	
	
	private DatacapMessage cancelTransaction() {
		String cancel = startTransactionXML();
		cancel += "<TranCode>TransactionCancel</TranCode>";
		cancel = completeTransactionXML(cancel);
		System.out.println(cancel);
		DatacapMessage msg=new DatacapMessage(cancel);
		sendDatacapMessage(msg);
		return msg; //caller handle 
	}

	
	private DatacapMessage getChoice() {
		String choiceXML = startTransactionXML();
		choiceXML += "<TranCode>GetMultipleChoice</TranCode>";
		choiceXML += "<ButtonData>Credit/Debit,,EBT-FOOD,EBT-CASH,FSA,CANCEL</ButtonData>";
		choiceXML = completeTransactionXML(choiceXML);
		System.out.println(choiceXML);
		DatacapMessage msg=new DatacapMessage(choiceXML);
		sendDatacapMessage(msg);
		//let caller handle result
		return msg;
	}
	
	
	private String processTransaction(String msg) {
		//get data from pos
		String balanceDue=getXML(msg,"balanceDue");
		String taxAmount=getXML(msg,"taxes");
		String foodstampTotal=getXML(msg,"foodstampTotal");
		String thisTransaction=getXML(msg,"transactionNumber");
		String rxTotal="";
		String qhpTotal="";
		
		String result="";
		DatacapMessage choice=getChoice();
		if (choice.isError) {
			result+="<ACEPrompt><promptText>"+choice.getErrorText()+"</promptText></ACEPrompt>";
			DatacapMessage reset=resetPinPad();
			return result;
		}
		String theChoice=getXML(choice.getFromPinPad(),"GetMultipleChoiceResponse");
		System.out.println("CHOICE:"+theChoice);
		if (theChoice=="") {
			DatacapMessage reset=resetPinPad();
			if (reset.isError()) {
				result="<ACEPrompt><promptText>"+choice.getErrorText()+"</promptText></ACEPrompt>";
			} else {
				result="<ACEPrompt><promptText>NO CHOICE FROM PIN PAD</promptText></ACEPrompt>";
			}
			return result;
		}
		if (theChoice.equalsIgnoreCase("CANCEL")) {
			DatacapMessage cancel=cancelTransaction();
			if (cancel.isError()) {
				result="<ACEPrompt><promptText>"+choice.getErrorText()+"</promptText></ACEPrompt>";
			} else {
				result="<ACEPrompt><promptText>CANCELLED AT PIN PAD</promptText></ACEPrompt>";
			}
			DatacapMessage reset=resetPinPad();
			return result;
		}
		String thisAmount=balanceDue;
		if (theChoice.equalsIgnoreCase("EBT-FOOD")) {
			System.out.println("changing to foodstamp total:"+foodstampTotal);
			thisAmount=foodstampTotal;
		}
		if (theChoice.equalsIgnoreCase("FSA")) { //adding for cert only
			rxTotal=getXML(msg,"rxTotal");
			qhpTotal=getXML(msg,"qhpTotal");
			float rxTmp=Float.parseFloat(rxTotal);
			float qhpTmp=Float.parseFloat(qhpTotal);
			if (rxTmp==0 & qhpTmp==0) {
				result="<ACEPrompt><promptText>NO QUALIFTING TOTAL FOR FSA</promptText></ACEPrompt>";
				DatacapMessage reset=resetPinPad();
				return result;
			}
		}
		
		String emvSaleResponse=sendEMVSale(theChoice,thisAmount,thisTransaction,rxTotal,qhpTotal);
		result=emvSaleResponse;
		
		return result;
	}
	
	private String sendEMVSale(String cardType,String amount,String trans,String rxAmt,String qhpAmt) {
		String result="";
		
		String saleXML = startTransactionXML();
		saleXML += "<TranCode>EMVSale</TranCode>";
		if (cardType.equalsIgnoreCase("EBT-FOOD")) {
			saleXML += "<CardType>Foodstamp</CardType>";
			saleXML += "<TranType>EBT</TranType>";
		} 
		if (cardType.equalsIgnoreCase("FSA")) {
			BigDecimal rxTmp=new BigDecimal(rxAmt);
			BigDecimal qhpTmp=new BigDecimal(qhpAmt);
			BigDecimal fsaTmp=rxTmp.add(qhpTmp);
			String fsaTotal=fsaTmp.setScale(2,RoundingMode.HALF_UP).toPlainString();
			
		}
		saleXML += "<InvoiceNo>"+trans.trim()+"</InvoiceNo>";
		saleXML += "<RefNo>"+trans.trim()+"</RefNo>";
		//saleXML += "<Amount><Purchase>" + amountField.getText() + "</Purchase></Amount>";
		saleXML += "<Amount><Purchase>"+amount.trim()+"</Purchase>";
		if (cardType.equalsIgnoreCase("FSA")) { //adding for cert only
			BigDecimal rxTmp=new BigDecimal(rxAmt);
			BigDecimal qhpTmp=new BigDecimal(qhpAmt);
			BigDecimal fsaTmp=rxTmp.add(qhpTmp);
			String fsaTotal=fsaTmp.setScale(2,RoundingMode.HALF_UP).toPlainString();
			saleXML +="<FSAWithLookup>"+fsaTotal+"</FSAWithLookup>";
			if (rxTmp.compareTo(BigDecimal.ZERO)!=0) {
				saleXML +="<FSAPrescription>"+rxAmt+"</FSAPrescription>";
			}
			if (qhpTmp.compareTo(BigDecimal.ZERO)!=0) {
				saleXML +="<FSAClinical>"+qhpAmt+"</FSAClinical>";
			}
		}
		saleXML +="</Amount>";
		if (forceOffline) {
			saleXML+="<ForceOffline>Y</ForceOffline><MaxTransactions>9999</MaxTransactions><OfflineTransactionPurchaseLimit>100.00</OfflineTransactionPurchaseLimit>";
		}
		saleXML +="<OKAmount>Disallow_All</OKAmount>"; //no amount ok prompt
		
		saleXML = completeTransactionXML(saleXML);
		System.out.println("toPP:"+saleXML);
		DatacapMessage msg=new DatacapMessage(saleXML);
		msg.timeoutSecs=180;
		sendDatacapMessage(msg);
		if (msg.isError) {
			result="<ACEPrompt><promptText>"+msg.getErrorText()+"</promptText></ACEPrompt>";
			DatacapMessage reset=resetPinPad();
			return result;
		}		
		result="<ACEPrompt><promptText>"+getXML(msg.getFromPinPad(),"TextResponse")+"</promptText></ACEPrompt>";
		StringBuilder sb=new StringBuilder();
		sb.append("<plainPrintLines>");
		for(int i=1;i<50;i++) {
			String l=getXML(msg.getFromPinPad(),"Line"+i);
			if (!l.isEmpty()) {
				if (l.startsWith(".")) {
					l=l.substring(1);
				}
				l=l.replaceFirst(" {3}"," ");
				sb.append("<Line").append(i).append(">").append(l).append("</Line").append(i).append(">");
			}
		}
		sb.append("<Line0>.....DATACAP DATA.....</Line0>");
		sb.append("</plainPrintLines>");
		result +=sb.toString();
		if (msg.getCmdStatus().equalsIgnoreCase("Approved")) {
			//String addTender="<addTender><tenderID>41</tenderID><tenderAmount>"+getXML(msg.getFromPinPad(),"Authorize")+"</tenderAmount></addTender>";
			String addTender="<addTender><CardType>"+getXML(msg.getFromPinPad(),"CardType")+"</CardType><tenderAmount>"+getXML(msg.getFromPinPad(),"Authorize")+"</tenderAmount></addTender>";
			result +=addTender;
		}
		DatacapMessage reset=resetPinPad();
		return result;
		
		
	}
	
	private String sendEMVSaleOLD(String cardType) {
		String result="";
	
		String saleXML = startTransactionXML();
		saleXML += "<TranCode>EMVSale</TranCode>";
		if (cardType.equalsIgnoreCase("EBT-FOOD")) {
			saleXML += "<CardType>Foodstamp</CardType>";
			saleXML += "<TranType>EBT</TranType>";
		}
		saleXML += "<InvoiceNo>838383</InvoiceNo>";
		saleXML += "<RefNo>838383</RefNo>";
		//saleXML += "<Amount><Purchase>" + amountField.getText() + "</Purchase></Amount>";
		saleXML += "<Amount><Purchase>6.00</Purchase></Amount>";
		saleXML = completeTransactionXML(saleXML);
		String saleResponse=sendXML(saleXML,180); //3min timeout
		String cmdStatus = getXML(saleResponse, "CmdStatus");
		switch (cmdStatus.toUpperCase()) {
			case "ERROR":
				String err=getXML(saleResponse, "TextResponse");
				result="<ACEPrompt><PromptText>"+err+"</PromptText>";
				return result;
			case "DECLINED":
				String dcl=getXML(saleResponse, "TextResponse");
				result="<ACEPrompt><PromptText>"+dcl+"</PromptText>";
				resetPinPad();
				return result;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<plainPrintLines>");
		for (int i = 1; i < 50; i++) {
			String l = getXML(saleResponse, "Line" + i);
			if (!l.isEmpty()) {
				sb.append("<Line").append(i).append(">").append(l).append("</Line").append(i).append(">");
			}
		}
		sb.append("<Line0>FORCE LINE 0</Line0>");
		sb.append("</plainPrintLines>");
		result=sb.toString();
		
		return result;
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

	private void sendDatacapMessage(DatacapMessage msg) {
		//the results are put back in msg
		//connect
		System.out.println("sendDatacapMessage");
		String theURL = "http://"+this.pinPadIP+":8080/ProcessEMVTransction";
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
			connection.setReadTimeout((int)((long)msg.timeoutSecs * 1000L)); //15sec timeout code
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			msg.setIsError(true);
			msg.setErrorText(e.toString());
			e.printStackTrace();
			updateCommStatus(false);
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			msg.setIsError(true);
			msg.setErrorText(e.toString());
			updateCommStatus(false);
			return;
		}
		
		//send
		OutputStream os;
		try {
			os = connection.getOutputStream();
			byte[] input = msg.getToPinPad().getBytes("UTF-8");
			os.write(input, 0, input.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			msg.setIsError(true);
			msg.setErrorText(e.toString());
			e.printStackTrace();
			updateCommStatus(false);
			return;
		}
		
		//response code and text
		int status;
		try {
			status = connection.getResponseCode();
			System.out.println("Response Code: " + status);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			msg.setIsError(true);
			msg.setErrorText(e.toString());
			e.printStackTrace();
			updateCommStatus(false);
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
			msg.setFromPinPad(response.toString());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			msg.setIsError(true);
			msg.setErrorText(e.toString());
			updateCommStatus(false);
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			msg.setIsError(true);
			msg.setErrorText(e.toString());
			updateCommStatus(false);
			return;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		updateCommStatus(true);
		//done comms
	}
	
	
	private String sendXML(String xmlData) {
		return sendXML(xmlData,15);
	}
	
	private String sendXML(String xmlData,int readTimeoutSecs){
		String theURL = "http://"+this.pinPadIP+":8080/ProcessEMVTransction";
		String result = "";
		HttpURLConnection connection = null;
		System.out.println("sendXML called");
		try {
			URL url = new URL(theURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
			connection.setConnectTimeout(10000); //10sec to connect
			connection.setReadTimeout((int)((long)readTimeoutSecs * 1000L)); //15sec timeout code

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = xmlData.getBytes("UTF-8");
				os.write(input, 0, input.length);
			}

			int status = connection.getResponseCode();
			System.out.println("Response Code: " + status);

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
				String line;
				StringBuilder response = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					response.append(line).append("\n");
				}
				System.out.println("Response Body:\n" + response.toString());
				result = response.toString();
			}

		} catch (IOException e) {
			System.out.println("IOEXCEPTION");
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return result;
	}
	
	public String getXML(String xml, String tag) {
		String result = "";
		String startTag = "<" + tag + ">";
		String endTag = "</" + tag + ">";

		int start = xml.indexOf(startTag);
		int end = xml.indexOf(endTag);

		if (start != -1 && end != -1 && end > start) {
			result = xml.substring(start + startTag.length(), end);
		}

		return result;
	}
	
	public boolean resetPinPadOld() {
		System.out.println("resetPinPad called");
		boolean result=true;
		String resetXML = startTransactionXML();
		resetXML += "<TranCode>EMVPadReset</TranCode>";
		resetXML = completeTransactionXML(resetXML);
		System.out.println(resetXML);
		String resetResponse=sendXML(resetXML);
		System.out.println(resetResponse);
		
		result=false; //bad until told good
		String rStream=getXML(resetResponse,"RStream");
		System.out.println("RStream:"+rStream);
		if (rStream!="") {
			String cmdStatus=getXML(rStream,"CmdStatus");
			System.out.println("cmdStatus:"+cmdStatus);
			if (cmdStatus.equalsIgnoreCase("Success")) {
				result=true;
			}
		}
		System.out.println("reset result:"+result);
		return result;
	}
	
	private void cancelTransactionOLD() {
		String cancel = startTransactionXML();
		cancel += "<TranCode>TransactionCancel</TranCode>";
		cancel = completeTransactionXML(cancel);
		System.out.println(cancel);
		String cancelResponse=sendXML(cancel);
		//don't care about reponse
		boolean resetReponse=resetPinPadOld();
		
	}	
	private String getChoiceOLD() {
		String result="";
		String choiceXML = startTransactionXML();
		choiceXML += "<TranCode>GetMultipleChoice</TranCode>";
		choiceXML += "<ButtonData>Credit/Debit,,EBT-FOOD,EBT-CASH,CANCEL</ButtonData>";
		choiceXML = completeTransactionXML(choiceXML);
		System.out.println(choiceXML);
		String choiceResponse=sendXML(choiceXML);
		String rStream=getXML(choiceResponse,"RStream");
		if (rStream!="") {
			String theChoice=getXML(rStream,"GetMultipleChoiceResponse");
			if (theChoice.equalsIgnoreCase("Credit/Debit") ||
				theChoice.equalsIgnoreCase("EBT-Food") ||
				theChoice.equalsIgnoreCase("EBT-Cash") ) {
				result=theChoice;
			} else {
				if (getXML(choiceResponse,"CmdStatus").equalsIgnoreCase("Error")) {
					result="*ERROR";
					resetPinPad();
				} else {
					result="*CANCELED";
					cancelTransaction();
				}
			}
		}
		return result;
	}

	

	
}
