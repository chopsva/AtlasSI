package com.icr.atlas.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.POSAppEvent;
import com.icr.atlas.eventPublishers.AtlasItemSalesEvent;

public interface AtlasMessageService extends Remote {

	boolean sendEvent(CustomerEvent event) throws RemoteException;
	void sendPropertyChangeEvent(AEFPropertyChangeEvent event) throws RemoteException;
    void sendListenerEvent(POSAppEvent event) throws RemoteException;
    void sendItemSalesEvent(AtlasItemSalesEvent event) throws RemoteException;
    
	
}
