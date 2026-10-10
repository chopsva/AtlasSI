package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;

import com.ibm.retail.AEF.event.TransactionTotalsEvent;
import com.ibm.retail.AEF.event.TransactionTotalsListener;

public class AtlasTransactionTotalsEventPublisher extends AtlasPublisher implements TransactionTotalsListener {
	private static AtlasTransactionTotalsEventPublisher instance;
	private TransactionTotalsEvent lastEvent;
	
	public static AtlasTransactionTotalsEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasTransactionTotalsEventPublisher();
		}
		return instance;
	}

	private AtlasTransactionTotalsEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void totalsChanged(TransactionTotalsEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		Iterator it=arg0.getPropertyEntries();
		while(it.hasNext()) {
			Map.Entry entry=(Map.Entry)it.next();
			System.out.println("  TOTALS "+entry.getKey()+":"+entry.getValue());
		}		
		
		lastEvent=arg0;
		publish(arg0);
		sendListenerEvent(arg0);
	}
	public TransactionTotalsEvent getLastTransactionTotalsEvent() {
		return lastEvent;
	}
	
}
