package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.retail.AEF.event.OperatorListener;

public class AtlasOperatorEventPublisher extends AtlasPublisher implements OperatorListener {
	private static AtlasOperatorEventPublisher instance;
	
	private OperatorEvent lastOperatorEvent;
	
	public static AtlasOperatorEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasOperatorEventPublisher();
		}
		return instance;
	}

	private AtlasOperatorEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void operatorEventOccurred(OperatorEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		lastOperatorEvent=arg0;
		publish(arg0);
		//call others
		 if (arg0.getEventType().equalsIgnoreCase("SIGNON")) {
			 publishIfExists("signOn",arg0);
		 }
		 if (arg0.getEventType().equalsIgnoreCase("SIGNOFF")) {
			 publishIfExists("signOff",arg0);
		 }
		 if (arg0.getEventType().equalsIgnoreCase("LOCK")) {
			 publishIfExists("termLock",arg0);
		 }
		 if (arg0.getEventType().equalsIgnoreCase("UNLOCK")) {
			 publishIfExists("termUnlock",arg0);
		 }
		 sendListenerEvent(arg0);
		 
	}

	public OperatorEvent getLastOperatorEvent() {
		return lastOperatorEvent;
	}
}
