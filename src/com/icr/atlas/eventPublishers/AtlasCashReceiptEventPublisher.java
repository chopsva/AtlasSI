package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.CashReceiptEvent;
import com.ibm.retail.AEF.event.CashReceiptListener;

public class AtlasCashReceiptEventPublisher extends AtlasPublisher implements CashReceiptListener {

	private static AtlasCashReceiptEventPublisher instance;
	
	public static AtlasCashReceiptEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasCashReceiptEventPublisher();
		}
		return instance;
	}
	
	private AtlasCashReceiptEventPublisher() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void linePrinted(CashReceiptEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		publish(arg0);
		
		
	}
	

}
