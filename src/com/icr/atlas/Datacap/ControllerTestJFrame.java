package com.icr.atlas.Datacap;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.icr.atlas.ATLASGuiExtend;

public class ControllerTestJFrame extends JFrame {

	private int seq=625;
	private String pinPadIP="192.168.5.190";
	private String merchID="INTERNORF1GP";
	private String cardType="";
	
	private JTextField amountField;
	
	static class PinPadRequest {
		final String xmlMsg;
		final Consumer<String> callback;
		final String msgType;
		
		PinPadRequest(String xml,String msgType,Consumer<String> callback) {
			this.xmlMsg=xml;
			this.callback=callback;
			this.msgType=msgType;
		}
	}
	
	static class PinPadController implements Runnable {
		
		private final BlockingQueue<PinPadRequest> queue=new LinkedBlockingQueue();
		private final Thread worker;
		private volatile boolean running=true;
		

		private String theURL="http://192.168.5.190:8080/ProcessEMVTransction";

		public PinPadController() {
			this.worker=new Thread(this,"PinPadController");
		}
		
		public void start() {
			worker.start();
		}
		
		public void stop() {
			running=false;
			worker.interrupt();
		}
		
		public void submit(PinPadRequest request) {
			queue.offer(request);
		}

		@Override
		public void run() {
			while(running) {
				try {
					PinPadRequest req=queue.take();
					String result=handle(req);
					if (req.callback!=null) {
						//req.callback.accept(result);
						SwingUtilities.invokeLater(()->req.callback.accept(result));
					}
				} catch (InterruptedException e) {
					if (!running)
						break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		private String handle(PinPadRequest req) {
			String result="";
			result=sendXML(req.xmlMsg);
			
			return result;
		}
		private String sendXML(String xmlData) {
			String result="??";
			        HttpURLConnection connection = null;

			        try {
			            // Create the connection
			            URL url = new URL(theURL);
			            connection = (HttpURLConnection) url.openConnection();

			            // Configure the connection for a POST request
			            connection.setRequestMethod("POST");
			            connection.setDoOutput(true);
			            connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");

			            // Send the XML data
			            try (OutputStream os = connection.getOutputStream()) {
			                byte[] input = xmlData.getBytes("UTF-8");
			                os.write(input, 0, input.length);
			            }

			            // Get the response
			            int status = connection.getResponseCode();
			            System.out.println("Response Code: " + status);

			            // Read the response (if any)
			            try (BufferedReader reader = new BufferedReader(
			                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
			                String line;
			                StringBuilder response = new StringBuilder();
			                while ((line = reader.readLine()) != null) {
			                    response.append(line).append("\n");
			                }
			                System.out.println("Response Body:\n" + response.toString());
			                result=response.toString();
			            }

			        } catch (IOException e) {
			            e.printStackTrace();
			        } finally {
			            if (connection != null) {
			                connection.disconnect();
			            }
			        }
			return result;    		
		}	
		
	}
	
	public ControllerTestJFrame() {
		setTitle("Datacap TEST");
		setSize(400,300);
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//setLayout(new GridLayout(2,2,10,10));
		setLayout(new BorderLayout(10,10));
		
		JPanel buttonPanel=new JPanel(new GridLayout(2,2,10,10));
		JButton btn1=new JButton("RESET PIN PAD");
		btn1.addActionListener(e -> resetPinPad());
		buttonPanel.add(btn1);
		JButton btn2=new JButton("GET CHOICE");
		btn2.addActionListener(e -> getChoice());
		buttonPanel.add(btn2);
		JButton btn3=new JButton("CANCEL");
		btn3.addActionListener(e -> cancelTransaction());
		buttonPanel.add(btn3);
		
		JPanel inputPanel=new JPanel(new GridLayout(3,2,5,5));
		inputPanel.add(new JLabel("AMOUNT"));
		amountField=new JTextField();
		inputPanel.add(amountField);
		
		add(inputPanel,BorderLayout.NORTH);
		add(buttonPanel,BorderLayout.CENTER);
	}
	
 
	
	public void runAuto() {
			System.out.println("Starting AUTO");
			popUp("start auto");
			resetPinPad();
	}
	
	private void resetPinPad() {
		String resetXML=startTransactionXML();
		resetXML+="<TranCode>EMVPadReset</TranCode>";
		resetXML=completeTransactionXML(resetXML);
		System.out.println(resetXML);
		//String res=sendXML(resetXML);
		PinPadController controller=new PinPadController();
		controller.start();
		controller.submit(new PinPadRequest(resetXML,"resetPinPad",this::resetPinPadCallback));
	}
	private void resetPinPadCallback(String r) {
		System.out.println("resetPinPad CALLBACK");
		cardType="";
		
		getChoice();
	}
	
	private void getChoice() {
		String choiceXML=startTransactionXML();
		choiceXML+="<TranCode>GetMultipleChoice</TranCode>";
		choiceXML+="<ButtonData>Credit/Debit,,EBT-FOOD,EBT-CASH,CANCEL</ButtonData>";
		choiceXML=completeTransactionXML(choiceXML);
		System.out.println(choiceXML);
		PinPadController controller=new PinPadController();
		controller.start();
		controller.submit(new PinPadRequest(choiceXML,"getChoice",this::getChoiceCallback));
	}
	private void getChoiceCallback(String r) {
		System.out.println("getChoice CALLBACK"+getXML(r,"GetMultipleChoiceResponse"));
		//popUp(getXML(r,"GetMultipleChoiceResponse"));
		String choice=getXML(r,"GetMultipleChoiceResponse");
		if (choice.equalsIgnoreCase("Credit/Debit") || choice.equalsIgnoreCase("EBT-Food") ||choice.equalsIgnoreCase("EBT-Cash")) {
			cardType=choice;
			sendEMV();
		} else {
			if (getXML(r,"CmdStatus").equalsIgnoreCase("Error")) {
				errorPopUp(getXML(r,"TextResponse"));
				resetPinPad();
			}else {
				
				popUp("TRANSACTION CANCELLED");
				cancelTransaction();
			}

		}
	}

	private void cancelTransaction() {
		String cancel=startTransactionXML();
		cancel+="<TranCode>TransactionCancel</TranCode>";
		cancel=completeTransactionXML(cancel);
		System.out.println(cancel);
		PinPadController controller=new PinPadController();
		controller.start();
		//controller.submit(new PinPadRequest(cancel,"cancelTransaction",this::cancelTransactionCallback));
		controller.submit(new PinPadRequest(cancel,"cancelTransaction",result-> {
			resetPinPad();
		}));
	}
	private void cancelTransactionCallback(String r) {
		System.out.println("cancelTransaction CALLBACK");
		resetPinPad();
	}
	
	private void sendEMV() {
		String saleXML=startTransactionXML();
		saleXML+="<TranCode>EMVSale</TranCode>";
		if (cardType.equalsIgnoreCase("EBT-FOOD")) {
			saleXML+="<CardType>Foodstamp</CardType>";
			saleXML+="<TranType>EBT</TranType>";
		}
		saleXML+="<InvoiceNo>838383</InvoiceNo>";
		saleXML+="<RefNo>838383</RefNo>";
		saleXML+="<Amount><Purchase>6.00</Purchase></Amount>";
		//saleXML+="<Amount><Purchase>"+amountField.getText()+"</Purchase></Amount>";
		saleXML=completeTransactionXML(saleXML);
		PinPadController controller=new PinPadController();
		controller.start();
		controller.submit(new PinPadRequest(saleXML,"EMVSale",this::sendEMVCallback));
	};
	private void sendEMVCallback(String r) {
		//String res=sendXML(saleXML);
		//popUp(res);
		String cmdStatus=getXML(r,"CmdStatus");
//		if (cmdStatus.equalsIgnoreCase("Error")) {
//			errorPopUp(getXML(r,"TextResponse"));
//			resetPinPad();
//			return;
//		}
		switch (getXML(r,"CmdStatus").toUpperCase()) {
		case "ERROR":
			errorPopUp(getXML(r,"TextResponse"));
			resetPinPad();
			return;
		case "DECLINED":
			declinedPopUp(getXML(r,"TextResponse"));
			resetPinPad();
			return;
		}
		
		String lines="";
		for(int i=1;i<20;i++) {
			lines+=getXML(r,"Line"+i) +"\r\n";
		}
		
		popUp(lines);
		StringBuffer sb=new StringBuffer();
		sb.append("<plainPrintLines>");
		for(int i=1;i<50;i++) {
			String l=getXML(r,"Line"+i);
			if (l!="") {
				sb.append("<Line"+i+">"+l+"</Line"+i+">");
			}
		}
		sb.append("<Line0>FORCE LINE 0</Line0>");
		sb.append("</plainPrintLines>");
		popUp(sb.toString());
		try {
			ATLASGuiExtend.getInstance().sendAtlasPipeData(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//popUp(lines);
		resetPinPad();
		
		//int amt=Integer.parseInt(amountField.getText() )*100;
		
		
		
	}
	
	public void resetResult(String res) {
		System.out.println("resetResult recv:"+res);
	}
	
	private String startTransactionXML() {
		seq+=1;
		String xml="<?xml version=\"1.0\"?>";
		xml +="<TStream><Transaction><MerchantID>"+merchID+"</MerchantID>";
		xml +="<POSPackageID>EMVUSCLIENT:1.26</POSPackageID>";
		xml +="<SecureDevice>CloudEMV2</SecureDevice>";
		xml +="<SequenceNo>"+seq+"</SequenceNo>";
		
		return xml;
	}
	
	
	private String completeTransactionXML(String xml) {
		xml+="</Transaction></TStream>";
		return xml;
	}
	
	public void popUp(String msg) {
		JOptionPane.showMessageDialog(
				null,
				msg,
				"Info",
				JOptionPane.INFORMATION_MESSAGE);
	}
	public void errorPopUp(String msg) {
		JOptionPane.showMessageDialog(
				null,
				msg,
				"ERROR",
				JOptionPane.ERROR_MESSAGE);
	}
	public void declinedPopUp(String msg) {
		JOptionPane.showMessageDialog(
				null,
				msg,
				"DECLINED",
				JOptionPane.ERROR_MESSAGE);
		
	}

	
	public String getXML(String xml, String tag) {
		String result="";
	    String startTag = "<" + tag + ">";
	    String endTag = "</" + tag + ">";

	    int start = xml.indexOf(startTag);
	    int end = xml.indexOf(endTag);

	    if (start != -1 && end != -1 && end > start) {
	    	result=xml.substring(start+startTag.length(),end);
	    }
		
		return result;
	}
		
	
	//private static void onDeviceResult(String r) {
	private void onDeviceResult(String r) {
		System.out.println("callabck"+r);
	}
	
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new ControllerTestJFrame().setVisible(true);
		});
	}
}
