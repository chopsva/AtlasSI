package com.icr.atlas.eventPublishers;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;

import com.ibm.retail.AEF.event.ItemSalesEvent;
import com.ibm.retail.AEF.event.ItemSalesListener;

public class AtlasItemSalesEventPublisher extends AtlasPublisher implements ItemSalesListener {
	private static AtlasItemSalesEventPublisher instance;
	
	private AtlasItemSalesEventPublisher() {
		// TODO Auto-generated constructor stub
	}
	
	public static AtlasItemSalesEventPublisher getInstance() {
		if (instance==null) {
			instance=new AtlasItemSalesEventPublisher();
		}
		return instance;
	}

	@Override
	public void itemAdded(ItemSalesEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		Iterator it=arg0.getPropertyEntries();
		while(it.hasNext()) {
			Map.Entry entry=(Map.Entry)it.next();
			System.out.println("  ITEM "+entry.getKey()+":"+entry.getValue());
		}
		String eType="ADD";
		if (arg0.getEventType().equalsIgnoreCase("priceVerify")) {
			eType="VERIFY";
		}
		AtlasItemSalesEvent iE=new AtlasItemSalesEvent(arg0,eType);
		this.publish(iE);
	}

	@Override
	public void itemDepositAdded(ItemSalesEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		AtlasItemSalesEvent iE=new AtlasItemSalesEvent(arg0,"ADDDEP");
		this.publish(iE);
	}

	@Override
	public void itemDepositRemoved(ItemSalesEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		AtlasItemSalesEvent iE=new AtlasItemSalesEvent(arg0,"REMOVEDEP");
		this.publish(iE);
		
	}

	@Override
	public void itemRefund(ItemSalesEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		AtlasItemSalesEvent iE=new AtlasItemSalesEvent(arg0,"REFUND");
		this.publish(iE);
	}

	@Override
	public void itemRemoved(ItemSalesEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		AtlasItemSalesEvent iE=new AtlasItemSalesEvent(arg0,"REMOVE");
		this.publish(iE);
	}
	
}
