package com.icr.atlas.AtlasSecurity.Base;

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
import com.icr.atlas.AtlasSecurity.AtlasSecurityLogger;
import com.icr.atlas.AtlasSecurity.AtlasSecurityLogger.Level;
import com.icr.atlas.eventPublishers.AtlasAEFPropertyChangeEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCouponEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCustomerEventPublisher;
import com.icr.atlas.eventPublishers.AtlasDiscountEventPublisher;
import com.icr.atlas.eventPublishers.AtlasItemSalesEvent;
import com.icr.atlas.eventPublishers.AtlasItemSalesEventPublisher;
import com.icr.atlas.eventPublishers.AtlasOperatorEventPublisher;
import com.icr.atlas.eventPublishers.AtlasPOSMessageEventPublisher;
import com.icr.atlas.eventPublishers.AtlasScannerEventPublisher;
import com.icr.atlas.eventPublishers.AtlasSubscriber;
import com.icr.atlas.eventPublishers.AtlasTenderEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionStatusEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionTotalsEventPublisher;
import com.icr.atlas.eventPublishers.AtlasWorkstationStatusEventPublisher;

public abstract class AtlasSecurityUpdaterBase<T> implements AtlasSubscriber<T> {
	protected String AtlasSecurityURL="";
	protected String[] tenderMap;
	protected AtlasSecurityHTTPClient AtlasSecurityHTTPClient;
	protected AtlasSecurityClientBase AtlasSecurityClientBase;
	//used for comms
	protected  DateTimeFormatter theDateTimeFormatter;
	protected int AtlasSecuritySeqCounter=0;
	protected int thisTerminalNumber=0;
	protected final String posMake="TOSHIBA";
	protected final String posModel="ICR_ATLAS";
	protected final String posVer="1.0.0";
	protected final String protoVer="1.0.0.0";//AtlasSecurity version
	protected JSONObject messageObject;
	protected String currentTransId="";
	protected long lastMessage=0;
	protected int heartbeatSeconds=60;
	protected int heartbeatInitDelay=300;
	protected ScheduledExecutorService commHeartbeat;
	protected Level loggingLevel=Level.INFO;
	
	protected int httpConnectTimeout=1000;
	protected int httpReadTimeout=1000;
	protected boolean keepSocketOpen=false;
	protected boolean ignoreAck=true;
	
	protected int sequentialCommErrors=0;
	protected boolean blockCommunications=false;
	protected int commErrorLimit=0; //don't turn on unless asked
		
	public AtlasSecurityUpdaterBase() {
		this.thisTerminalNumber=Integer.parseInt(ATLASGuiExtend.getInstance().getTermId());
		AtlasSecurityLogger log=AtlasSecurityLogger.init(String.format("%03d",this.thisTerminalNumber));
		
		//logging level set by readProps
		
		this.AtlasSecurityURL="http://AtlasSecurity:8080";
		//implementers MUST call init() 
	}
	protected final void init() {
		readProps();
		readProps(this.thisTerminalNumber);
		AtlasSecurityLogger.getInstance().setLevel(this.loggingLevel); //need to do after load b/c base might be NONE 
		
	//moved to initAtlasSecurityClient	
		//this.AtlasSecurityClientBase=new AtlasSecurityHTTPClient(false);
		//this.AtlasSecurityClientBase=new AtlasSecuritySocketClient(false);
//		this.AtlasSecurityClientBase.setURL(this.AtlasSecurityURL);
//		this.AtlasSecurityClientBase.connectTimeout=this.httpConnectTimeout;
//		this.AtlasSecurityClientBase.readTimeout=this.httpReadTimeout;
		
		this.theDateTimeFormatter=DateTimeFormatter.ofPattern("MM/dd/yy-HH:mm:ss.SSS");
		this.AtlasSecuritySeqCounter=0;
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
		AtlasScannerEventPublisher.getInstance().subscribe(this);

		AtlasSecurityLogger.getInstance().info("url:"+this.AtlasSecurityURL);
		AtlasSecurityLogger.getInstance().info("httpConnectTimeout:"+this.httpConnectTimeout);
		AtlasSecurityLogger.getInstance().info("httpReadTimeout:"+this.httpReadTimeout);
		AtlasSecurityLogger.getInstance().info("heartbeatSeconds:"+this.heartbeatSeconds);
		AtlasSecurityLogger.getInstance().info("heartbeatInitDelay:"+this.heartbeatInitDelay);
		AtlasSecurityLogger.getInstance().info("keepSocketOpen:"+this.keepSocketOpen);
		AtlasSecurityLogger.getInstance().info("ignoreAck:"+this.ignoreAck);
		AtlasSecurityLogger.getInstance().info("commErrorLimit:"+this.commErrorLimit);
		if (this.commErrorLimit>0 && this.heartbeatSeconds==0) {
			AtlasSecurityLogger.getInstance().error("commErrorLimit is "+this.commErrorLimit+" but heartbeat is 0");
		}
		
//TODO- if Heartbeat 0- no heartbeat
//		socket keepAlive option
		
		this.commHeartbeat=Executors.newSingleThreadScheduledExecutor();
		this.commHeartbeat.scheduleAtFixedRate(this::checkHeartbeat,this.heartbeatInitDelay,this.heartbeatSeconds,TimeUnit.SECONDS); //delay 5 min b4 starting
		AtlasSecurityLogger.getInstance().info("AtlasSecurityUpdater started");
		
	}
	
	protected void initAtlasSecurityClient(AtlasSecurityClientBase sc) {
		
		this.AtlasSecurityClientBase=sc;
		if (sc!=null) {
			this.AtlasSecurityClientBase.setURL(this.AtlasSecurityURL);
			this.AtlasSecurityClientBase.connectTimeout=this.httpConnectTimeout;
			this.AtlasSecurityClientBase.readTimeout=this.httpReadTimeout;
			this.AtlasSecurityClientBase.setKeepSocketOpen(this.keepSocketOpen);
			this.AtlasSecurityClientBase.setIgnoreAck(this.ignoreAck);
			//AtlasSecurityLogger.getInstance().info("initAtlasSecurityClient
		} else {
			AtlasSecurityLogger.getInstance().error("AtlasSecurityClientBase is NULL???");
		}
		
	}
	
	protected void readProps() {
		readProps(0);
	}
	
	protected void readProps(int termId) {
		Properties theProps=new Properties();
		String theFilename="atlasSecurity.properties";
		if (termId!=0) {
			theFilename ="atlasSecurity_"+String.format("%03d", termId)+".properties";
		}
		System.out.println("AtlasSecurityUpdaterBase ***readProps:"+theFilename);
		
		try {
			//theProps.load(AtlasSecurityUpdaterBase.class.getClassLoader().getResourceAsStream("AtlasSecurity.properties"));
			theProps.load(AtlasSecurityUpdaterBase.class.getClassLoader().getResourceAsStream(theFilename));
		} catch (Exception e) { //was IOException
			// TODO Auto-generated catch block
			System.out.println("AtlasSecurityUpdaterBase   props error:"+e.getMessage());
			//e.printStackTrace();
			return;
		}
		System.out.println("AtlasSecurityUpdaterBase   props:"+theProps);
		this.AtlasSecurityURL=theProps.getProperty("url", this.AtlasSecurityURL);
		try {
			this.httpConnectTimeout=Integer.parseInt(theProps.getProperty("httpConnectTimeout",String.valueOf(this.httpConnectTimeout)));
			this.httpReadTimeout=Integer.parseInt(theProps.getProperty("httpReadTimeout",String.valueOf(this.httpReadTimeout)));
			this.heartbeatSeconds=Integer.parseInt(theProps.getProperty("heartbeatSeconds",String.valueOf(this.heartbeatSeconds)));
			this.heartbeatInitDelay=Integer.parseInt(theProps.getProperty("heartbeatInitDelay",String.valueOf(this.heartbeatInitDelay)));
			this.commErrorLimit=Integer.parseInt(theProps.getProperty("commErrorLimit",String.valueOf(this.commErrorLimit)));
		} catch (Exception e) {
			System.out.println("AtlasSecurityUpdaterBase props A error"+e.getMessage());
			//e.printStackTrace();
		}
		keepSocketOpen=Boolean.parseBoolean(theProps.getProperty("keepSocketOpen",String.valueOf(this.keepSocketOpen)));
		ignoreAck=Boolean.parseBoolean(theProps.getProperty("ignoreAck",String.valueOf(this.ignoreAck)));
		
		String logLevel=theProps.getProperty("loggingLevel",this.loggingLevel.name()).toUpperCase();
		AtlasSecurityLogger.Level theLevel;
		switch (logLevel) {
			case "ERROR":
				theLevel=AtlasSecurityLogger.Level.ERROR;
				break;
			case "WARN":
				theLevel=AtlasSecurityLogger.Level.WARN;
				break;
			case "DEBUG":
				theLevel=AtlasSecurityLogger.Level.DEBUG;
				break;
			case "INFO":
				theLevel=AtlasSecurityLogger.Level.INFO;
				break;
			default:
				theLevel=AtlasSecurityLogger.Level.NONE;
				break;
		}
		//AtlasSecurityLogger.getInstance().setLevel(theLevel);
		System.out.println("AtlasSecurityUpdaterBase loggingLevel:"+theLevel.name());
		this.loggingLevel=theLevel;
		readPropsExtend(theProps);
	}
	
	protected void readPropsExtend(Properties theProps) {
		//allow extends to read from the properties files
		
	}
	
	protected void initializeJSONMessage() {
		this.AtlasSecuritySeqCounter+=1;
		this.messageObject=new JSONObject();

	}
	
	protected void addToJSONMessage(String key,Object value) {
		if (this.messageObject==null) {
			initializeJSONMessage();
		}
		this.messageObject.put(key, value);
	}
	
	protected int makeMoneyInt(String value) {
		return new BigDecimal(value).movePointRight(2).intValueExact();
	}
	
	protected void sendTheMessage() {
		AtlasSecurityLogger.getInstance().debug("sendTheMessage called");
		if (this.AtlasSecurityClientBase==null) {
			AtlasSecurityLogger.getInstance().error("sendTheMessage AtlasSecurityClient is NULL!");
			return;
		}
		
		//idiot check (sendTheMessage) will clear the messageObject to prevent double send 
		if (this.messageObject==null) {
			AtlasSecurityLogger.getInstance().error("sendMessage called with no message");
			return; //??????
		}
		
		String response="";
		try {
			this.AtlasSecurityClientBase.sendData(messageObject.toString());
			if (this.blockCommunications) {
				AtlasSecurityLogger.getInstance().error("Successfull comm, clearing block");
			}
			this.sequentialCommErrors=0;
			this.blockCommunications=false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			AtlasSecurityLogger.getInstance().error(e.getMessage());
			this.sequentialCommErrors+=1;
			AtlasSecurityLogger.getInstance().error("Num comm errors:"+this.sequentialCommErrors);
			if (this.sequentialCommErrors>this.commErrorLimit && this.commErrorLimit>0) {
				AtlasSecurityLogger.getInstance().error("Num comm errors:"+sequentialCommErrors+" blockingComms");
				this.blockCommunications=true;
			}
			
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
	protected void handleCustomerEvent(CustomerEvent event) {
		AtlasSecurityLogger.getInstance().debug("CustomerEvent SEEN");
	}
	
	protected void handleAtlasItemSalesEvent(AtlasItemSalesEvent event) {
		AtlasSecurityLogger.getInstance().debug("AtlasItemSalesEvent SEEN");
	}
	public void handlePriceVerify(AtlasItemSalesEvent event) {
		AtlasSecurityLogger.getInstance().debug("PRICE VERIFY SEEN");
	}
	
	public void transactionStart(TransactionStatusEvent event) {
		AtlasSecurityLogger.getInstance().debug("TRANSACTION START SEEN");
	}
	
	public void transactionEnd(TransactionStatusEvent event) {
		AtlasSecurityLogger.getInstance().debug("TRANSACTION END SEEN");
	}
	public void totalProcessingEnd(TransactionStatusEvent event) {
		
	}
	public void transactionVoid(TransactionStatusEvent event) {
		AtlasSecurityLogger.getInstance().debug("TRANS VOID SEEN");
	}
	public void transactionNoSale(TransactionStatusEvent event) {
		AtlasSecurityLogger.getInstance().debug("NO SALE SEEN");
	}
	public void transactionSuspended(TransactionStatusEvent event) {
		AtlasSecurityLogger.getInstance().debug("Suspend SEEN");
	}
	public void retrieveProcessingStart(TransactionStatusEvent event) {
		AtlasSecurityLogger.getInstance().debug("Retrieve START SEEN");
	}
	
	public void retrieveProcessingEnd(TransactionStatusEvent event) {
		AtlasSecurityLogger.getInstance().debug("Retrieve END SEEN");
	}
	
	protected void handleTransactionStatusEvent(TransactionStatusEvent event) {
		
	}
	protected void handleTenderEvent(TenderEvent event) {
		AtlasSecurityLogger.getInstance().debug("TENDER EVENT SEEN");
	}
	protected void handleWorkstationStatusEvent(WorkstationStatusEvent event) {
		
	}
	
	protected void handleCouponEvent(CouponEvent event) {
		AtlasSecurityLogger.getInstance().debug("COUPON EVENT SEEN");
	}
	 public void signOn(OperatorEvent event) {
		 AtlasSecurityLogger.getInstance().debug("signOn called");
	 }
	 
	 public void signOff(OperatorEvent event) {
		 AtlasSecurityLogger.getInstance().debug("signOff called");
	}
	 
	 public void termLock(OperatorEvent event) {
		 AtlasSecurityLogger.getInstance().debug("termLock called");
	 }	 
	 public void termUnlock(OperatorEvent event) {
		 AtlasSecurityLogger.getInstance().debug("termUnlock called");
	 }
	 
	 protected void handleOperatorEvent(OperatorEvent event) {
		 
	 }
	 protected void handleTransactionTotals(TransactionTotalsEvent event) {
		 //send it every time ACE sends it
		 AtlasSecurityLogger.getInstance().debug("TOTALS SEEN");
	 }

	 protected void handleDiscountEvent(DiscountEvent event) {
		 //using custom instead of the API
		 AtlasSecurityLogger.getInstance().debug("DISCOUNT SEEN");
	 }
	 
	 public void ageEntry(String ageXML) {
		 AtlasSecurityLogger.getInstance().debug("ageEntry SEEN");
		 
	 }
	 public void scannerData(String theData) {
		 AtlasSecurityLogger.getInstance().debug("scannerData:"+theData);
	 }
	 
	 protected void sendHeartbeat() {
		 AtlasSecurityLogger.getInstance().debug("NoOp");
	 }
	 
	 protected void checkHeartbeat() {
		 AtlasSecurityLogger.getInstance().debug("Heartbeat check");

		 long now=System.currentTimeMillis();
		 if (now-this.lastMessage > this.heartbeatSeconds*1000) {
			 sendHeartbeat();
		 }
		 
 	 }
	
}
