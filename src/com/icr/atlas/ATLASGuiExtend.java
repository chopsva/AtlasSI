package com.icr.atlas;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import java.awt.Dialog;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.OS4690.File4690;
import com.ibm.retail.AEF.data.CustomerProperties;
import com.ibm.retail.AEF.data.TransactionStatusProperties;
import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.ibm.retail.AEF.event.PointsEvent;
import com.ibm.retail.AEF.event.PointsTotal;
import com.ibm.retail.AEF.session.AEFSession;
import com.ibm.retail.AEF.session.SessionContext;
import com.ibm.retail.AEF.workstation.Workstation;
import com.ibm.retail.si.util.AEFException;
import com.ibm.sureview.application.SureviewApplication;
import com.ibm.sureview.application.SureviewState;
import com.ibm.sureview.application.UserData;
import com.ibm.sureview.inputSequences.InputSequences;
import com.icr.atlas.Datacap.DatacapController;
import com.icr.atlas.EventLogger.AtlasEventLogger;
import com.icr.atlas.POSGateway.AtlasPOSGateway;
import com.icr.atlas.eventPublishers.AtlasPOSMessageEventPublisher;
import com.icr.atlas.rmi.AtlasMessageService;

public class ATLASGuiExtend {
	
	private static ATLASGuiExtend instance=null;
	private static Log log=LogFactory.getLog(ATLASGuiExtend.class);
	
	public ATLASSIGuiSessionExtension theAEFSession=null;
	private String thisTermId="000";
	private String atlasClassExtensions="";
	private String atlasCustomerClassExtensions="";
	
	
	private String UD_CUSTOMER_NAME="ICR_CUSTOMER_NAME";
	private String UD_CUSTOMER_ID="ICR_CUSTOMER_ID";
	private String UD_CUSTOMER_POINTS="ICR_CUSTOMER_POINTS";
	private float pointsMultiplier=0;
	private boolean formatPointsAsMoney=false;
	
	
	private String Customer_Name="";
	private String Customer_Id="";
	private String Customer_Points="";
	
	private boolean onSecondJVM=false;
	private AtlasMessageService atlasMessageService=null;
	
	private Object theStatusBar=null;
	private CommState currentCommState=CommState.OTHER;
	private class CommStatusRecord {
		public final CommState status;
		public final long timestamp;
		
		public CommStatusRecord(CommState status) {
			this.status=status;
			this.timestamp=System.currentTimeMillis();
		}
	}
	private final Map<String,CommStatusRecord> commStatusMap=new ConcurrentHashMap<>();
	
	//SINGLETON
	public static ATLASGuiExtend getInstance() {
			if (instance==null) {
				 synchronized(ATLASGuiExtend.class) {
					 if (instance==null) {
						 instance=new ATLASGuiExtend();
					 }
				 }
			}
			return instance;
	}
	
	private ATLASGuiExtend() {
		readProps();
		//TestSubscribe ts=new TestSubscribe();
		//AtlasEventLogger ael=new AtlasEventLogger();
		
	}
	
	public enum CommState {
		DISABLED,
		ACTIVE,
		GOOD,
		WARN,
		ERROR,
		OTHER
	}
	

	
	public void setAEFSession(ATLASSIGuiSessionExtension e) {
		theAEFSession=e;
		try {
			thisTermId=theAEFSession.theSession.getTerminalNumber();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public void secondJVMStart() {
		log.info("ATLASGuiExetension JVM2 start");
		System.out.println("ATLASGuiExetension JVM2 start");
		onSecondJVM=true;
		initUserData();
		startAtlasExtensions();
		
	}
	
	public boolean onSecondJVM() {
		return onSecondJVM;
	}
	
	public AtlasMessageService getAtlasMessageService() {
		return atlasMessageService;
	}
	
	public void connectCGUI() {
		log.info("connecting to AtlasMessageService to send");
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry("localhost",6260);
			atlasMessageService=(AtlasMessageService) registry.lookup("AtlasMessageService");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (atlasMessageService!=null) {
			log.info("We have AtlasMessageService");
		}
	}
	//TODO- move read Props to here so we can have the terminal ID
	public void sessionReady() {
		log.info("ICRSIGUIExtension Session Ready");
		initUserData();
		//TestSubscribe ts=new TestSubscribe();
		//AtlasEventLogger ael=new AtlasEventLogger();
		startAtlasExtensions();
		if (!onSecondJVM) {
			connectCGUI();
		}
	}
	
	private void startAtlasExtensions() {
		System.out.println("startAtlasExtensions");
		String theClassList=atlasClassExtensions;
		if (onSecondJVM) {
			System.out.println("switching to atlasCustomerClassExtensions");
			theClassList=atlasCustomerClassExtensions;
		}
		System.out.println("classList:"+theClassList);		
		
		//for(String className : atlasClassExtensions.split(",")) {
		for(String className : theClassList.split(",")) {		
			System.out.println("Attempting to start "+className);
			if (className.charAt(0)!='*') {
				System.out.println("starting as object");
				try {
					Class<?> cls=Class.forName(className.trim());
					Object instance=cls.getDeclaredConstructor().newInstance( );
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("starting as SINGLETON "+className.substring(1) );
				try {
					Class<?> cls=Class.forName(className.substring(1).trim());
					Method instanceMethod=cls.getMethod("getInstance");
					Object instance=instanceMethod.invoke(null);
					
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("-----------------");
		}
		if (!onSecondJVM) {
			System.out.println("starting AtlasPOSGateway");
			new Thread(() ->{AtlasPOSGateway apg=AtlasPOSGateway.getInstance();}).start();
		}
	}
	
	public String getTermId() {
		return thisTermId;
	}
	
	public void propertyChanged(AEFPropertyChangeEvent event) {
		//System.out.println("PropChanged:"+event.getCategoryName()+":"+event.getPropertyName()+"="+event.getNewValue());
		if (event.getCategoryName().equals(CustomerProperties.CATEGORY)) {
			customerChangeEvent(event);
		}
		if (event.getCategoryName().equals(TransactionStatusProperties.CATEGORY)) {
			transactionStatusEvent(event);
		}
		
		if (atlasMessageService != null && !onSecondJVM) {
			try {
				atlasMessageService.sendPropertyChangeEvent(event);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		System.out.println("------propChange");
//		System.out.println(event);
	}
	
	
	public void customerChangeEvent(AEFPropertyChangeEvent event) {
	  try {
		//System.out.println("cust--"+event.getPropertyName());
		if (event.getPropertyName().equals("data")) {
			log.debug("cust data event!!");
			System.out.println("cust data event"+(String)event.getNewValue());
			//updateUserData(UD_CUSTOMER_POINTS,"...");
		}
		if (event.getPropertyName().equals("name")) {
			updateUserData(UD_CUSTOMER_NAME,(String)event.getNewValue());
			Customer_Name=(String)event.getNewValue();
			log.debug("set Customer_Name"+Customer_Name);
			System.out.println("set Customer_Name"+Customer_Name);
		}
		if (event.getPropertyName().equals("id")) {
			updateUserData(UD_CUSTOMER_ID,(String)event.getNewValue());
			Customer_Id=(String)event.getNewValue();
			log.debug("set Customer_Id"+Customer_Id);
			System.out.println("set Customer_Id"+Customer_Id);
			
		}
//		if (event.getPropertyName().equals("pointsBalances")) {
//			System.out.println("pointsBalances event!");
//			ArrayList al=(ArrayList) event.getNewValue();
//			for(int i=0;i<al.size();i++) {
//				System.out.println("i"+al.get(i).getClass().getName());
//				log.debug("Points "+i+":"+al.get(i));
//				System.out.println("Points "+i+":"+al.get(i));
//				PointsTotal pe=(PointsTotal)al.get(i);
//				System.out.println("point obj:"+i);
//				System.out.println("Points:"+pe.getValue());
//				System.out.println("Type:"+pe.getType());
//				System.out.println("Descr:"+pe.getDescription());
//			}
//			
//		}
		//if (event.getPropertyName().equals("pointsTotals")) {  //changed to balances 8/22/25
		if (event.getPropertyName().equals("pointsBalances")) {
			//updateUserData(UD_CUSTOMER_POINTS,"626");
			log.debug("customer pointsBalances event");
			System.out.println("customer pointsBalances event");
			//updateUserData(UD_CUSTOMER_POINTS,"---");
			Customer_Points="..";
			ArrayList al=(ArrayList) event.getNewValue();
			log.debug("point array size"+al.size());
			System.out.println("point array size"+al.size());
			for(int i=0;i<al.size();i++) {
				log.debug("Points "+i+":"+al.get(i));
				System.out.println("Points "+i+":"+al.get(i));
				PointsTotal pe=(PointsTotal)al.get(i);
				System.out.println("point obj:"+i);
				System.out.println("Points:"+pe.getValue());
				System.out.println("Type:"+pe.getType());
				System.out.println("Descr:"+pe.getDescription());
				if (pe.getType().equalsIgnoreCase("PRIMARY")) {
					if (pointsMultiplier!=0) {
						int tmpPoints=Integer.parseInt(pe.getValue());
						float workPoints=tmpPoints * pointsMultiplier;
						Customer_Points=Float.toString(workPoints);
						if (formatPointsAsMoney) {
							Customer_Points=NumberFormat.getCurrencyInstance().format(workPoints);
						}
					} else {
						Customer_Points=pe.getValue(); //mult 0?, take whatever is given
					}
					updateUserData(UD_CUSTOMER_POINTS,Customer_Points);
				}
			}
			
			//Customer_Points="626"; //(String)event.getNewValue();
			log.debug("set Customer_Points"+Customer_Points);
			System.out.println("set Customer_Points"+Customer_Points);
		}
		//testing
		updateUserData("CUSTOMER_TEST_TEXT",Customer_Id+":"+Customer_Points);
	  } catch (Exception e) {
		  e.printStackTrace();
		  System.out.println("EXCEPTION in CUSTOMER EVENT");
	  }
	}
	
	public void transactionStatusEvent(AEFPropertyChangeEvent event) {
		//System.out.println("transation status");
		//System.out.println(event);
		if (event.getNewValue().equals("transactionEnd")) {
			endTransaction();
		}
		if (event.getNewValue().equals("transactionVoid")) {
			endTransaction();
		}
	}
	
	private void endTransaction() {
		initUserData();
	}
	
	private void initUserData() {
		updateUserData(UD_CUSTOMER_NAME,"");
		updateUserData(UD_CUSTOMER_ID,"");
		updateUserData(UD_CUSTOMER_POINTS,"");
		updateUserData("CUSTOMER_TEST_TEXT","---...---");
	}
	
	public void updateUserData(String userData,String value) {
		log.debug("update UserData("+userData+","+value+")");
		try {
			UserData ud=SureviewApplication.getInstance().getUserData();
			ud.putUsingSwingThread(userData, value); 
		} catch (Exception e) {
			log.debug("Set userdata fail");
			log.debug(e);
		} catch (NoClassDefFoundError err) {
			log.debug("SureviewApplication not there? CHEC?");
			log.debug(err);
		}
	}
	//TODO- NEED TERMINAL SPECIFIC OPTIONS
	private void readProps() {
		//just read f:/atlas.properties for now
		Properties theProps=new Properties();
		//String theFileName="/cdrive/f_drive/atlas.properties";
		try {
			//theProps.load(new FileInputStream(theFileName));
			theProps.load(ATLASGuiExtend.class.getClassLoader().getResourceAsStream("atlas.properties"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		atlasClassExtensions=theProps.getProperty("atlasClassExtensions");
		log.info("atlasClassExtensions:"+atlasClassExtensions);
		atlasCustomerClassExtensions=theProps.getProperty("atlasCustomerClassExtensions");
		log.info("atlasCustomerClassExtensions:"+atlasCustomerClassExtensions);
		
		UD_CUSTOMER_NAME=theProps.getProperty("UD_CUSTOMER_NAME", UD_CUSTOMER_NAME);
		UD_CUSTOMER_ID=theProps.getProperty("UD_CUSTOMER_ID",UD_CUSTOMER_ID);
		UD_CUSTOMER_POINTS=theProps.getProperty("UD_CUSTOMER_POINTS",UD_CUSTOMER_POINTS);
		formatPointsAsMoney=Boolean.parseBoolean(theProps.getProperty("formatPointsAsMoney",Boolean.toString(formatPointsAsMoney)));
		float tmp=1.0f;
		tmp=Float.parseFloat(theProps.getProperty("pointsMultiplier",Float.toString(pointsMultiplier)));
		pointsMultiplier=tmp;

		log.info("UD_CUSTOMER_NAME:"+UD_CUSTOMER_NAME);
		log.info("UD_CUSTOMER_ID:"+UD_CUSTOMER_ID);
		log.info("UD_CUSTOMER_POINTS:"+UD_CUSTOMER_POINTS);
		log.info("formatPointsAsMoney:"+formatPointsAsMoney);
		log.info("pointsMultiplier:"+pointsMultiplier);
		
		System.out.println("ATLAS:UD_CUSTOMER_NAME:"+UD_CUSTOMER_NAME);
		System.out.println("ATLAS:UD_CUSTOMER_ID:"+UD_CUSTOMER_ID);
		System.out.println("ATLAS:UD_CUSTOMER_POINTS:"+UD_CUSTOMER_POINTS);
		System.out.println("ATLAS:formatPointsAsMoney:"+formatPointsAsMoney);
		System.out.println("ATLAS:pointsMultiplier:"+pointsMultiplier);

	}
	
	public void sendKeySequence(String seq) {
		System.out.println("sendKeySequence:"+seq);
		SureviewState cState=null;
		cState=SureviewApplication.getInstance().getSureviewState();
		InputSequences iSeqs=cState.getInputSequences();
		System.out.println("----D");
		iSeqs.postKeySequence(seq);
		System.out.println("---E");		
	}
	
	public void sendAtlasPipeData(String msgData) {
		StringBuffer data=new StringBuffer();
		data.append("<AtlasTestEntry>");
		data.append(msgData);
		data.append("</AtlasTestEntry>");
		System.out.println("data:"+data);
		try {
			System.out.println("---a");
			//AEFSession session=SessionContext.getSession();
			//System.out.println(" session:"+session);
			//Workstation wkstn=session.getWorkstation();
			//System.out.println("   wkstn:"+wkstn);
			//SessionContext.getSession().getWorkstation().putDataOnApplicationPipe(data.toString());
			AEFSession session=theAEFSession.theSession;
			System.out.println(" session:"+session);
			Workstation wkstn=session.getWorkstation();
			System.out.println("  wkstn:"+wkstn);
			//wkstn.setLineDisplayText("CHECKING",1,1,true);
			
			wkstn.putDataOnApplicationPipe(data.toString());
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

	public String processPOSMessage(String msg) {
		String result="";
		StringBuffer out=new StringBuffer();
		
		String datacap=getXML(msg,"Datacap");
		if (datacap!="") {
			result=DatacapController.getInstance().handlePOSMessage(datacap);
			out.append(result);
		} 
		//call listeners who can't return anything
		AtlasPOSMessageEventPublisher.getInstance().receivePOSMessage(msg);		
		
		
		//JOptionPane.showMessageDialog(null, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
		
		//esult="<HelloWorld>Hi!</HelloWorld>";
		
//		out.append("<plainPrintLines>");
//		for(int i=1;i<3;i++) {
//			out.append("<Line"+i+">.....THIS IS "+i+".....</Line"+i+">");
//		}
//		out.append("</plainPrintLines>");
		
		//out.append("<ACEPrompt><promptText>DO YOU FEEL BONITA?</promptText></ACEPrompt>");
		result=out.toString();
		
		// TODO Auto-generated method stub
		return result;
	}
	
	public String getXML(String xml, String tag) {
		String result = "";
		String startTag = "<" + tag + ">";
		String endTag = "</" + tag + ">";

		int start = xml.indexOf(startTag);
		int end = xml.indexOf(endTag);

		if (start != -1 && end != -1 && end > start) {
			result = xml.substring(start + startTag.length(), end);
		}

		return result;
	}
	
	
	public void setStatusBar(Object sb) {
		theStatusBar=sb;
	}
	
	public void setCommState(CommState theState) {
		StackTraceElement[] stack=Thread.currentThread().getStackTrace();
		String callerClass=stack[3].getClassName();
		String caller=callerClass.substring(callerClass.lastIndexOf('.')+1);
		setCommState(theState,caller);
	}
	
	public void setCommState(CommState theState,String caller) {
		System.out.println("setCommState CALLED:"+theState);
		if (theStatusBar==null) {
			return;
		}
		
//		StackTraceElement[] stack=Thread.currentThread().getStackTrace();
//		String callerClass=stack[3].getClassName();
//		String caller=callerClass.substring(callerClass.lastIndexOf('.')+1);
		System.out.println("      caller:"+caller);
		this.commStatusMap.put(caller,new CommStatusRecord(theState));
		CommState newState=CommState.DISABLED;
		for(CommStatusRecord record: commStatusMap.values()) {
			System.out.println("record:"+record.status.ordinal()+ " newState:"+newState.ordinal());
			if (record.status.ordinal()>newState.ordinal()) {
				System.out.println("   setting newState:"+record.status.name());
				newState=record.status;
			}
		}
		System.out.println("FINAL newState:"+newState.name());
		Method m;
		try {
			m = theStatusBar.getClass().getMethod("setCommState", CommState.class);
			m.invoke(theStatusBar,newState);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void showCommStatusOLD() {
		String theList="";
		for(Map.Entry<String, CommStatusRecord> entry: commStatusMap.entrySet()) {
			String className=entry.getKey();
			CommStatusRecord csr=entry.getValue();
			theList +=className+"-"+csr.status.name()+"\n\r";
		}
		JOptionPane.showMessageDialog(null, theList, "Comm Status", JOptionPane.INFORMATION_MESSAGE);
	}
	public void showCommStatus(Component parent) {
		java.util.List<Map.Entry<String, CommStatusRecord>> entries=new java.util.ArrayList<>(commStatusMap.entrySet());
		String[] columns={"Key","Status","Timestamp"};
		Object[][] data=new Object[entries.size()][columns.length];
		java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat("HH:mm:ss MM/dd");
		int i=0;
		for(Map.Entry<String, CommStatusRecord> entry : entries) {
			CommStatusRecord csr=entry.getValue();
			data[i][0]=entry.getKey();
			data[i][1]=csr.status.name();
			data[i][2]=sdf.format(new java.util.Date(csr.timestamp));
			i++;
		}
		JTable table=new JTable(data, columns);
		table.setEnabled(false);
		table.setPreferredScrollableViewportSize(new java.awt.Dimension(700,150));
		JScrollPane scrollPane=new JScrollPane(table);
		//JOptionPane.showMessageDialog(parent,scrollPane,"Comm Status",JOptionPane.PLAIN_MESSAGE);
		
		final JDialog dialog;
		dialog=new JDialog((Window) parent,"Comm Status",Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	    dialog.setLayout(new BorderLayout());
	    dialog.add(scrollPane, BorderLayout.CENTER);

	    dialog.pack();
	    dialog.setLocationRelativeTo(parent); // center over parent (or screen if null)
	    dialog.setResizable(true);            // or false if you want fixed size

	    dialog.setVisible(true);
	}
	
	
	
}
