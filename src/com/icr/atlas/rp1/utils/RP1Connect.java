package com.icr.atlas.rp1.utils;

import java.util.ArrayDeque;
import java.util.Deque;

import org.json.JSONObject;

import com.ibm.retail.AEF.event.AEFPropertyChangeEvent;
import com.icr.atlas.ATLASGuiExtend;
import com.icr.atlas.eventPublishers.AtlasAEFPropertyChangeEventPublisher;
import com.icr.atlas.eventPublishers.AtlasSubscriber;

public class RP1Connect  {
	
	public static final long POLL_INTERVAL_MS=30L * 1000L;
	public final int terminal;
	private Thread pollThread=null;
	private volatile boolean running=false;

	public RP1Connect() {
		// TODO Auto-generated constructor stub
		terminal=Integer.parseInt(ATLASGuiExtend.getInstance().getTermId()); 
		
		start();
	}
	
	public synchronized void start() {

        if (pollThread != null && pollThread.isAlive()) {
            return;
        }
        running = true;
        pollThread =new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (running) {
                                try {
                                    processCycle();
                                } catch (Exception e) {
                                    System.out.println("RP1Connect poll error: "+ e);
                                }
                                if (!running) {
                                    break;
                                }
                                try {
                                    Thread.sleep(POLL_INTERVAL_MS);
                                } catch (InterruptedException e) {
                                    if (!running) {
                                        break;
                                    }
                                }
                            }
                        }
                    });

        pollThread.setName("RP1ConnectPollThread");
        pollThread.setDaemon(true);
        pollThread.start();
    }


    private void processCycle() {

        try {
            /*
             * Build request:
             *
             * {
             *    "terminal":626
             * }
             */
        	System.out.println("RP1Connect cycle!");
            JSONObject request =new JSONObject();
            request.put("terminal",terminal);
            ApiResponse response =RP1RelayClient.getInstance().postJson("/api/term_poll.php",request.toString(),true); //true for skipQueue
            if (response == null) {
                System.out.println("RP1Connect: null response");
                return;
            }
            if (!response.isSuccess()) {
                System.out.println("RP1Connect poll failed. Status="+ response.getStatus()+ " body="+ response.getBody());
                return;
            }

            String body =response.getBody();
            if (body == null ||body.trim().length() == 0) {
                System.out.println("RP1Connect: empty response");
                return;
            }
            System.out.println("response:"+body);

            JSONObject result = new JSONObject(body);
            boolean monitorRequested =result.optBoolean("monitorRequested",false);
            System.out.println("monitorRequested:"+monitorRequested);
            /*
             * Monitoring requested.
             */
            if (monitorRequested) {
                if (!RP1TerminalMonitor.getInstance().isMonitoring()) {
                    System.out.println("RP1Connect: starting terminal monitor");
                    RP1TerminalMonitor.getInstance().startMonitoring(terminal,RP1TerminalMonitor.ImageFormat.JPG,800,0,0.65f);
                } else {
                    /*
                     * Since your monitor automatically
                     * expires after 5 minutes, renew the
                     * expiration every time RP1 says
                     * monitoring is still requested.
                     */
                    //RP1TerminalMonitor.extendMonitoring();
                	//let's not automatically extend, if the flag is still set after init, it will restart
                }
            } else {
                /*
                 * RP1 says monitoring is no longer
                 * requested.
                 */
                if (RP1TerminalMonitor.getInstance().isMonitoring()) {
                    System.out.println("RP1Connect: stopping terminal monitor");
                    RP1TerminalMonitor.getInstance().stopMonitoring();
                }
            }
        } catch (Exception e) {
            /*
             * Do NOT stop the polling thread because
             * of one communications or JSON failure.
             */
            System.out.println("RP1Connect processCycle error: "+ e);
        }
    }


    public synchronized void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
        /*
         * Optional:
         *
         * If RP1Connect itself is stopped, also
         * stop screen monitoring.
         */
        if (RP1TerminalMonitor.getInstance().isMonitoring()) {
            RP1TerminalMonitor.getInstance().stopMonitoring();
        }
    }


    public boolean isRunning() {
        return running;
    }


    public int getTerminal() {
        return terminal;
    }

	
}
