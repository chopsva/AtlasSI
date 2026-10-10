package com.icr.atlas.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.ibm.OS4690.FileInputStream4690;
import com.ibm.retail.AEF.event.OperatorEvent;
import com.ibm.sureview.application.SureviewApplication;
import com.ibm.sureview.application.SureviewState;
import com.ibm.sureview.inputSequences.InputSequences;
import com.icr.atlas.eventPublishers.AtlasOperatorEventPublisher;
import com.icr.atlas.eventPublishers.AtlasSubscriber;

public class AtlasTestScriptPlayer implements AtlasSubscriber<Object>{

	private String scriptFilename="R::adx_idt1:atlas.tst";
	private int topStep=0;
	private Thread scriptThread = null;
	private boolean scriptRunning = false;

    public static class Step {
        private final int testStep;
        private final String action;

        public Step(int testStep, String action) {
            this.testStep = testStep;
            this.action = action;
        }

        public int getTestStep() {
            return testStep;
        }

        public String getAction() {
            return action;
        }

        public boolean hasAction() {
            return action != null && action.trim().length() > 0;
        }

        @Override
        public String toString() {
            return "Step{" +
                    "testStep=" + testStep +
                    ", action='" + action + '\'' +
                    '}';
        }
    }

    private final List<Step> steps = new ArrayList<Step>();
    private long intervalMillis = 1000L;

	
	public AtlasTestScriptPlayer() {
		// TODO Auto-generated constructor stub
		try {
			System.out.println("loading "+scriptFilename);
			load(scriptFilename);
			System.out.println("subscribing to operator events");
			AtlasOperatorEventPublisher.getInstance().subscribe(this);
			System.out.println("waiting for signon");
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
	}

	 public void setIntervalMillis(long intervalMillis) {
	        if (intervalMillis <= 0) {
	            throw new IllegalArgumentException("Interval must be greater than zero");
	        }

	        this.intervalMillis = intervalMillis;
	 }
	
	 public long getIntervalMillis() {
	        return intervalMillis;
	    }

	    public Step[] getSteps() {
	        return steps.toArray(new Step[steps.size()]);
	    }

	    public void load(String filename) throws IOException {
	        steps.clear();

	        //BufferedReader reader = new BufferedReader(new FileReader(filename));
	        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream4690(filename),StandardCharsets.UTF_8));

	        try {
	            String line;

	            while ((line = reader.readLine()) != null) {
	                line = line.trim();

	                if (line.length() == 0) {
	                    continue;
	                }

	                Step step = parseLine(line);
	               
	                if (step != null) {
	                	if (step.getTestStep()>topStep) {
	 	                	topStep=step.getTestStep();
	 	                }
	                    steps.add(step);
	                }
	            }
	        } finally {
	            reader.close();
	        }
	    }

	    private Step parseLine(String line) {
	        String[] parts = splitStepLine(line);

	        if (parts.length < 1) {
	            return null;
	        }

	        String stepText = stripQuotes(parts[0]).trim();

	        if (stepText.length() == 0) {
	            return null;
	        }

	        int testStep = Integer.parseInt(stepText);

	        String action = "";

	        if (parts.length > 1) {
	            action = stripQuotes(parts[1]).trim();
	        }

	        return new Step(testStep, action);
	    }

	    private String[] splitStepLine(String line) {
	        List<String> parts = new ArrayList<String>();

	        StringBuffer current = new StringBuffer();
	        boolean inQuotes = false;

	        for (int i = 0; i < line.length(); i++) {
	            char ch = line.charAt(i);

	            if (ch == '"') {
	                inQuotes = !inQuotes;
	                current.append(ch);
	            } else if (ch == ',' && !inQuotes) {
	                parts.add(current.toString());
	                current.setLength(0);
	            } else {
	                current.append(ch);
	            }
	        }

	        parts.add(current.toString());

	        return parts.toArray(new String[parts.size()]);
	    }

	    private String stripQuotes(String value) {
	        if (value == null) {
	            return "";
	        }

	        value = value.trim();

	        if (value.length() >= 2 &&
	                value.charAt(0) == '"' &&
	                value.charAt(value.length() - 1) == '"') {
	            return value.substring(1, value.length() - 1);
	        }

	        return value;
	    }

	    public void runSteps() throws InterruptedException {
	        Step[] stepArray = getSteps();
	     while (true) {
	        for (int s=0;s<=topStep;s++) {
	        	System.out.println("step:"+s);
	        	
	        	 for (int i = 0; i < stepArray.length; i++) {
	 	            Step step = stepArray[i];
	 	            if (step.getTestStep()==s) {
	 		            System.out.println("Step "+s+"\nstep:"+step.getTestStep()+"\naction:"+step.getAction());

	 		            if (step.hasAction()) {
	 		                sendAction(step.getAction());
	 		            }
	 		            break;
	 	            }
	        	 }
                Thread.sleep(intervalMillis);
            
	        }
	     }
	    }

	    protected void sendAction(String actionText) {
	        // Replace this with your actual action-sending code.
	        System.out.println("Sending action: " + actionText);
	        SureviewState cState=null;
	        cState=SureviewApplication.getInstance().getSureviewState();
	        InputSequences iSeqs=cState.getInputSequences();
	        iSeqs.postKeySequence(actionText);
	    }
	
	
	
	
	
	@Override
	public void update(Object o) {
		// TODO Auto-generated method stub
		
	}
	
	 public void signOn(OperatorEvent event) {
		    System.out.println("signOn called... starting");

		    if (scriptRunning) {
		        System.out.println("Test script is already running.");
		        return;
		    }

		    scriptRunning = true;

		    scriptThread = new Thread(new Runnable() {
		        public void run() {
		            try {
		                runSteps();
		            } catch (InterruptedException e) {
		                System.out.println("Test script interrupted.");
		                Thread.currentThread().interrupt();
		            } catch (Exception e) {
		                e.printStackTrace();
		            } finally {
		                scriptRunning = false;
		                scriptThread = null;
		                System.out.println("Test script finished.");
		            }
		        }
		    });

		    scriptThread.setName("AtlasTestScriptPlayerThread");
		    scriptThread.start();
	
	 }
	 public synchronized void signOff(OperatorEvent event) {
		    if (scriptThread != null) {
		        scriptThread.interrupt();
		    }
		}

}
