package com.icr.atlas.eventPublishers;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.CustomerListener;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.rmi.AtlasMessageService;

public class AtlasCustomerEventPublisher extends AtlasPublisher implements CustomerListener {
	private static AtlasCustomerEventPublisher instance;
	
		
	private AtlasCustomerEventPublisher() {
		// TODO Auto-generated constructor stub
	}
	public static synchronized AtlasCustomerEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasCustomerEventPublisher();
		}
		return instance;
	}
	

	@Override
	public void customerCardEntered(CustomerEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		this.publish(arg0);
		sendListenerEvent(arg0);
	}

}
