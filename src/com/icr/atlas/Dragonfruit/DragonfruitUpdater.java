package com.icr.atlas.Dragonfruit;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.ibm.retail.AEF.event.CouponEvent;
import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.DiscountEvent;
import com.ibm.retail.AEF.event.ItemSalesEvent;
import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.retail.AEF.event.TenderEvent;
import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.ibm.retail.AEF.event.TransactionTotalsEvent;
import com.ibm.retail.AEF.event.WorkstationStatusEvent;
import com.ibm.retail.si.util.AEFException;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.AtlasUtils;
import com.icr.atlas.Dragonfruit.DragonfruitLogger.Level;
import com.icr.atlas.eventPublishers.AtlasAEFPropertyChangeEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCouponEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCustomerEventPublisher;
import com.icr.atlas.eventPublishers.AtlasDiscountEventPublisher;
import com.icr.atlas.eventPublishers.AtlasItemSalesEvent;
import com.icr.atlas.eventPublishers.AtlasItemSalesEventPublisher;
import com.icr.atlas.eventPublishers.AtlasOperatorEventPublisher;
import com.icr.atlas.eventPublishers.AtlasPOSMessageEventPublisher;
import com.icr.atlas.eventPublishers.AtlasSubscriber;
import com.icr.atlas.eventPublishers.AtlasTenderEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionStatusEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionTotalsEventPublisher;
import com.icr.atlas.eventPublishers.AtlasWorkstationStatusEventPublisher;

public class DragonfruitUpdater implements AtlasSubscriber<Object>{
	private String dragonfruitURL="";
	private String[] tenderMap;
	private DragonfruitHTTPClient dragonfruitHTTPClient;
	//used for comms
	private  DateTimeFormatter theDateTimeFormatter;
	private int dragonfruitSeqCounter=0;
	private int thisTerminalNumber=0;
	private final String posMake="TOSHIBA";
	private final String posModel="ICR_ATLAS";
	private final String posVer="1.0.0";
	private final String protoVer="1.3.0.0";//dragonfruit version
	private JSONObject messageObject;
	private String currentTransId="";
	private long lastMessage=0;
	private int heartbeatSeconds=60;
	private int heartbeatInitDelay=300;
	private ScheduledExecutorService commHeartbeat;
	private Level loggingLevel=Level.INFO;
	
	private int httpConnectTimeout=1000;
	private int httpReadTimeout=1000;
	
	
		
	public DragonfruitUpdater() {
		this.thisTerminalNumber=Integer.parseInt(ATLASGuiExtend.getInstance().getTermId());
		DragonfruitLogger log=DragonfruitLogger.init(String.format("%03d",this.thisTerminalNumber));
		
		//logging level set by readProps
		
		// TODO Auto-generated constructor stub
		//this.dragonfruitURL="https://c51c614b5633ce9967d9419409a637c4.m.pipedream.net";
		this.dragonfruitURL="http://dragonfruit:8080";
		this.tenderMap=new String[100];
		readProps();
		readProps(this.thisTerminalNumber);
		DragonfruitLogger.getInstance().setLevel(this.loggingLevel); //need to do after load b/c base might be NONE 
		
		this.dragonfruitHTTPClient=new DragonfruitHTTPClient(false);
		this.dragonfruitHTTPClient.setURL(this.dragonfruitURL);
		this.dragonfruitHTTPClient.connectTimeout=this.httpConnectTimeout;
		this.dragonfruitHTTPClient.readTimeout=this.httpReadTimeout;
		this.theDateTimeFormatter=DateTimeFormatter.ofPattern("MM/dd/yy-HH:mm:ss.SSS");
		this.dragonfruitSeqCounter=0;
//		this.thisTerminalNumber=Integer.parseInt(ATLASGuiExtend.getInstance().getTermId());
		
		//subscribe to ATLAS
		AtlasCustomerEventPublisher.getInstance().subscribe(this);
		AtlasItemSalesEventPublisher.getInstance().subscribe(this);
		AtlasTransactionStatusEventPublisher.getInstance().subscribe(this);
		AtlasTenderEventPublisher.getInstance().subscribe(this);
		AtlasWorkstationStatusEventPublisher.getInstance().subscribe(this);
		AtlasCouponEventPublisher.getInstance().subscribe(this);
		AtlasOperatorEventPublisher.getInstance().subscribe(this);
		AtlasTransactionTotalsEventPublisher.getInstance().subscribe(this);
		AtlasDiscountEventPublisher.getInstance().subscribe(this);
		AtlasPOSMessageEventPublisher.getInstance().subscribe(this);

		DragonfruitLogger.getInstance().info("url:"+this.dragonfruitURL);
		DragonfruitLogger.getInstance().info("httpConnectTimeout:"+this.httpConnectTimeout);
		DragonfruitLogger.getInstance().info("httpReadTimeout:"+this.httpReadTimeout);
		DragonfruitLogger.getInstance().info("heartbeatSeconds:"+this.heartbeatSeconds);
		DragonfruitLogger.getInstance().info("heartbeatInitDelay:"+this.heartbeatInitDelay);
		
		
		
		this.commHeartbeat=Executors.newSingleThreadScheduledExecutor();
		this.commHeartbeat.scheduleAtFixedRate(this::checkHeartbeat,this.heartbeatInitDelay,this.heartbeatSeconds,TimeUnit.SECONDS); //delay 5 min b4 starting
		DragonfruitLogger.getInstance().info("DragonfruitUpdater started");
		
	}
	private void readProps() {
		readProps(0);
	}
	
	private void readProps(int termId) {
		Properties theProps=new Properties();
		String theFilename="dragonfruit.properties";
		if (termId!=0) {
			theFilename ="dragonfruit_"+String.format("%03d", termId)+".properties";
		}
		System.out.println("DragonfruitUpdater ***readProps:"+theFilename);
		
		try {
			//theProps.load(DragonfruitUpdater.class.getClassLoader().getResourceAsStream("dragonfruit.properties"));
			theProps.load(DragonfruitUpdater.class.getClassLoader().getResourceAsStream(theFilename));
		} catch (Exception e) { //was IOException
			// TODO Auto-generated catch block
			System.out.println("DragonfruitUpdater   props error:"+e.getMessage());
			//e.printStackTrace();
			return;
		}
		System.out.println("DragonfruitUpdater   props:"+theProps);
		this.dragonfruitURL=theProps.getProperty("url", this.dragonfruitURL);
		try {
			this.httpConnectTimeout=Integer.parseInt(theProps.getProperty("httpConnectTimeout",String.valueOf(this.httpConnectTimeout)));
			this.httpReadTimeout=Integer.parseInt(theProps.getProperty("httpReadTimeout",String.valueOf(this.httpReadTimeout)));
			this.heartbeatSeconds=Integer.parseInt(theProps.getProperty("heartbeatSeconds",String.valueOf(this.heartbeatSeconds)));
			this.heartbeatInitDelay=Integer.parseInt(theProps.getProperty("heartbeatInitDelay",String.valueOf(this.heartbeatInitDelay)));
		} catch (Exception e) {
			System.out.println("DragonfruitUpdater props A error"+e.getMessage());
			//e.printStackTrace();
		}
		String logLevel=theProps.getProperty("loggingLevel",this.loggingLevel.name()).toUpperCase();
		DragonfruitLogger.Level theLevel;
		switch (logLevel) {
			case "ERROR":
				theLevel=DragonfruitLogger.Level.ERROR;
				break;
			case "WARN":
				theLevel=DragonfruitLogger.Level.WARN;
				break;
			case "DEBUG":
				theLevel=DragonfruitLogger.Level.DEBUG;
				break;
			case "INFO":
				theLevel=DragonfruitLogger.Level.INFO;
				break;
			default:
				theLevel=DragonfruitLogger.Level.NONE;
				break;
		}
		//DragonfruitLogger.getInstance().setLevel(theLevel);
		System.out.println("DragonfruitUpdater loggingLevel:"+theLevel.name());
		this.loggingLevel=theLevel;
		//tendermap  -- only store level options
		if (termId==0) {
			for(int t=11;t<20;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"cash");
			}
			for(int t=21;t<30;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"check");
			}
			for(int t=31;t<40;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"food_stamps");
			}
			for(int t=41;t<50;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"credit");
			}
			for(int t=51;t<60;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"debit");
			}
			for(int t=61;t<70;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"other");
			}
			for(int t=71;t<90;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"redemption");
			}
			for(int t=91;t<100;t++) {
				this.tenderMap[t]=theProps.getProperty("Tender"+t,"other");
			}
		}
	
	}
	
	private void initializeJSONMessage(String eventType) {
		this.dragonfruitSeqCounter+=1;
		//eventType is optional
		this.messageObject=new JSONObject();
		this.messageObject.put("ts",LocalDateTime.now().format(theDateTimeFormatter) );
		if (eventType!="") {
			this.messageObject.put("eventType", eventType);
		}
		this.messageObject.put("reg", this.thisTerminalNumber);
		this.messageObject.put("seq", this.dragonfruitSeqCounter);
		this.messageObject.put("seqType", "reg");
		//delay is optional field
	}
	
	private void addToJSONMessage(String key,Object value) {
		if (this.messageObject==null) {
			initializeJSONMessage("");
		}
		this.messageObject.put(key, value);
	}
	
	private int makeMoneyInt(String value) {
		return new BigDecimal(value).movePointRight(2).intValueExact();
	}
	
	private void sendTheMessage() {
		DragonfruitLogger.getInstance().debug("sendTheMessage called");
		//idiot check (sendTheMessage) will clear the messageObject to prevent double send 
		if (this.messageObject==null) {
			DragonfruitLogger.getInstance().error("sendMessage called with no message");
			return; //??????
		}
		String response="";
		try {
			this.dragonfruitHTTPClient.postJSON(messageObject.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			DragonfruitLogger.getInstance().error(e.getMessage());
		} finally {
			//System.out.println("response:"+response);
			this.messageObject=null; //clear it
		}
		this.lastMessage=System.currentTimeMillis();
	}

	@Override
	public void update(Object o) {
		if (o instanceof CustomerEvent) {
			handleCustomerEvent((CustomerEvent)o);
		}
		if (o instanceof AtlasItemSalesEvent) {
			handleAtlasItemSalesEvent((AtlasItemSalesEvent)o);
		}
		if (o instanceof TransactionStatusEvent) {
			handleTransactionStatusEvent((TransactionStatusEvent)o);
		}
		if (o instanceof TenderEvent) {
			handleTenderEvent((TenderEvent)o);
		}
		if (o instanceof WorkstationStatusEvent) {
			handleWorkstationStatusEvent((WorkstationStatusEvent)o);
		}
		if (o instanceof CouponEvent) {
			handleCouponEvent((CouponEvent)o);
		}
		if (o instanceof OperatorEvent) {
			handleOperatorEvent((OperatorEvent)o);
		}
		if (o instanceof TransactionTotalsEvent) {
			handleTransactionTotals((TransactionTotalsEvent)o);
		}
		if (o instanceof DiscountEvent) {
			handleDiscountEvent((DiscountEvent)o);
		}
		
		
	}
	private void handleCustomerEvent(CustomerEvent event) {
		DragonfruitLogger.getInstance().debug("CustomerEvent SEEN");
		initializeJSONMessage("ClubCard");
		addToJSONMessage("cardNumber",event.getID());
		sendTheMessage();
	}
	
	private void handleAtlasItemSalesEvent(AtlasItemSalesEvent event) {
		DragonfruitLogger.getInstance().debug("AtlasItemSalesEvent SEEN");
		ItemSalesEvent ise=event.getItemSalesEvent();
		if (event.getEventType()=="VERIFY") { //11-22-25
			handlePriceVerify(event);
			return;
		}
		initializeJSONMessage("Item");
		//some of these need to be "massaged"
		addToJSONMessage("dept",Integer.parseInt(ise.getDepartmentNumber()));
		addToJSONMessage("desc",ise.getDescription());
		addToJSONMessage("isVoid",(event.getEventType().equalsIgnoreCase("REMOVE")) ? true : false);
		
		addToJSONMessage("itemType","sale");
		int price=makeMoneyInt(ise.getExtendedPrice());
		addToJSONMessage("price",price);
		int qty=1;
		try {
			qty=ise.getQuantity();
		} catch (AEFException e) {
			//
		}
		addToJSONMessage("qty",qty); //?
		addToJSONMessage("sku",ise.getItemID());
		int unitPrice=makeMoneyInt(ise.getUnitPrice());
		addToJSONMessage("unitPrice",unitPrice);
		int weight=new BigDecimal(ise.getWeight()).movePointRight(3).intValueExact();
		addToJSONMessage("weight",weight);
		int requiredAge=-1;
		try {
			requiredAge=ise.getIntegerPropertyValue("requiredAge");
		} catch (AEFException e1) {
			requiredAge=-1;
		}
		addToJSONMessage("isAgeRestricted",(requiredAge!=-1) ? true : false);
		addToJSONMessage("minAge",requiredAge);
		Boolean isHandEntry=false;
		Boolean isDeptKey=false;
		switch (ise.getItemIDQualifier()) {
		case "ScannedItemCode":
			isHandEntry=false;
			isDeptKey=false;
			break;
		case "KeyedItemCode":
			isHandEntry=true;
			isDeptKey=false;
			break;
		case "ItemLookupKeyed": 
			isHandEntry=false;
			isDeptKey=true;
			break;
		default:
			isHandEntry=false;
			isDeptKey=false;
			break;
		}
		boolean isDeposit=false;
		try {
			isDeposit=ise.isDeposit();
		} catch (AEFException e) {
			// TODO Auto-generated catch block
		}
		addToJSONMessage("isBottleDeposit",isDeposit);
		addToJSONMessage("isHandEntry",isHandEntry);
		addToJSONMessage("isDeptKey",isDeptKey);
		
		sendTheMessage();
	}
	public void handlePriceVerify(AtlasItemSalesEvent event) {
		DragonfruitLogger.getInstance().debug("PRICE VERIFY SEEN");
		initializeJSONMessage("PriceCheck");
		ItemSalesEvent ise=event.getItemSalesEvent();
		addToJSONMessage("dept",Integer.parseInt(ise.getDepartmentNumber()));
		addToJSONMessage("desc",ise.getDescription());
		addToJSONMessage("isHandEntry",(ise.getItemIDQualifier()=="KeyedItemCode") ? true : false);
		addToJSONMessage("price",makeMoneyInt(ise.getExtendedPrice()));
		int qty=1;
		try {
			qty=ise.getQuantity();
		} catch (AEFException e) {
			//
		}
		addToJSONMessage("qty",qty); //?
		addToJSONMessage("sku",ise.getItemID());
		int unitPrice=makeMoneyInt(ise.getUnitPrice());
		addToJSONMessage("unitPrice",unitPrice);
		int weight=new BigDecimal(ise.getWeight()).movePointRight(3).intValueExact();
		addToJSONMessage("weight",weight);
		sendTheMessage();
	}
	
	public void transactionStart(TransactionStatusEvent event) {
		DragonfruitLogger.getInstance().debug("TRANSACTION START SEEN");
		initializeJSONMessage("TransStart");
		addToJSONMessage("posMake", this.posMake);
		addToJSONMessage("posModel", this.posModel);
		addToJSONMessage("posVer", this.posVer);
		addToJSONMessage("protoVer",this.protoVer);
		sendTheMessage();
		//we have a 3-fer now send the transID
		initializeJSONMessage("TransID");
		addToJSONMessage("transID",Integer.parseInt( event.getID()));
		this.currentTransId=event.getID();
		sendTheMessage();
		initializeJSONMessage("CashierID");
		addToJSONMessage("cashierID",event.getOperatorID());
		sendTheMessage();
	}
	
	public void transactionEnd(TransactionStatusEvent event) {
		DragonfruitLogger.getInstance().debug("TRANSACTION END SEEN");
		initializeJSONMessage("TransEnd");
		//no other values
		sendTheMessage();
		this.currentTransId="";
	}
	public void totalProcessingEnd(TransactionStatusEvent event) {
		
	}
	public void transactionVoid(TransactionStatusEvent event) {
		DragonfruitLogger.getInstance().debug("TRANS VOID SEEN");
		initializeJSONMessage("TransCancel");
		//nothing to add
		sendTheMessage();
	}
	public void transactionNoSale(TransactionStatusEvent event) {
		DragonfruitLogger.getInstance().debug("NO SALE SEEN");
		initializeJSONMessage("NoSale");
		//nothing to add
		sendTheMessage();
	}
	public void transactionSuspended(TransactionStatusEvent event) {
		DragonfruitLogger.getInstance().debug("Suspend SEEN");
		initializeJSONMessage("TransSuspend");
		sendTheMessage();
	}
	public void retrieveProcessingStart(TransactionStatusEvent event) {
		DragonfruitLogger.getInstance().debug("Retrieve START SEEN");
//		initializeJSONMessage("TransRetrieveStart");
//		sendTheMessage();
	}
	
	public void retrieveProcessingEnd(TransactionStatusEvent event) {
		DragonfruitLogger.getInstance().debug("Retrieve END SEEN");
//		initializeJSONMessage("TransRetrieveEnd");
//		sendTheMessage();
	}
	
	private void handleTransactionStatusEvent(TransactionStatusEvent event) {
		
	}
	private void handleTenderEvent(TenderEvent event) {
		DragonfruitLogger.getInstance().debug("TENDER EVENT SEEN");
		initializeJSONMessage("Tender");
		addToJSONMessage("amount",makeMoneyInt(event.getAmount()));
		addToJSONMessage("desc",event.getDescription());
		addToJSONMessage("isCurrencyTender",true);
		addToJSONMessage("qty",1);
		int tenderID=99;
		try {
			tenderID=Integer.parseInt(event.getTenderType()+event.getTenderVariety());
		} catch (Exception e) {
			//don't really care right now.
		}
		if (tenderID<100) {
			addToJSONMessage("tenderType",tenderMap[tenderID]);
		} else {
			addToJSONMessage("tenderType","other");
		}
		//addToJSONMessage("tenderType",event.getTenderType()+event.getTenderVariety()); //just a place holder for now
		addToJSONMessage("account", null);
		sendTheMessage();
	}
	private void handleWorkstationStatusEvent(WorkstationStatusEvent event) {
		
	}
	
	private void handleCouponEvent(CouponEvent event) {
		DragonfruitLogger.getInstance().debug("COUPON EVENT SEEN");
		initializeJSONMessage("Coupon");
		addToJSONMessage("amount",makeMoneyInt(event.getValue()));
		if (event.getDepartmentNumber()!="") {
			addToJSONMessage("dept",Integer.parseInt(event.getDepartmentNumber()));
		}
		addToJSONMessage("desc",event.getDescription());
		addToJSONMessage("qty",1);
		addToJSONMessage("sku",event.getItemID());
		String entryType="scan";
		if (event.getItemIDQualifier()=="KeyedItemCode") {
			entryType="hand_entry";
		}
		addToJSONMessage("entryType",entryType);
		addToJSONMessage("type",event.getCouponType());
		sendTheMessage();
	}
	 public void signOn(OperatorEvent event) {
		 DragonfruitLogger.getInstance().debug("signOn called");
		 initializeJSONMessage("CashierSignIn");
		 addToJSONMessage("cashierID",event.getID());
		 addToJSONMessage("employeeType","cashier");
		 //?? -- event.getOperatorAuthorization()
		 sendTheMessage();
	 }
	 
	 public void signOff(OperatorEvent event) {
		 DragonfruitLogger.getInstance().debug("signOff called");
		 initializeJSONMessage("CashierSignOut");
		 addToJSONMessage("cashierID",event.getID());
		 sendTheMessage();
	}
	 
	 public void termLock(OperatorEvent event) {
		 DragonfruitLogger.getInstance().debug("termLock called");
		 initializeJSONMessage("Custom");
		 addToJSONMessage("subType","TermLocked");
		 int transID=0;
		 try {
			 transID=Integer.parseInt(this.currentTransId);
		 } catch (Exception e) {
		 }
		 addToJSONMessage("transID",transID);
		 sendTheMessage();
	 }
	 
	 public void termUnlock(OperatorEvent event) {
		 DragonfruitLogger.getInstance().debug("termUnlock called");
		 initializeJSONMessage("Custom");
		 addToJSONMessage("subType","TermUnLocked");
		 int transID=0;
		 try {
			 transID=Integer.parseInt(this.currentTransId);
		 } catch (Exception e) {
		 }
		 addToJSONMessage("transID",transID);

		 sendTheMessage();

	 }
	 
	 private void handleOperatorEvent(OperatorEvent event) {
		 
	 }
	 private void handleTransactionTotals(TransactionTotalsEvent event) {
		 //send it every time ACE sends it
		 DragonfruitLogger.getInstance().debug("TOTALS SEEN");
		 initializeJSONMessage("TransTotal");
		 
		 addToJSONMessage("tax",makeMoneyInt(event.getTax()));
		 addToJSONMessage("total",makeMoneyInt(event.getTotal()));
		 sendTheMessage();
	 }

	 private void handleDiscountEvent(DiscountEvent event) {
		 //using custom instead of the API
		 DragonfruitLogger.getInstance().debug("DISCOUNT SEEN");
		 initializeJSONMessage("Custom");
		 String subtype="ItemDiscount";
		 Boolean isVoid=false;
		 Boolean isTaxExempt=false;
		 try {
			if (event.isTransactionDiscount()) {
				 subtype="TransDiscount";
			 }
			if (event.isRefunded()) {
				isVoid=true;
			}
			if (event.reducesTaxBalanceDue()) {
				isTaxExempt=true;
			}
		 } catch (AEFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
		 addToJSONMessage("subType",subtype);
		 addToJSONMessage("isVoid",isVoid);
		 addToJSONMessage("appliesTo",event.getAppliesTo());
		 addToJSONMessage("discountReason",event.getDiscountReason());
		 addToJSONMessage("descr",event.getDescription());
		 addToJSONMessage("method",event.getDiscountMethod());
		 addToJSONMessage("rate",event.getDiscountRate());
		 addToJSONMessage("taxExempt",isTaxExempt);
		 addToJSONMessage("amount",event.getAmount());
		 addToJSONMessage("transID",Integer.parseInt(this.currentTransId));
		 sendTheMessage();
	 }
	 
	 public void ageEntry(String ageXML) {
		 DragonfruitLogger.getInstance().debug("ageEntry SEEN");
		 initializeJSONMessage("AgeVerification");
		 int years=0;
		 try  {
			 years=Integer.parseInt(AtlasUtils.getXML(ageXML, "ageInYears"));
		 } catch (Exception e) {
			 e.printStackTrace();
		 }
		 if (years==0) {
			 return; //no age?
		 }
		 String fuzzyString=AtlasUtils.getXML(ageXML, "isAgeFuzzy");
		 boolean isFuzzy=false;
		 if (fuzzyString.equalsIgnoreCase("true")) {
			 isFuzzy=true;
		 }
		 
		 addToJSONMessage("age",years);
		 addToJSONMessage("entered",!isFuzzy); //placeholder
		 
		 addToJSONMessage("approverID",""); //no info
		 sendTheMessage();
		 
	 }
	 
	 
	 private void sendHeartbeat() {
		 DragonfruitLogger.getInstance().debug("NoOp");
		 initializeJSONMessage("NoOp");
		 sendTheMessage();
	 }
	 
	 private void checkHeartbeat() {
		 DragonfruitLogger.getInstance().debug("Heartbeat check");
//		 if (this.lastMessage==0) {
//			 return;
//		 }
		 long now=System.currentTimeMillis();
		 if (now-this.lastMessage > this.heartbeatSeconds*1000) {
			 sendHeartbeat();
		 }
		 
 	 }
	
}
