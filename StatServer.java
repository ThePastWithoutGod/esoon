package com.genesys.codesamples.psdk;

import java.util.*;

import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.reporting.protocol.StatServerProtocol;
import com.genesyslab.platform.reporting.protocol.statserver.*;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;
import com.genesyslab.platform.reporting.protocol.statserver.requests.*;

public class StatServer extends Server {

	private String employeeId;
    private String tenant;
    private int refNo = 1;
    private List<StatServerInfoEvent> listeners = new ArrayList<StatServerInfoEvent>();
    private StatServerChannelListenerHandler statServerChannelListenerHandler = new StatServerChannelListenerHandler();
    private StatServerMessageHandler statServerMessageHandler = new StatServerMessageHandler();
    
    public StatServer(final ILogger log_, CfgConnInfo cfgConnInfo, CfgApplication cfgApplication, String applicationName, String employeeId) {
        try { 
            log = log_.createChildLogger("Stats");
            log.debug("Constructor - entry");
            this.employeeId = employeeId;
            Collection<CfgTenant> cfgTenants = cfgApplication.getTenants();
            for (CfgTenant cfgTenant : cfgTenants) {
                this.tenant = cfgTenant.getName();
            }
           
            Endpoint endpoint = new Endpoint(cfgApplication.getServerInfo().getHost().getIPaddress(), 
            		Integer.parseInt(cfgApplication.getServerInfo().getPort()));
            StatServerProtocol statProtocol = new StatServerProtocol(endpoint);
            statProtocol.setClientName(applicationName);
            statProtocol.setInvoker(new SwingInvoker());
            Initialize(cfgConnInfo, cfgApplication, statProtocol, null);
        }
        catch (Exception e) {
            log.error("Constructor", e);
        }

        log.debug("Constructor - exit");
    }
    
	@Override
	public void Start() {
		log.debug("Start - entry");
		protocol.removeChannelListener(statServerChannelListenerHandler);
		protocol.addChannelListener(statServerChannelListenerHandler);
		protocol.setMessageHandler(statServerMessageHandler);
        Connect();
        log.debug("Start - exit");
	}

	@Override
	public void Stop() {
		log.debug("Stop - entry");
		protocol.removeChannelListener(statServerChannelListenerHandler);
        StopAHT();
        Disconnect();
        log.debug("Stop - exit");
	}
	
	public void addListener(StatServerInfoEvent toAdd) {
        listeners.add(toAdd);
    }
	
	public void removeListener(StatServerInfoEvent toAdd) {
        listeners.remove(toAdd);
    }
	
	private void StartAHT() {
        log.info("StartAHT - entry");

        if (protocol.getState() == ChannelState.Opened) {
            RequestOpenStatistic statRequest = RequestOpenStatistic.create();

            statRequest.setStatisticObject(StatisticObject.create());
            statRequest.getStatisticObject().setObjectId(employeeId);
            statRequest.getStatisticObject().setObjectType(StatisticObjectType.Agent);
            statRequest.getStatisticObject().setTenantName(tenant);

            statRequest.setStatisticMetric(StatisticMetric.create());
            statRequest.getStatisticMetric().setStatisticType("AverageHandlingTime");

            statRequest.setNotification(Notification.create());
            statRequest.getNotification().setMode(NotificationMode.Periodical);
            statRequest.getNotification().setFrequency(15);

            statRequest.setReferenceId(refNo);
            log.debug(statRequest.toString());
            
            try {
				protocol.send(statRequest);
			} catch (ProtocolException | IllegalStateException e) {
				log.error("StartAHT", e);
			}
        }

        log.info("StartAHT - exit");
    }
	
	private void StopAHT() {
        log.info("StopAHT - entry");

        if (protocol.getState() == ChannelState.Opened) {
            RequestCloseStatistic request = RequestCloseStatistic.create();
            request.setStatisticId(refNo);
            try {
				protocol.send(request);
			} catch (ProtocolException | IllegalStateException e) {
				log.error("StopAHT", e);
			}
        }

        log.info("StopAHT - exit");
    }
	
	private class StatServerChannelListenerHandler implements ChannelListener {

		@Override
		public void onChannelClosed(ChannelClosedEvent arg0) {
			log.info("onChannelClosed");
		}

		@Override
		public void onChannelError(ChannelErrorEvent arg0) {
			log.info("onChannelError");
		}

		@Override
		public void onChannelOpened(EventObject arg0) {
			log.info("onChannelOpened");
			StartAHT();
		}
    }
    
    private class StatServerMessageHandler implements MessageHandler {

		@Override
		public void onMessage(Message message) {
			log.info("OnMessageReceived: " + message.toString());

	        if (message instanceof EventInfo) {
	            EventInfo info = (EventInfo)message;
	            
	            for (StatServerInfoEvent hl : listeners)
            		hl.onStatInfoEvent(info);
	        }
		}
    }
}
