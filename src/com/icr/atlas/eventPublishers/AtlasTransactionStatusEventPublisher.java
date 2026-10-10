package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;

import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.ibm.retail.AEF.event.TransactionStatusListener;

public class AtlasTransactionStatusEventPublisher extends AtlasPublisher implements TransactionStatusListener {
	private static AtlasTransactionStatusEventPublisher instance;
	private TransactionStatusEvent lastTransactionStatusEvent;
	
	
	public static AtlasTransactionStatusEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasTransactionStatusEventPublisher();
		}
		return instance;
	}
	
	private AtlasTransactionStatusEventPublisher()  {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void transactionStatusEventOccurred(TransactionStatusEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		Iterator it=arg0.getPropertyEntries();
		while(it.hasNext()) {
			Map.Entry entry=(Map.Entry)it.next();
			System.out.println("  TSTAT "+entry.getKey()+":"+entry.getValue());
		}
		
		lastTransactionStatusEvent=arg0;
		publish(arg0);
		System.out.println("     TSTAT getType():"+arg0.getType());
		//call others
		if (arg0.getEventType().equalsIgnoreCase("transactionStart")) {
			if (arg0.getType().equalsIgnoreCase("noSale")) { //11-22-25 ---needs work, look at category
				publishIfExists("transactionNoSale",arg0);
			} else {
				publishIfExists("transactionStart",arg0);
			}
		}
		if (arg0.getEventType().equalsIgnoreCase("transactionEnd")) {
			if (!arg0.getCategory().equalsIgnoreCase("nonsales")) { //11-22.25
				publishIfExists("transactionEnd",arg0);
			}
		}
		if (arg0.isTotalProcessingStarting()) {
			publishIfExists("totalProcessingStart",arg0);
		}
		if (arg0.isTotalProcessingEnding()) {
			publishIfExists("totalProcessingEnd",arg0);
		}
		if (arg0.getEventType().equalsIgnoreCase("transactionVoid")) {
			publishIfExists("transactionVoid",arg0);
		}
		if (arg0.getEventType().equalsIgnoreCase("transactionSuspended")) {
			publishIfExists("transactionSuspended",arg0);
		}
		if (arg0.isRetrieveProcessingStarting()) {
			publishIfExists("retrieveProcessingStart",arg0);
		}
		if (arg0.isRetrieveProcessingEnding()) {
			publishIfExists("retrieveProcessingEnd",arg0);
		}
		
		sendListenerEvent(arg0);
	}
	
	public TransactionStatusEvent getLastTransactionStatusEvent() {
		return lastTransactionStatusEvent;
	}

}
