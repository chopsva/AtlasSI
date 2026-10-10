package com.icr.atlas.test;

import java.rmi.RemoteException;

import com.ibm.retail.AEF.workstation.PipeOutputDeviceImpl;
import com.ibm.retail.AEF.workstation.WorkstationImpl;
import com.ibm.retail.si.util.AEFException;

public class AtlasWorkstation extends WorkstationImpl {

	public AtlasWorkstation() throws RemoteException, AEFException {
		// TODO Auto-generated constructor stub
		super();
		System.out.println("ATLAS WORKSTATION INIT!");
	}
	
	@Override 
	public void createOutputPipe() throws AEFException, RemoteException {
		System.out.println("WK- CreateOutputPipe");
		System.out.println("      ouputPipe:"+outputPipe);
		super.createOutputPipe();
		//outputPipe=new PipeOutputDeviceImpl("pi:aceaf001",this);
	}
	
	@Override
    public void putDataOnApplicationPipe(String data) throws AEFException, RemoteException {
		System.out.println("WK- putDataOnApplicationPipe");
		System.out.println("     outputPipe:"+outputPipe);
		System.out.println(" outputPipeName:"+outputPipeName);
		super.putDataOnApplicationPipe(data);
		System.out.println("WK - out putDataOnApplicationPipe");
	}
	
	
}
