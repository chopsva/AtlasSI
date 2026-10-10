package com.icr.atlas;

import java.rmi.RemoteException;
import java.util.Vector;

import com.ibm.retail.AEF.data.POSDataProvider;
import com.ibm.retail.AEF.session.AEFSession;
import com.ibm.retail.AEF.session.SessionStatusEvent;
import com.ibm.retail.AEF.session.SessionStatusListener;
import com.ibm.retail.si.util.AEFException;
import com.ibm.sureview.ace.ACECustomerGUI;
import com.icr.atlas.eventPublishers.AtlasCustomerEventPublisher;
import com.icr.atlas.rmi.AtlasMessageServer;


public class ATLASCustomerGUI extends ACECustomerGUI implements Runnable, SessionStatusListener {

	ATLASGuiExtend theInstance;
	
	public ATLASCustomerGUI() {
		// TODO Auto-generated constructor stub
		super();
		System.out.println("ATLASCustomerGUI CREATED!!!");
		
	}

	@Override
	public boolean createSession() {
		boolean result=super.createSession();
		
		System.out.println("ATLASCustomerGUI createSession result:"+result);
		System.out.println("session:"+session);
		System.out.println("svApp session:"+svApp.getSession());
	
		//int k=0;
		//int f=1/k;
		return result;
	}
	
	public void sessionReady(SessionStatusEvent arg0) {
		System.out.println("******************************************************");
		System.out.println("session:"+session);
		try {
			System.out.println("session readty:"+session.isReady());
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		theInstance=ATLASGuiExtend.getInstance();
		theInstance.secondJVMStart();
		AtlasMessageServer server=new AtlasMessageServer();
//		//do what the sessionextension tries to do 
//		try {
//			System.out.println("A");
//			POSDataProvider dataProvider=session.getPOSDataProvider();
//			System.out.println("B");
//			dataProvider.addCustomerListener(AtlasCustomerEventPublisher.getInstance());
//			System.out.println("C");
//		} catch (RemoteException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (AEFException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
	}
	
	@Override
	public void registerWithSession(Vector v) throws Exception{
		System.out.println("registerWithSession called");
		System.out.println("Sesssion ready?:"+session.isReady());
		super.registerWithSession(v);
	}
	
	public static void main(String args[]) {
		System.out.println("STARTING ATLASCustomerGUI");
		new ATLASCustomerGUI();
	}
	



}
