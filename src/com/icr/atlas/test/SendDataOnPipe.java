package com.icr.atlas.test;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.ibm.retail.AEF.action.AEFAction;
import com.ibm.retail.AEF.automation.ActionRequest;
import com.ibm.retail.AEF.factory.ActionFactory;
import com.ibm.retail.AEF.factory.FactoryImpl;
import com.ibm.retail.AEF.session.SessionContext;
import com.ibm.retail.si.util.AEFException;
import com.ibm.sureview.application.SureviewApplication;
import com.ibm.sureview.application.SureviewState;
import com.ibm.sureview.inputSequences.InputSequences;
import com.icr.atlas.ATLASGuiExtend;

public class SendDataOnPipe {

	public SendDataOnPipe() {
		// TODO Auto-generated constructor stub
	}
	
	public void tryIt() {
		System.out.println("tryIt called");
		StringBuffer data=new StringBuffer();
		data.append("<plainPrintLines>");
		for(int i=1;i<30;i++) {
			data.append("<Line"+i+">");
			data.append("..........This is line "+i+".......");
			data.append("</Line"+i+">");
		}
		data.append("</plainPrintLines>");
		//new Thread(()->{
			ATLASGuiExtend.getInstance().sendAtlasPipeData(data.toString());
		//}).start();
	}
	
	public void tryItInternal() {
		System.out.println("tryIt called");
		//do it all our selves
		StringBuffer data=new StringBuffer();
		data.append("<AtlasTestEntry>");
		data.append("<TestData>HELLO WORLD</TestData>");
		data.append("</AtlasTestEntry>");
		System.out.println("data:"+data);
		try {
			System.out.println("---a");
			SessionContext.getSession().getWorkstation().putDataOnApplicationPipe(data.toString());
			System.out.println("---b");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AEFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("---C");
		SureviewState cState=null;
		cState=SureviewApplication.getInstance().getSureviewState();
		InputSequences iSeqs=cState.getInputSequences();
		System.out.println("----D");
		iSeqs.postKeySequence("[241][80]");
		System.out.println("---E");
		
	}
	
	public void tryItWithActions() {
		System.out.println("SendDataOnPipe-- tryIt()");
		Map<String,Object> myArgs=new HashMap<>();
		myArgs.put("toPrint", "HELLO WORLD!!!");
		//ActionRequest pipeRequest=new ActionRequest("com.icr.atlas.test.TestPipeAction",myArgs);
		ActionRequest pipeRequest=new ActionRequest("AtlasTestPipeAction",myArgs);
		System.out.println("pipe:"+pipeRequest);
		ActionRequest keyRequest=new ActionRequest("TransactionDataOnPipeAction",myArgs);
		System.out.println("key:"+keyRequest);
		Vector requests=new Vector();
		requests.add(pipeRequest);
		requests.add(keyRequest);
		myArgs.put("ACTIONS", requests);
		ActionFactory actionFactory;
		try {
			System.out.println("tryIT- a");
			actionFactory = (ActionFactory)FactoryImpl.getFactory("ActionFactory");
			if (actionFactory!=null) {
				System.out.println("tryIT- b");
				AEFAction theAction=(AEFAction)actionFactory.makeAction(new ActionRequest("CompoundAction",myArgs));
				System.out.println("tryIT- c");
				theAction.performAction();
				System.out.println("tryIT- d");
			} else {
				
				System.out.println("SEND DATA ON PIPE!! no actionFactory!!!");
			}
		} catch (AEFException e) {
			// TODO Auto-generated catch block
			System.out.println("tryIT- errrr");
			e.printStackTrace();
		}
		
	}

}
