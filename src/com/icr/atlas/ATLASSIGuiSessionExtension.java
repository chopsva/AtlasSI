package com.icr.atlas;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.automation.POSAutomationProvider;
import com.ibm.retail.AEF.client.AEFPropertyListenerProxy;
import com.ibm.retail.AEF.client.SessionStatusListenerProxy;
import com.ibm.retail.AEF.data.POSDataProvider;
import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.ibm.retail.AEF.event.AEFPropertyChangeListener;
import com.ibm.retail.AEF.session.AEFSession;
import com.ibm.retail.AEF.session.SessionExtension;
import com.ibm.retail.AEF.session.SessionStatusEvent;
import com.ibm.retail.AEF.session.SessionStatusListener;
import com.ibm.retail.si.util.AEFException;
import com.ibm.sureview.factory.FactoryTable;
import com.ibm.sureview.factory.SessionManagerFactory;
import com.icr.atlas.eventPublishers.AtlasAEFPropertyChangeEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCashReceiptEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCouponEventPublisher;
import com.icr.atlas.eventPublishers.AtlasCustomerEventPublisher;
import com.icr.atlas.eventPublishers.AtlasDiscountEventPublisher;
import com.icr.atlas.eventPublishers.AtlasItemSalesEventPublisher;
import com.icr.atlas.eventPublishers.AtlasOperatorEventPublisher;
import com.icr.atlas.eventPublishers.AtlasOptionsEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTenderEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionStatusEventPublisher;
import com.icr.atlas.eventPublishers.AtlasTransactionTotalsEventPublisher;
import com.icr.atlas.eventPublishers.AtlasWorkstationStatusEventPublisher;

public class ATLASSIGuiSessionExtension  implements SessionExtension, SessionStatusListener,
AEFPropertyChangeListener {

	AEFSession theSession=null;
	POSAutomationProvider automation=null;
	POSDataProvider dataProvider=null;
	AEFPropertyListenerProxy proxy=null;
	SessionStatusListenerProxy sessionStatusProxy=null;
	
	public ATLASSIGuiSessionExtension() {
		System.out.println("ICRSIGuiSessionExtension started");
	}
		
	@Override
	public void propertyChanged(AEFPropertyChangeEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		ATLASGuiExtend.getInstance().propertyChanged(arg0);
	}

	@Override
	public void sessionEnded(SessionStatusEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void sessionReady(SessionStatusEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("ICRSIGui session is ready");
		try {
			dataProvider=theSession.getPOSDataProvider();
			
		} catch (Throwable t) {
			System.out.println("ICRSIGui getting dataprovider:"+t);
		}
		System.out.println("ICRSIGui dataprovider:"+dataProvider);
		
		//link up AEFPropertyListener
		System.out.println("ICRSIGui attaching AEFPropertyListener");
		
		dataProvider.addAEFPropertyChangeListener(this);

		try {
			dataProvider.addCustomerListener(AtlasCustomerEventPublisher.getInstance());
			dataProvider.addItemSalesListener(AtlasItemSalesEventPublisher.getInstance());
			dataProvider.addTransactionStatusListener(AtlasTransactionStatusEventPublisher.getInstance());
			dataProvider.addTenderListener(AtlasTenderEventPublisher.getInstance());
			dataProvider.addWorkstationStatusListener(AtlasWorkstationStatusEventPublisher.getInstance());
			dataProvider.addCouponListener(AtlasCouponEventPublisher.getInstance());
			dataProvider.addOperatorListener(AtlasOperatorEventPublisher.getInstance());
			dataProvider.addTransactionTotalsListener(AtlasTransactionTotalsEventPublisher.getInstance());
			dataProvider.addDiscountListener(AtlasDiscountEventPublisher.getInstance());
			dataProvider.addAEFPropertyChangeListener(AtlasAEFPropertyChangeEventPublisher.getInstance());
			dataProvider.addCashReceiptListener(AtlasCashReceiptEventPublisher.getInstance());
			dataProvider.addOptionsListener(AtlasOptionsEventPublisher.getInstance());
		} catch (AEFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		ATLASGuiExtend.getInstance().setAEFSession(this);
		ATLASGuiExtend.getInstance().sessionReady();
	}

	@Override
	public void sessionStatusChanged(SessionStatusEvent arg0) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSession(AEFSession arg0) {
		// TODO Auto-generated method stub
		System.out.println("ICRSIGuiSessionExtension  setSessionCalled");
		theSession=arg0;

		try {
			automation=theSession.getPOSAutomationProvider();
			sessionStatusProxy= new SessionStatusListenerProxy(theSession,this);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AEFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
}
