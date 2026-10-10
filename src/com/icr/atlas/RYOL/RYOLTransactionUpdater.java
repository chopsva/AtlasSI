package com.icr.atlas.RYOL;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.DiscountEvent;
import com.ibm.retail.AEF.event.ItemSalesEvent;
import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.retail.AEF.event.TenderEvent;
import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.ibm.retail.AEF.event.TransactionTotalsEvent;
import com.ibm.retail.si.util.AEFException;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.eventPublishers.AtlasCouponEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCustomerEventPublisher;
import com.icr.atlas.eventPublishers.AtlasDiscountEventPublisher;
import com.icr.atlas.eventPublishers.AtlasItemSalesEvent;
import com.icr.atlas.eventPublishers.AtlasItemSalesEventPublisher;
import com.icr.atlas.eventPublishers.AtlasOperatorEventPublisher;
import com.icr.atlas.eventPublishers.AtlasSubscriber;
import com.icr.atlas.eventPublishers.AtlasTenderEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionStatusEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionTotalsEventPublisher;
import com.icr.atlas.eventPublishers.AtlasWorkstationStatusEventPublisher;

// TODO  options
//     only send with customer


public class RYOLTransactionUpdater implements AtlasSubscriber<Object> {
	private List<AtlasItemSalesEvent> items;
	private List<TenderEvent> tenders;
	private List<DiscountEvent> discounts;
	private boolean haveCustomer=false;
	private String customerId="";
	private TransactionTotalsEvent lastTotalsEvent;
	private String currentTransId="";
	private String lastOperator="";
	private RYOLTransactionSender transactionSender;
	
	public RYOLTransactionUpdater() {
		// TODO Auto-generated constructor stub
		AtlasCustomerEventPublisher.getInstance().subscribe(this);
		AtlasItemSalesEventPublisher.getInstance().subscribe(this);
		AtlasTransactionStatusEventPublisher.getInstance().subscribe(this);
		AtlasTenderEventPublisher.getInstance().subscribe(this);
		//AtlasWorkstationStatusEventPublisher.getInstance().subscribe(this);
		AtlasCouponEventPublisher.getInstance().subscribe(this);
		AtlasOperatorEventPublisher.getInstance().subscribe(this);
		AtlasTransactionTotalsEventPublisher.getInstance().subscribe(this);
		AtlasDiscountEventPublisher.getInstance().subscribe(this);		
		System.out.println("RYOLTransactionUpdater init!");
		items=new ArrayList<>();
		tenders=new ArrayList<>();
		discounts=new ArrayList<>();
		transactionSender=new RYOLTransactionSender();
	}

	@Override
	public void update(Object o) {
		// TODO Auto-generated method stub
		if (o instanceof TransactionTotalsEvent) {
			lastTotalsEvent=(TransactionTotalsEvent)o;
		}
		if (o instanceof AtlasItemSalesEvent) {
			items.add((AtlasItemSalesEvent)o);
		}
		if (o instanceof TenderEvent) {
			tenders.add((TenderEvent)o);
		}
		if (o instanceof CustomerEvent ) {
			customerId=((CustomerEvent)o).getID();
			System.out.println("customerID:"+customerId+":");
			if (customerId!="") {
				haveCustomer=true;
			} else {
				haveCustomer=false;
			}
		}
		if (o instanceof OperatorEvent) {
			try {
				lastOperator=((OperatorEvent)o).getID();
				System.out.println("lastOperator now:"+lastOperator);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void clearValues() {
		System.out.println("clearValues");
		items.clear();
		tenders.clear();
		discounts.clear();
		haveCustomer=false;
		customerId="";
		lastTotalsEvent=null;
	}
	
	private String createHeader() {
		System.out.println("createHeader");
		String keyDate=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
		String tranKey="";
		try {
			tranKey=keyDate+"-"+Integer.parseInt(ATLASGuiExtend.getInstance().getTermId())+"-"+Integer.parseInt(currentTransId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String result="";
		result="\"header\":{";
		result+="\"customerFound\":"+String.valueOf(haveCustomer)+",";
		result+="\"tranKey\":\""+tranKey+"\",";
		result+="\"operator\":\""+lastOperator+"\",";
		result+="\"termId\":"+Integer.parseInt(ATLASGuiExtend.getInstance().getTermId()) +",";
		result+="\"transNum\":"+Integer.parseInt(currentTransId)+",";
		result+="\"transDate\":\""+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))+"\",";
		String transTotal="0.00";
		if (lastTotalsEvent!=null) {
			transTotal=lastTotalsEvent.getTotal();
		}
		result+="\"transTotal\":"+transTotal+",";
		result+="\"customerId\":\""+customerId+"\"";
		result+="}";

		return result;
	}
	
	private String createItem(AtlasItemSalesEvent event) {
		System.out.println("createItem");
		ItemSalesEvent ise=event.getItemSalesEvent();
		String itemType="ITEM";
		try {
			if (ise.isVoided()) {
				itemType="VOID";
			}
			if (ise.isReturn()) {
				itemType="RFND";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String result="{";
		result +="\"itemcode\":\""+ise.getItemID()+"\",";
		result +="\"amount\":"+ise.getExtendedPrice()+",";
		result +="\"descr\":\""+ise.getDescription()+"\","; ///need JSON safe here
		result +="\"dept\":"+Integer.parseInt(ise.getDepartmentNumber())+",";
		result +="\"itemtype\":\""+itemType+"\",";
		try {
			result +="\"qty\":"+ise.getQuantity()+",";
		} catch (AEFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!ise.getWeightOrigin().equalsIgnoreCase("CALCULATED")) {
			result+="\"isWeight\": true,";
			result+="\"weight\":"+ise.getWeight();
		} else {
			result+="\"isWeight\": false,";
			result+="\"weight\":0";
			
		}
		result+="}";
		
		return result;
	}
	
	private String createItems() {
		System.out.println("createItems");
		String result="\"items\":[";
		for(int i=0;i<items.size();i++) {
			if (i!=0) {
				result+=",";
			}
			result +=createItem(items.get(i));
		}
		result+="]";
		return result;
	}
	
	private String createTender(TenderEvent event) {
		System.out.println("createTender");
		String result="{";
		result +="\"amount\":"+event.getAmount()+",";
		result +="\"tenderId\":"+event.getTenderType()+event.getTenderVariety();
		
		result+="}";
		return result;
	}
	
	private String createTenders() {
		System.out.println("createTenders");
		String result="\"tenders\":[";
		for(int i=0;i<tenders.size();i++) {
			if(i!=0) {
				result+=",";
			}
			result +=createTender(tenders.get(i));
		}
		
		result +="]";
		return result;
	}
	
	private String createTaxes() {
		//would like all taxes but we only have combined taxes right now
		System.out.println("createTaxes");
		String result="\"tax\":{";
		if (lastTotalsEvent != null) {
			result+="\"taxableAmountA\":0.00,";
			result+="\"taxAmountA\":0.00,";
			result+="\"taxableAmountB\":0.00,";
			result+="\"taxAmountB\":0.00,";
			result+="\"taxableAmountC\":0.00,";
			result+="\"taxAmountC\":0.00,";
			result+="\"taxableAmountD\":0.00,";
			result+="\"taxAmountC\":0.00,";
			result+="\"taxAmountCombined\":"+ lastTotalsEvent.getTax();
		}
		result+="}";
		
		return result;
	}
	
	private String createTransactionJSON() {
		System.out.println("createTransactionJSON");
		String result="";
		String theHeader=createHeader();
		String theItems=createItems();
		String theTenders=createTenders();
		String theTaxes=createTaxes();
		//System.out.println("theHeader:"+theHeader);
		//System.out.println("theItems:"+theItems);
		//System.out.println("theTenders:"+theTenders);
		result="{"+theHeader+","+theItems+","+theTenders+","+theTaxes+"}";
		System.out.println(result);
		return result;
	}
	
	public void transactionStart(TransactionStatusEvent event) {
		
		clearValues();
		currentTransId=event.getID();
	}
	
	public void transactionEnd(TransactionStatusEvent event) {
		String json=createTransactionJSON();
		transactionSender.sendTransaction(json);
	}
	
//	private void sendTransaction(String theTrans) {
//			String theBody="{\"transactions\":["+theTrans+"]}";
//			System.out.println("attempt to send:"+theBody);
//			int rc=sendHTTPTest(theBody);
//			
//	}
//	
//	private int sendHTTPTest(String theBody) {
//		int responseCode=-1;
//		
//		try {
//			URL url=new URL("http://ryol:8888/api/posTrans");
//			HttpURLConnection conn=(HttpURLConnection)url.openConnection();
//			conn.setConnectTimeout(5000);
//			conn.setRequestMethod("POST");
//			conn.setRequestProperty("Content-Type", "application/json");
//			conn.setDoOutput(true);
//			OutputStream os=conn.getOutputStream();
//			byte[] toSend=theBody.getBytes();
//			os.write(toSend,0,toSend.length);
//			responseCode=conn.getResponseCode();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		System.out.println("ResponseCode:"+responseCode);
//		return responseCode;
//	}

}
