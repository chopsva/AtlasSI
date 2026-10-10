package com.icr.atlas.EventLogger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.OS4690.File4690;
import com.ibm.OS4690.FileOutputStream4690;
import com.ibm.retail.AEF.event.CouponEvent;
import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.DiscountEvent;
import com.ibm.retail.AEF.event.ItemSalesEvent;
import com.ibm.retail.AEF.event.LineItemEvent;
import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.retail.AEF.event.TenderEvent;
import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.ibm.retail.AEF.event.TransactionTotalsEvent;
import com.ibm.retail.AEF.event.WorkstationStatusEvent;
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
import com.icr.atlas.test.SendDataOnPipe;

public class AtlasEventLogger implements AtlasSubscriber<Object> {
//not a singleton
	private File4690 outputFile;
	private FileOutputStream4690 outputStream;
	private DateTimeFormatter theDateTimeFormatter;
	private DateTimeFormatter theFileDateFormatter;
	private String termId="";
	private String currentTransId="";
	private String lastFilename="";
	
	public AtlasEventLogger() {
		theDateTimeFormatter=DateTimeFormatter.ofPattern("MM/dd/yy-HH:mm:ss.SSS");
		theFileDateFormatter=DateTimeFormatter.ofPattern("yyMMdd");
		System.out.println("ATLAS EVENT LOGGER INIT");
		// TODO Auto-generated constructor stub
		AtlasCustomerEventPublisher.getInstance().subscribe(this);
		AtlasItemSalesEventPublisher.getInstance().subscribe(this);
		AtlasTransactionStatusEventPublisher.getInstance().subscribe(this);
		AtlasTenderEventPublisher.getInstance().subscribe(this);
		AtlasWorkstationStatusEventPublisher.getInstance().subscribe(this);
		AtlasCouponEventPublisher.getInstance().subscribe(this);
		AtlasOperatorEventPublisher.getInstance().subscribe(this);
		AtlasTransactionTotalsEventPublisher.getInstance().subscribe(this);
		AtlasDiscountEventPublisher.getInstance().subscribe(this);
		///
		termId=ATLASGuiExtend.getInstance().getTermId();
		System.out.println("ATLAS EVENT LOGGER TERM:"+termId);
		List<String> l=new ArrayList<>();
		checkOrOpenFile();
		l.add("LOGINIT");
		writeEvent(l);
	}

	@Override
	public void update(Object o) {
		// TODO Auto-generated method stub
		checkOrOpenFile();
		
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

	private String getFilename() {
		String result="EVyymmdd.xxx";
		result="R::F:/EV/"+LocalDateTime.now().format(theFileDateFormatter)+"."+termId;
		return result;
	}
	
	private void flushFile() {
		try {
			outputStream.flush();
			//outputStream.getFD().sync(); //sync errors every time
		} catch (IOException e) {
			 
			e.printStackTrace();
		}
	}
	
	private void closeFile() {
		if (outputStream!=null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				 
				e.printStackTrace();
			}
		}
	}
	
	private void openTheFile(String fn) {
		System.out.println("openTheFile:"+fn);
		outputFile=new File4690(fn);
		try {
			outputStream=new FileOutputStream4690(outputFile,true);
		} catch (IOException e) {
			 
			e.printStackTrace();
		}
		if (!outputFile.exists()) {
			try {
				outputFile.createNewFile();
			} catch (IOException e) {
				 
				e.printStackTrace();
			}
		}
		lastFilename=fn;
	}
	
	private boolean writeToFile(String theEvent) {
		System.out.println("writeToFile:"+lastFilename+":"+theEvent);
		
		boolean result=true;
		theEvent=theEvent+"\r\n";
		byte[] theBytes=theEvent.getBytes();
		try {
			outputStream.write(theBytes);
		} catch (IOException e) {
			 
			result=false;
			e.printStackTrace();
		}
		return result;
	}
	
	private void checkOrOpenFile() {
		System.out.println("checkoropenFile "+lastFilename);
		//String filename="R::F:/EV/EVmmddyy.xxx"; //for now
		//String filename="R::F:/EV/"+LocalDateTime.now().format(theFileDateFormatter)+"."+termId;
		String filename=getFilename();
		if ((outputFile==null)||(!outputFile.canWrite())) {
			openTheFile(filename);
		} else {
			System.out.println("file is open and writable");
			if (!lastFilename.equals(filename)) {
				System.out.println("closing file");
				closeFile();
				openTheFile(filename);
			}
		}
		//assume all is ok for testing
	}
	
	private boolean writeEvent(List<String> eventFields) {
		System.out.println("writeEvent"+eventFields);
		boolean result=true;
		String theEvent=LocalDateTime.now().format(theDateTimeFormatter)+",\""+termId+"\"";
		for(String field : eventFields) {
			String thisField=(field!=null) ? field : "nul";
			theEvent+=",\""+thisField.trim()+"\"";
		}
//		for(int i=0;i<eventFields.length;i++) {
//			theEvent+=","+eventFields[i].trim();
//		}
		System.out.println("to write:"+theEvent);
		result=writeToFile(theEvent);
		return result;
	}
	
	
	private void handleCustomerEvent(CustomerEvent event) {
		List<String> fields=new ArrayList<>();
		fields.add("CUSTOMER");
		fields.add(event.getID());
		fields.add(event.getName());
		writeEvent(fields);
		SendDataOnPipe test=new SendDataOnPipe();
		test.tryIt();
	}
	private void handleAtlasItemSalesEvent(AtlasItemSalesEvent event) {
		System.out.println("TEST SUBSCRIBE GOT itemSale EVENT");
		System.out.println("   event type:"+event.getEventType());
		System.out.println("   event id:"+event.getItemSalesEvent().getItemID());
		System.out.println("   event unitprice:"+event.getItemSalesEvent().getUnitPrice());
		List<String> fields=new ArrayList<>();
		ItemSalesEvent ise=event.getItemSalesEvent();
		String thisEvent="ITEM_"+event.getEventType();
		if (event.getEventType().equalsIgnoreCase("ADD")) {
			thisEvent="ITEM";
		}
		if (event.getEventType().equalsIgnoreCase("REMOVE")) {
			thisEvent="VOIDITEM";
		}
		//deal with refund and deposit with thisEvent
		
		fields.add(thisEvent);
		fields.add(ise.getItemID());
		fields.add(convertInputMethod(ise.getItemIDQualifier()));
		fields.add(ise.getDescription());
		fields.add(ise.getExtendedPrice());
		try {
			fields.add(String.valueOf(ise.getQuantity()));
		} catch (AEFException e) {
			fields.add("");
			e.printStackTrace();
		}
		fields.add(ise.getUnitPrice());
		fields.add(ise.getDepartmentNumber());
		fields.add(ise.getWeightOrigin());
		fields.add(ise.getWeight());
		try {
			fields.add(String.valueOf(ise.getIntegerPropertyValue("requiredAge")));
		} catch (AEFException e1) {
			 
			fields.add("");
		}
		try {
			fields.add(String.valueOf(ise.isFoodstampEligible()));
		} catch (AEFException e1) {
			 
			fields.add("");
		}
		try {
			fields.add(String.valueOf(ise.isTaxable()));
		} catch (AEFException e1) {
			 
			fields.add("");
		}
		fields.add(ise.getItemType());
		
		fields.add(currentTransId);
		try {
			fields.add(String.valueOf(ise.getItemOrdinalNumber()));
		} catch (AEFException e) {
			fields.add("");
		}
		
		writeEvent(fields);
		
	}
	
	public void transactionStart(TransactionStatusEvent event) {
		System.out.println("transactionStart called!!!");
		System.out.println("   event type:"+event.getEventType());
		List<String> fields=new ArrayList<>();
		fields.add("STARTTRANS");
		fields.add(event.getID());
		fields.add(event.getOperatorID());
		fields.add(String.valueOf(event.getTerminalNumber()));
		fields.add(String.valueOf(event.isInTrainingMode()));
		writeEvent(fields);
		flushFile();
		currentTransId=event.getID();
	}
	 public void transactionEnd(TransactionStatusEvent event) {
		 System.out.println("transactionEnd called!!!");
		 List<String> fields=new ArrayList<>();
			fields.add("ENDTRANS");
			fields.add(event.getID());
			fields.add(event.getTotal());
			fields.add(event.getTax());
			writeEvent(fields);
			flushFile();
	 }
	 
	 public void totalProcessingEnd(TransactionStatusEvent event) {
		 System.out.println("totalProcessingEND");
		 TransactionTotalsEvent lastTotals=AtlasTransactionTotalsEventPublisher.getInstance().getLastTransactionTotalsEvent();
		 if (lastTotals==null) {
			 return;
		 }
		 List<String> fields=new ArrayList<>();
		 fields.add("TOTAL");
		 fields.add(lastTotals.getTotal());
		 fields.add(lastTotals.getTotalItems());
		 fields.add(lastTotals.getTax());
		 fields.add(lastTotals.getFoodStampTotal());
		 fields.add(lastTotals.getCouponTotal());
		 fields.add(currentTransId);
		 writeEvent(fields);
	 }
	
	 public void transactionVoid(TransactionStatusEvent event) {
		 List<String> fields=new ArrayList<>();
		 fields.add("VOIDTRANS");
		 fields.add(event.getID());
		 writeEvent(fields);
	 }
	 
	private void handleTransactionStatusEvent(TransactionStatusEvent event) {
		//System.out.println("TEST SUBSCRIBE transStats");
		//System.out.println("   event type:"+event.getEventType());
//		String thisEvent="";
//		if (event.getEventType().equalsIgnoreCase("transactionStart")) {
//			thisEvent="STARTTRANS";
//		}
//		if (event.getEventType().equalsIgnoreCase("transactionEnd")) {
//			thisEvent="ENDTRANS";
//		}
//		if (thisEvent=="") {
//			return;
//		}
//		List<String> fields=new ArrayList<>();
//		fields.add(thisEvent);
//		fields.add(event.getID());
//		fields.add(event.getOperatorID());
//		fields.add(String.valueOf(event.getTerminalNumber()));
//		fields.add(String.valueOf(event.isInTrainingMode()));
//		writeEvent(fields);
//		flushFile();
	}
	
	private void handleTenderEvent(TenderEvent event) {
		System.out.println("TEST SUBSCRIBE tenderEvent");
		System.out.println("   eventType:"+event.getEventType());
		System.out.println("   ID:"+event.getTenderType());
		System.out.println("  amount:"+event.getAmount());
		List<String> fields=new ArrayList<>();
		String thisEvent="TENDER";
		try {
			if (event.isVoided()) {
				thisEvent="VOIDTENDER";
			}
			if (event.isRefunded()) {
				thisEvent="REFUNDTENDER";
			}
		} catch (AEFException e) {
			 
			//e.printStackTrace();
		}
		fields.add(thisEvent);
		fields.add(event.getTenderType()+event.getTenderVariety());
		fields.add(event.getAmount());
		fields.add(event.getDescription());
		fields.add(event.getChange());
		fields.add(event.getFee());
		fields.add(currentTransId);
		writeEvent(fields);
	}
	
	private void handleWorkstationStatusEvent(WorkstationStatusEvent event) {
//		System.out.println("TEST SUBSCRIUBE worksnEvent");
//		System.out.println("    eventType:"+event.getEventType());
//		System.out.println("    trmstatus"+event.getTerminalStatus());
	}
	 private void handleCouponEvent(CouponEvent event) {
		 System.out.println("TEST SUBSCRIBE couponEvent");
		 List<String> fields=new ArrayList<>();
		 String thisEvent="CPN";
		 try {
			if (event.isVoided()) {
				 thisEvent="VOIDCPN";
			 }
		} catch (AEFException e1) {
			 
			e1.printStackTrace();
		}
		 
		 
		 fields.add(thisEvent); //for now
		 fields.add(event.getItemID());
		 fields.add(convertInputMethod(event.getItemIDQualifier()));
		 fields.add(event.getDescription());
		 fields.add(event.getValue());
		 try {
			fields.add(String.valueOf(event.getQuantity()));
		} catch (AEFException e) {
			fields.add("");
		}
		fields.add(event.getUnitPrice());
		fields.add(event.getDepartmentNumber());
		if (event.getWeight().equals("0.00")) {
			fields.add("");
		} else {
			fields.add(event.getWeightOrigin());
		}
		fields.add(event.getWeight());
		try {
			fields.add(String.valueOf(event.reducesFoodstampBalanceDue()));
		} catch (AEFException e) {
			fields.add("");
		}
		try {
			fields.add(String.valueOf(event.reducesTaxDue()));
		} catch (AEFException e) {
			 
			fields.add("");
		}
		fields.add(event.getCouponType());
		fields.add(currentTransId);
		try {
			fields.add(String.valueOf(event.getCouponOrdinalNumber()));
		} catch (AEFException e) {
			 
			fields.add("");
		}
		writeEvent(fields);
		 
	 }
	 
	 public void signOn(OperatorEvent event) {
		 System.out.println("signOn called");
	 }
	 
	 public void signOff(OperatorEvent event) {
		 System.out.println("signOff called");
	 }
	 
	 public void termLock(OperatorEvent event) {
		 System.out.println("termLock called");
	 }
	 
	 public void termUnlock(OperatorEvent event) {
		 System.out.println("termUnlock called");
	 }
	 
	 private void handleOperatorEvent(OperatorEvent event) {
		 System.out.println("TEST SUBSCRIBE operatorEvent");
		 System.out.println("   eventType:"+event.getEventType());
		 System.out.println("       "+event.getID()+"-"+event.getName());
		 String thisEvent="";
		 if (event.getEventType().equalsIgnoreCase("SIGNON")) {
			 thisEvent="SIGNON";
		 }
		 if (event.getEventType().equalsIgnoreCase("SIGNOFF")) {
			 thisEvent="SIGNOFF";
		 }
		 if (event.getEventType().equalsIgnoreCase("LOCK")) {
			 thisEvent="LOCKED";
		 }
		 if (event.getEventType().equalsIgnoreCase("UNLOCK")) {
			 thisEvent="UNLOCKED";
		 }
		 if (thisEvent=="") {
			 return;
		 }
		 List<String> fields=new ArrayList<>();
		 fields.add(thisEvent);
		 fields.add(event.getID());
		 fields.add(event.getName());
		 writeEvent(fields);
		 flushFile();
		 
		 
	 }
	 private void handleTransactionTotals(TransactionTotalsEvent event) {
		 System.out.println("TEST SUBSCRIBE transactionTotals");
		 System.out.println("    amtDue:"+event.getAmountDue());
//		 List<String> fields=new ArrayList<>();
//		 fields.add("TOTAL");
//		 fields.add(event.getAmountDue());
//		 fields.add(event.getFoodStampBalance());
//		 fields.add(event.getTotalItems());
//		 fields.add(event.getTotal());
//		 fields.add(event.getTax());
//		 writeEvent(fields);
	 }
	 private void handleDiscountEvent(DiscountEvent event) {
		 System.out.println("TEST SUBSCRIBE discountEvent");
//		 System.out.println("     eventType:"+event.getEventType());
//		 System.out.println("      discType:"+event.getDiscountType());
//		 System.out.println("     discAmt:"+event.getAmount());
//		 System.out.println(event);
		 String thisType="DISC";
		 try {
			if (event.isVoided()) {
				 thisType="VOIDDISC";
			 }
		} catch (AEFException e) {
			 
			//e.printStackTrace();
		}
		 List<String> fields=new ArrayList<>();
		 
		 fields.add(thisType);
		 fields.add(event.getDiscountReason());
		 try {
			if (event.isTransactionDiscount()) {
				 fields.add("TRANS");
			 } else {
				 fields.add("ITEM");
			 }
		} catch (AEFException e) {
			 
			fields.add("");
		}
		 fields.add(event.getDescription());
		 fields.add(event.getAmount());
		 fields.add(event.getDiscountMethod());
		 fields.add(event.getDiscountRate());
		 try {
			fields.add(String.valueOf(event.reducesTaxBalanceDue()));
		} catch (AEFException e) {
			 
			fields.add("");
		}
		 fields.add(event.getAppliesTo());
		 fields.add(currentTransId);
		writeEvent(fields); 
	 }

	 private String convertInputMethod(String in) {
		 String result=in;
		 
		 if (in.equalsIgnoreCase("ScannedItemCode")) {
			 result="SCANNED";
		 }
		 if (in.equalsIgnoreCase("KeyedItemCode")) {
			 result="KEYED";
		 }
		 if (in.equalsIgnoreCase("ItemLookupKeyed")) {
			 result="LOOKUP";
		 }
		 if (in.equalsIgnoreCase("LinktedItemCode")) {
			 result="LINKED";
		 }
 		 return result;
	 }
	 
}
