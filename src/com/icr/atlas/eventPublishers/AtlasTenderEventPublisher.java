package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.TenderEvent;
import com.ibm.retail.AEF.event.TenderListener;

public class AtlasTenderEventPublisher extends AtlasPublisher implements TenderListener {
	private static AtlasTenderEventPublisher instance;
	
	public static AtlasTenderEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasTenderEventPublisher();
		}
		return instance;
	}
	

	private AtlasTenderEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void tenderAccepted(TenderEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		publish(arg0);
		sendListenerEvent(arg0);
	}

}
