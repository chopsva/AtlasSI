package com.icr.atlas.rmi;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.ibm.retail.AEF.event.CouponEvent;
import com.ibm.retail.AEF.event.CustomerEvent;
import com.ibm.retail.AEF.event.DiscountEvent;
import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.retail.AEF.event.POSAppEvent;
import com.ibm.retail.AEF.event.TenderEvent;
import com.ibm.retail.AEF.event.TransactionStatusEvent;
import com.ibm.retail.AEF.event.TransactionTotalsEvent;
import com.ibm.retail.AEF.event.WorkstationStatusEvent;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.eventPublishers.AtlasCouponEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCustomerEventPublisher;
import com.icr.atlas.eventPublishers.AtlasDiscountEventPublisher;
import com.icr.atlas.eventPublishers.AtlasItemSalesEvent;
import com.icr.atlas.eventPublishers.AtlasItemSalesEventPublisher;
import com.icr.atlas.eventPublishers.AtlasOperatorEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTenderEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionStatusEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionTotalsEventPublisher;
import com.icr.atlas.eventPublishers.AtlasWorkstationStatusEventPublisher;

public class AtlasMessageServiceImpl extends UnicastRemoteObject implements AtlasMessageService {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AtlasMessageServiceImpl() throws RemoteException {
		// TODO Auto-generated constructor stub
		super();
	}

	public AtlasMessageServiceImpl(int port) throws RemoteException {
		super(port);
		// TODO Auto-generated constructor stub
	}

	public AtlasMessageServiceImpl(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf)
			throws RemoteException {
		super(port, csf, ssf);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean sendEvent(CustomerEvent event) throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("Received CustomerEvent:"+event);
		AtlasCustomerEventPublisher.getInstance().customerCardEntered(event);
		return true;
	}

	@Override
	public void sendPropertyChangeEvent(AEFPropertyChangeEvent event) throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("Recived PropChangeEvent");
		ATLASGuiExtend.getInstance().propertyChanged(event);
	}

	@Override
	public void sendListenerEvent(POSAppEvent event) throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("POSAppEvent received");
		if (event instanceof CustomerEvent) {
			AtlasCustomerEventPublisher.getInstance().customerCardEntered((CustomerEvent)event);
		}
		if (event instanceof TransactionStatusEvent) {
			AtlasTransactionStatusEventPublisher.getInstance().transactionStatusEventOccurred((TransactionStatusEvent)event);
		}
		if (event instanceof TenderEvent) {
			AtlasTenderEventPublisher.getInstance().tenderAccepted((TenderEvent)event);
		}
		if (event instanceof WorkstationStatusEvent) {
			AtlasWorkstationStatusEventPublisher.getInstance().workstationStatusChanged((WorkstationStatusEvent) event);
		}
		if (event instanceof CouponEvent) {
			AtlasCouponEventPublisher.getInstance().couponApplied((CouponEvent) event);
		}
		if (event instanceof OperatorEvent) {
			AtlasOperatorEventPublisher.getInstance().operatorEventOccurred((OperatorEvent) event);
		}
		if (event instanceof TransactionTotalsEvent) {
			AtlasTransactionTotalsEventPublisher.getInstance().totalsChanged((TransactionTotalsEvent) event);
		}
		if (event instanceof DiscountEvent) {
			AtlasDiscountEventPublisher.getInstance().discountApplied((DiscountEvent) event);
		}
	}

	@Override
	public void sendItemSalesEvent(AtlasItemSalesEvent event) throws RemoteException {
		// TODO Auto-generated method stub
		switch (event.getEventType()) {
		case "ADD":
			AtlasItemSalesEventPublisher.getInstance().itemAdded(event.getItemSalesEvent());
			break;
		case "ADDDEP":
			AtlasItemSalesEventPublisher.getInstance().itemDepositAdded(event.getItemSalesEvent());
			break;
		case "REMOVEDEP":
			AtlasItemSalesEventPublisher.getInstance().itemDepositRemoved(event.getItemSalesEvent());
			break;
		case "REFUND":
			AtlasItemSalesEventPublisher.getInstance().itemRefund(event.getItemSalesEvent());
			break;
		case "REMOVE":
			AtlasItemSalesEventPublisher.getInstance().itemRemoved(event.getItemSalesEvent());
			break;
		default:
			System.out.println("Unknown itemsales event"+event);
			break;
			
		}
	}

}
