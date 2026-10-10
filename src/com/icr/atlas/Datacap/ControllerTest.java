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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.icr.atlas.ATLASGuiExtend;

public class ControllerTest extends JPanel {

	private int seq = 625;
	private String pinPadIP = "192.168.5.190";
	private String merchID = "INTERNORF1GP";
	private String cardType = "";
	

	private JTextField amountField;

	static class PinPadRequest {
		final String xmlMsg;
		final Consumer<String> callback;
		final String msgType;

		PinPadRequest(String xml, String msgType, Consumer<String> callback) {
			this.xmlMsg = xml;
			this.callback = callback;
			this.msgType = msgType;
		}
	}

	static class PinPadController implements Runnable, AutoCloseable {

		private final BlockingQueue<PinPadRequest> queue = new LinkedBlockingQueue<>();
		private final Thread worker;
		private volatile boolean running = true;

		private String theURL = "http://192.168.5.190:8080/ProcessEMVTransction";

		public PinPadController() {
			this.worker = new Thread(this, "PinPadController");
		}

		public void start() {
			worker.start();
		}

		public void stop() {
			running = false;
			worker.interrupt();
		}

		public void submit(PinPadRequest request) {
			System.out.println("submitting:"+request);
			queue.offer(request);
		}

		@Override
		public void run() {
			while (running) {
				try {
					PinPadRequest req = queue.take();
					String result = handle(req);
					if (req.callback != null) {
						SwingUtilities.invokeLater(() -> req.callback.accept(result));
						running=false;//set running false here ? or call stop?
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
			return sendXML(req.xmlMsg);
		}

		private String sendXML(String xmlData) {
			String result = "??";
			HttpURLConnection connection = null;

			try {
				URL url = new URL(theURL);
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");

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
				e.printStackTrace();
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
			return result;
		}

		@Override
		public void close(){
			// TODO Auto-generated method stub
			stop();
		}
	}

	public ControllerTest() {
		setLayout(new BorderLayout(10, 10));

		JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
		JButton btn1 = new JButton("RESET PIN PAD");
		btn1.addActionListener(e -> resetPinPad());
		buttonPanel.add(btn1);

		JButton btn2 = new JButton("GET CHOICE");
		btn2.addActionListener(e -> getChoice());
		buttonPanel.add(btn2);

		JButton btn3 = new JButton("CANCEL");
		btn3.addActionListener(e -> cancelTransaction());
		buttonPanel.add(btn3);

		JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
		inputPanel.add(new JLabel("AMOUNT"));
		amountField = new JTextField();
		inputPanel.add(amountField);

		add(inputPanel, BorderLayout.NORTH);
		add(buttonPanel, BorderLayout.CENTER);
	}
	
	public void runAuto() {
		popUp("run Auto");
		resetPinPad();
	}

	private void resetPinPad() {
		String resetXML = startTransactionXML();
		resetXML += "<TranCode>EMVPadReset</TranCode>";
		resetXML = completeTransactionXML(resetXML);
		System.out.println(resetXML);
		popUp("send reset");
		PinPadController controller = new PinPadController();
		controller.start();
		controller.submit(new PinPadRequest(resetXML, "resetPinPad", this::resetPinPadCallback));
//		try (PinPadController controller=new PinPadController()) {
//			controller.start();
//			controller.submit(new PinPadRequest(resetXML, "resetPinPad", this::resetPinPadCallback));
//		}
		
	}

	private void resetPinPadCallback(String r) {
		System.out.println("resetPinPad CALLBACK");
		cardType = "";
		popUp("back");
		StringBuilder sb = new StringBuilder();
		sb.append("<plainPrintLines>");
		for(int i=1;i<3;i++) {
			sb.append("<Line"+i+">..........................LINE #"+i+"</Line"+i+"></plainPrintLines>");
		}
//		try {
		//new Thread(() ->{
			System.out.println("thread to send pipe data start");
			//ATLASGuiExtend.getInstance().sendAtlasPipeData(sb.toString());
			System.out.println("ending send thread");
		//}).start();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		//getChoice();
	}

	private void getChoice() {
		String choiceXML = startTransactionXML();
		choiceXML += "<TranCode>GetMultipleChoice</TranCode>";
		choiceXML += "<ButtonData>Credit/Debit,,EBT-FOOD,EBT-CASH,CANCEL</ButtonData>";
		choiceXML = completeTransactionXML(choiceXML);
		System.out.println(choiceXML);
		popUp("send getchoice");
		PinPadController controller = new PinPadController();
		controller.start();
		controller.submit(new PinPadRequest(choiceXML, "getChoice", this::getChoiceCallback));
//		try(PinPadController controller = new PinPadController()) {
//			controller.start();
//			controller.submit(new PinPadRequest(choiceXML, "getChoice", this::getChoiceCallback));
//		}
	}

	private void getChoiceCallback(String r) {
		System.out.println("getChoice CALLBACK" + getXML(r, "GetMultipleChoiceResponse"));
		String choice = getXML(r, "GetMultipleChoiceResponse");
		popUp("Choice:"+choice);
		if (choice.equalsIgnoreCase("Credit/Debit") || choice.equalsIgnoreCase("EBT-Food")
				|| choice.equalsIgnoreCase("EBT-Cash")) {
			cardType = choice;
			sendEMV();
		} else {
			if (getXML(r, "CmdStatus").equalsIgnoreCase("Error")) {
				errorPopUp(getXML(r, "TextResponse"));
				resetPinPad();
			} else {
				popUp("TRANSACTION CANCELLED");
				cancelTransaction();
			}
		}
	}

	private void cancelTransaction() {
		String cancel = startTransactionXML();
		cancel += "<TranCode>TransactionCancel</TranCode>";
		cancel = completeTransactionXML(cancel);
		System.out.println(cancel);
		PinPadController controller = new PinPadController();
		controller.start();
		controller.submit(new PinPadRequest(cancel, "cancelTransaction", result -> {
			resetPinPad();
		}));
	}

	private void sendEMV() {
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
		popUp("send EMVSale");
		PinPadController controller = new PinPadController();
		controller.start();
		controller.submit(new PinPadRequest(saleXML, "EMVSale", this::sendEMVCallback));
//		try(PinPadController controller = new PinPadController()) {
//			controller.start();
//			controller.submit(new PinPadRequest(saleXML, "EMVSale", this::sendEMVCallback));
//		}
	}

	private void sendEMVCallback(String r) {
		String cmdStatus = getXML(r, "CmdStatus");
		switch (cmdStatus.toUpperCase()) {
			case "ERROR":
				errorPopUp(getXML(r, "TextResponse"));
				resetPinPad();
				return;
			case "DECLINED":
				declinedPopUp(getXML(r, "TextResponse"));
				resetPinPad();
				return;
		}

		StringBuilder lines = new StringBuilder();
		for (int i = 1; i < 20; i++) {
			lines.append(getXML(r, "Line" + i)).append("\r\n");
		}

		popUp(lines.toString());
		StringBuilder sb = new StringBuilder();
		sb.append("<plainPrintLines>");
		for (int i = 1; i < 50; i++) {
			String l = getXML(r, "Line" + i);
			if (!l.isEmpty()) {
				sb.append("<Line").append(i).append(">").append(l).append("</Line").append(i).append(">");
			}
		}
		sb.append("<Line0>FORCE LINE 0</Line0>");
		sb.append("</plainPrintLines>");
		try {
			ATLASGuiExtend.getInstance().sendAtlasPipeData(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		resetPinPad();
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

	public void popUp(String msg) {
		JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
	}

	public void errorPopUp(String msg) {
		JOptionPane.showMessageDialog(this, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
	}

	public void declinedPopUp(String msg) {
		JOptionPane.showMessageDialog(this, msg, "DECLINED", JOptionPane.ERROR_MESSAGE);
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

	// Optional main() for standalone testing
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			javax.swing.JFrame f = new javax.swing.JFrame("Datacap TEST");
			f.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			f.add(new ControllerTest());
			f.setSize(400, 300);
			f.setVisible(true);
		});
	}
}
