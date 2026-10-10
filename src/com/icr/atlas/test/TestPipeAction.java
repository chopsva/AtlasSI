package com.icr.atlas.test;

import java.util.Set;

import com.ibm.retail.AEF.action.WriteToPipeActionImpl;
import com.ibm.retail.AEF.automation.ActionRequest;
import com.ibm.retail.AEF.session.SessionContext;
import com.ibm.retail.si.util.AEFException;

public class TestPipeAction extends WriteToPipeActionImpl {

	public TestPipeAction(ActionRequest request) throws AEFException {
		super(request);
		// TODO Auto-generated constructor stub
		System.out.println("TestPipeAction created");
	}

	@Override
	protected Set createOptionalArguments() {
		// TODO Auto-generated method stub
		return createEmptySet();
	}

	@Override
	protected Set createRequiredArguments() {
		// TODO Auto-generated method stub
		Set requiredArgs = createEmptySet();
		return requiredArgs;
	}

	@Override
	protected String formatDataForPipe(ActionRequest arg0) throws AEFException {
		// TODO Auto-generated method stub
		//return null;
		System.out.println("format for pipe called");
		System.out.println("   arg:"+arg0);
		StringBuffer data=new StringBuffer();
		data.append("<AtlasTestEntry>");
		data.append("<TestData>HELLO WORLD</TestData>");
		data.append("</AtlasTestEntry>");
		System.out.println("data:"+data);
		return data.toString();
	}

	@Override
	protected void validateArgument(String arg0, Object arg1) throws AEFException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object performAction() throws AEFException {
		System.out.println("perform action called");
		Object result=super.performAction();
		System.out.println("back from performAction"+result);
		//SessionContext.getSession().getWorkstation().putDataOnApplicationPipe(arg0);
		return result;
	}
	
}
