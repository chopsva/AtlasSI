package com.icr.atlas.AtlasSecurity.SIA;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.json.JSONObject;

import com.ibm.retail.AEF.event.CouponEvent;
import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.DiscountEvent;
import com.ibm.retail.AEF.event.ItemSalesEvent;
import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.retail.AEF.event.TenderEvent;
import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.ibm.retail.AEF.event.TransactionTotalsEvent;
import com.ibm.retail.si.util.AEFException;
import com.icr.atlas.AtlasSecurity.AtlasSecurityBasicUpdater;
import com.icr.atlas.AtlasSecurity.AtlasSecurityLogger;
import com.icr.atlas.AtlasSecurity.Base.AtlasSecurityHTTPClient;
import com.icr.atlas.AtlasSecurity.Base.AtlasSecuritySocketClient;
import com.icr.atlas.AtlasSecurity.Base.AtlasSecurityUpdaterBase;
import com.icr.atlas.eventPublishers.AtlasItemSalesEvent;

/* originally was going to extend AtalsSecurityBasicUpdater but 
 * super() calls would put wrong data in the working object so
 * copied the BasicUpdater and made changes as needed
 */

public class AtlasSecuritySIAUpdater extends AtlasSecurityUpdaterBase<Object>{
	
	protected String currentOperator="";
	private String transportType="tcp";
	private JSONObject eventData;
	private String eventBlockList="";
	

	public AtlasSecuritySIAUpdater() {
		super();
		init(); //need this because super() inits its vars BEFORE this class does it's init so anything being set
				//above for this class will be set AFTER super() is called. Moving all the init stuff in the 
				//base allows this class to do its init before we start reading props etc. 
		
		AtlasSecurityLogger.getInstance().info("AtlasSecuritySIAUpdater started");
		
		if (this.AtlasSecurityURL.startsWith("tcp")) {
			initAtlasSecurityClient(new AtlasSecuritySocketClient(false));
			((AtlasSecuritySocketClient)this.AtlasSecurityClientBase).setKeepSocketOpen(true);
		} else {
			initAtlasSecurityClient(new AtlasSecurityHTTPClient(false));
		}
		AtlasSecurityLogger.getInstance().info("eventBLockList:"+this.eventBlockList);

		
	}
	
	@Override
	public void readPropsExtend(Properties theProps) {
		System.out.println("AtlasSecuritySIAUpdater readPropsExtend");
		System.out.println("props eventBlockList:"+theProps.getProperty("eventBlockList", "nope!"));
		this.eventBlockList=theProps.getProperty("eventBlockList",this.eventBlockList);	
		System.out.println("  this.eventBlockList:"+this.eventBlockList);
		
	}
	
	protected void initializeJSONMessage(String eventType) {
		super.initializeJSONMessage();
		JSONObject headerObj=new JSONObject();
		//headerObj.put("timeStamp",LocalDateTime.now().format(theDateTimeFormatter) );
		headerObj.put("ts",DateTimeFormatter.ISO_INSTANT.format(Instant.now()) );

		headerObj.put("lane", this.thisTerminalNumber);
		headerObj.put("msgSeqNo", this.AtlasSecuritySeqCounter);
		if (this.currentTransId!="") {
			headerObj.put("txId",Integer.parseInt(this.currentTransId));
		}
		if (this.currentOperator!="") {
			headerObj.put("operatorId", this.currentOperator);
		}
		if (eventType!="") {
			this.messageObject.put("eventType", eventType);
		}
		this.messageObject.put("header", headerObj);
		this.eventData=new JSONObject();
	}
	
	public void addToEventJSON(String key,Object value) {
		if (this.eventData==null) {
			this.eventData=new JSONObject();
		}
		this.eventData.put(key, value);
	}
	
	@Override
	public void sendTheMessage() {
		//complete our message object
		if (this.eventData!=null) {
			addToJSONMessage("eventData", this.eventData);
		}
		//TODO- wrap in "event" object?
		String thisEventType=this.messageObject.getString("eventType");
		if (eventBlockList.contains(thisEventType)) {
			//AtlasSecurityLogger.getInstance().debug("would BLOCK this message:"+thisEventType);
			AtlasSecurityLogger.getInstance().debug("EventType:"+thisEventType+" blocked by eventBlockList");
			return;
		}
		//allow heartbeats to try
		if (!thisEventType.equalsIgnoreCase("heartbeat") && blockCommunications) {
			AtlasSecurityLogger.getInstance().debug("Message "+thisEventType+" blocked by blockCommunications");
			return;
		}
		
		super.sendTheMessage();
	}
	
	
	@Override
	protected void handleCustomerEvent(CustomerEvent event) {
		super.handleCustomerEvent(event); //just logs it, but might need mommy
		initializeJSONMessage("customer");
		this.eventData.put("customerId", event.getID());
		this.eventData.put("customerName","");
		this.eventData.put("currentPoints",0);
		sendTheMessage();
	}
	
	@Override
	protected void handleAtlasItemSalesEvent(AtlasItemSalesEvent event) {
		super.handleAtlasItemSalesEvent(event);
		AtlasSecurityLogger.getInstance().debug("ItemSale a");
		ItemSalesEvent ise=event.getItemSalesEvent();
		if (event.getEventType()=="VERIFY") { ///make new event ????
			handlePriceVerify(event);
			return;
		}
		initializeJSONMessage("scan-info");
		addToEventJSON("barcode",ise.getItemID());
		int price=makeMoneyInt(ise.getExtendedPrice());
		addToEventJSON("extPrice",price);
		int qty=1;
		try {
			qty=ise.getQuantity();
		} catch (AEFException e) {
			//
		}
		addToEventJSON("qty",qty); 
		
		AtlasSecurityLogger.getInstance().debug("ItemSale b");
		int unitPrice=makeMoneyInt(ise.getUnitPrice());
		addToEventJSON("unitPrice",unitPrice);
		addToEventJSON("dept",Integer.parseInt(ise.getDepartmentNumber()));
		addToEventJSON("description",ise.getDescription());
		addToEventJSON("isVoid",(event.getEventType().equalsIgnoreCase("REMOVE")) ? true : false);
		String entryMethod=getInputType(ise.getItemIDQualifier());
		addToEventJSON("entryMethod",entryMethod);
		//isMgrOverride - TODO
		AtlasSecurityLogger.getInstance().debug("ItemSale c");
		int weight=0;
		try {		
			weight=new BigDecimal(ise.getWeight()).movePointRight(3).intValueExact();
		} catch (Throwable e) {
			AtlasSecurityLogger.getInstance().error("Weight error:"+e);
		}
		addToEventJSON("weight",weight);
		addToEventJSON("itemType",""); //TODO
		AtlasSecurityLogger.getInstance().debug("ItemSale c-1");
		sendTheMessage();
		AtlasSecurityLogger.getInstance().debug("ItemSale e");
	}
	
	@Override
	public void handlePriceVerify(AtlasItemSalesEvent event) {
		super.handlePriceVerify(event);
		initializeJSONMessage("priceCheck");
		ItemSalesEvent ise=event.getItemSalesEvent();
		addToEventJSON("barcode",ise.getItemID());
		int price=makeMoneyInt(ise.getExtendedPrice());
		addToEventJSON("extPrice",price);
		int unitPrice=makeMoneyInt(ise.getUnitPrice());
		addToEventJSON("unitPrice",unitPrice);
		addToEventJSON("dept",Integer.parseInt(ise.getDepartmentNumber()));
		addToEventJSON("description",ise.getDescription());
		String entryMethod=getInputType(ise.getItemIDQualifier());
		addToEventJSON("entryMethod",entryMethod);
		//TODO- isMgrOverride 
		int weight=0;
		try {
			weight=new BigDecimal(ise.getWeight()).movePointRight(3).intValueExact();
		} catch (Throwable e) {
			AtlasSecurityLogger.getInstance().error("Weight error:"+e);
		}
		addToEventJSON("weight",weight); 
		addToEventJSON("itemType",ise.getItemType()); //TODO
		sendTheMessage();
		
	}
	
	@Override
	public void transactionStart(TransactionStatusEvent event) {
		super.transactionStart(event);
		
		this.currentTransId=event.getID();
		this.currentOperator=event.getOperatorID();
		initializeJSONMessage("tx-start");
		//these are  in the header
		sendTheMessage();
	}
	
	@Override
	public void transactionEnd(TransactionStatusEvent event) {
		super.transactionEnd(event);
		initializeJSONMessage("transEnd");
		addToEventJSON("endType","COMPLETE");
		sendTheMessage();
		this.currentTransId=""; //clear trans number
	}
	
	@Override
	public void transactionVoid(TransactionStatusEvent event) {
		super.transactionVoid(event);
		initializeJSONMessage("transVoid");
		addToEventJSON("endType","VOID");
		sendTheMessage();
		//TODO- see if transEnd is called, if not clear the trans number
	}
	
	@Override
	public void transactionNoSale(TransactionStatusEvent event) {
		super.transactionNoSale(event);
		initializeJSONMessage("noSale");
		
		sendTheMessage();
	}
	
	@Override
	public void transactionSuspended(TransactionStatusEvent event) {
		super.transactionSuspended(event);
		initializeJSONMessage("transSuspend");
		addToEventJSON("endType","SUSPEND");
		sendTheMessage();
		//TODO- see if transEnd is called, if not clear the trans number
	}
	
	@Override 
	public void handleTenderEvent(TenderEvent event) {
		super.handleTenderEvent(event);
		initializeJSONMessage("tender");
		int tenderID=99;
		try {
			tenderID=Integer.parseInt(event.getTenderType()+event.getTenderVariety());
		} catch (Exception e) {
			//don't really care right now.
		}
		addToEventJSON("tenderId",tenderID);
		addToEventJSON("tenderDesc",event.getDescription()); //TODO - check
		addToEventJSON("amount",makeMoneyInt(event.getAmount()));
		sendTheMessage();
	}
	
	@Override 
	public void handleCouponEvent(CouponEvent event) {
		super.handleCouponEvent(event);
		initializeJSONMessage("coupon");
		addToEventJSON("upc",event.getItemID());
		int price=makeMoneyInt(event.getValue());
		addToEventJSON("amount",price);
		addToEventJSON("dept",Integer.parseInt(event.getDepartmentNumber()));
		addToEventJSON("type",event.getCouponType()); //TODO- "Store", "Manufacturer"
		String entryMethod=getInputType(event.getItemIDQualifier());
		sendTheMessage();
	}
	
	@Override
	public void signOn(OperatorEvent event) {
		super.signOn(event);
		this.currentOperator=event.getID();
		initializeJSONMessage("signOn");
		try {
			addToEventJSON("operatorId",event.getID());
		} catch (Exception e) {
			//ntohing just couldnot get operator id
		}
		//TODO - name?
		sendTheMessage();
	}
	
	@Override
	public void signOff(OperatorEvent event) {
		super.signOff(event);
		initializeJSONMessage("signOff");
		sendTheMessage();
		this.currentOperator="";
	}
	
	@Override
	public void termLock(OperatorEvent event) {
		super.termLock(event);
		initializeJSONMessage("termLocked");
		//just in case
		this.currentOperator=event.getID(); //TODO - CHECK
		sendTheMessage();
	}
	
	@Override 
	public void termUnlock(OperatorEvent event) {
		super.termUnlock(event);
		initializeJSONMessage("termUnlocked");
		//just in case
		this.currentOperator=event.getID(); //TODO - CHECK
		sendTheMessage();
	}
	
	public void handleTransactionTotals(TransactionTotalsEvent event) {
		super.handleTransactionTotals(event);
		initializeJSONMessage("transTotal");
		addToEventJSON("taxTotal",makeMoneyInt(event.getTax()));
		addToEventJSON("transTotal",makeMoneyInt(event.getTotal()));
		addToEventJSON("amtDue",makeMoneyInt(event.getAmountDue()));
		sendTheMessage();
	}
	@Override
	public void handleDiscountEvent(DiscountEvent event) {
		super.handleDiscountEvent(event);
		initializeJSONMessage("discount");
		//addToEventJSON("discountId",event.get) //TODO - ID?
		String discType="item";
		boolean isVoid=false;
		boolean isTaxExempt=false;
		boolean isRefund=false;
		 try {
				if (event.isTransactionDiscount()) {
					 discType="trans";
				 }
				if (event.isRefunded()) {
					isRefund=true;
				}
				if (event.isVoided()) {
					isVoid=true;
				}
				if (event.reducesTaxBalanceDue()) {
					isTaxExempt=true;
				}
			 } catch (AEFException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
		addToEventJSON("discountType",discType);
		addToEventJSON("isVoid",isVoid);
		addToEventJSON("isRefund",isRefund);
		addToEventJSON("isTaxExempt",isTaxExempt);
		addToEventJSON("appliesTo",event.getAppliesTo());
		addToEventJSON("discountReason",event.getDiscountReason());
		addToEventJSON("description",event.getDescription());
		addToEventJSON("rate",event.getDiscountRate());
		addToEventJSON("amount",event.getAmount());
		
		sendTheMessage();
	}
	public void scannerData(String theData) {
		super.scannerData(theData);
		initializeJSONMessage("scanner");
		addToEventJSON("scannerData",theData);
		sendTheMessage();
		
	}
	
	//TODO- ageentry
	
	@Override
	public void sendHeartbeat() {
		super.sendHeartbeat();
		initializeJSONMessage("heartbeat");
		sendTheMessage();
	}
	
	protected String getInputType(String mode) {
		AtlasSecurityLogger.getInstance().debug("getInputType:"+mode);
		String result=mode;
		switch (mode) {
		case "ScannedItemCode":
			result="scanned";
			break;
		case "KeyedItemCode":
			result="keyed";
			break;
		case "ItemLookupKeyed":
			result="lookup";
			break;
		case "LinkedItemCode":
			result="linked";
			break;
		}
		return result;
	}
}
