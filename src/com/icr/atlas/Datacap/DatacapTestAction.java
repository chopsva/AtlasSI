package com.icr.atlas.Datacap;

import java.awt.event.ActionEvent;
import java.util.Hashtable;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.ibm.sureview.action.SureviewAction;
import com.ibm.sureview.guiComponent.SureviewActionData;
import com.icr.atlas.ATLASGuiExtend;

public class DatacapTestAction extends SureviewAction {

	public DatacapTestAction(String aID) {
		super(aID);
		// TODO Auto-generated constructor stub
	}

	public DatacapTestAction(SureviewActionData theActionData) {
		super(theActionData);
		// TODO Auto-generated constructor stub
	}

	public DatacapTestAction(String aID, boolean regBundles) {
		super(aID, regBundles);
		// TODO Auto-generated constructor stub
	}

	public DatacapTestAction(String aID, Hashtable attributes) {
		super(aID, attributes);
		// TODO Auto-generated constructor stub
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		System.out.println("attempting to start controllertest");
		String l="<BiteME>ddddddd</BiteMe>";
		ATLASGuiExtend.getInstance().sendAtlasPipeData(l);
		System.out.println("send done");
		
		SwingUtilities.invokeLater(()-> {
		ControllerTest ct=new ControllerTest();
		//ct.setVisible(true);
		//ct.runAuto();
		JFrame existingFrame=(JFrame)javax.swing.FocusManager.getCurrentManager().getActiveWindow();
		JDialog dialog=new JDialog(existingFrame,"DATACAP TEST",false);
		dialog.getContentPane().add(ct);
		dialog.pack();
		dialog.setLocationRelativeTo(existingFrame);
		dialog.setVisible(true);
		});
		//ct.runAuto();
		System.out.println("actionPerformed end");
	}
}
