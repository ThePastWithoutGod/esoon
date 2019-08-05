package com.genesys.codesamples.psdk;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.genesyslab.platform.applicationblocks.com.IConfService;
import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.applicationblocks.com.queries.*;
import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.management.protocol.*;
import com.genesyslab.platform.management.protocol.localcontrolagent.events.*;
import com.genesyslab.platform.management.protocol.localcontrolagent.responses.ResponseExecutionModeChanged;

public class LCAServer extends Server {

	private LocalControlAgentProtocol lcaProtocol;
	private List<LCAShutdownEvent> listeners = new ArrayList<LCAShutdownEvent>();
	private LCAMessageHandler lcaMessageHandler = new LCAMessageHandler();
	
	public LCAServer(final ILogger log_, IConfService confService, String applicationName) {
        try
        {
            log = log_.createChildLogger("LCAServer");
            log.debug("Constructor - entry");

            CfgHost thisHost = null;
            CfgHostQuery hostQuery = new CfgHostQuery(confService);
            
            InetAddress[] addressList = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress ip : addressList) {
            	
            	hostQuery.setName(ip.getHostAddress());
                CfgHost cfgHost = confService.retrieveObject(hostQuery);

                if (cfgHost != null && ip.getHostAddress().equals(cfgHost.getIPaddress())) {
                    thisHost = cfgHost;
                    break;
                }
            }
            
            if (thisHost != null) {
                Endpoint lcaEndpoint = new Endpoint("localhost", Integer.parseInt(thisHost.getLCAPort()));
                lcaProtocol = new LocalControlAgentProtocol(lcaEndpoint);
                lcaProtocol.setClientName(applicationName);
                lcaProtocol.setExecutionMode(ApplicationExecutionMode.Backup);
                lcaProtocol.setControlStatus(ApplicationStatus.Running.asInteger());
                lcaProtocol.setInvoker(new SwingInvoker());
                protocol = lcaProtocol;
            }
        }
        catch (Exception e) {
            log.error("Constructor", e);
        }

        log.debug("Constructor - exit");
    }
	
	@Override
	public void Start() {
		log.debug("Start - entry");
		
		if (protocol != null) {
			protocol.setMessageHandler(lcaMessageHandler);
	        Connect();
		}
		
		log.debug("Start - exit");
	}

	@Override
	public void Stop() {
		log.debug("Stop - entry");
		if (protocol != null) {
			Disconnect();
		}
		
        log.debug("Stop - exit");
	}
	
	public void addListener(LCAShutdownEvent toAdd) {
        listeners.add(toAdd);
    }
	
	public void removeListener(LCAShutdownEvent toRemove) {
        listeners.remove(toRemove);
    }
	
	private class LCAMessageHandler implements MessageHandler {

		@Override
		public void onMessage(Message message) {
			log.debug("onMessage: " + message);
			
			if (message instanceof EventChangeExecutionMode) {
				EventChangeExecutionMode eventChangeExecutionMode = (EventChangeExecutionMode)message;

	            ApplicationExecutionMode mode = eventChangeExecutionMode.getExecutionMode();
	            lcaProtocol.setExecutionMode(mode);
	            ResponseExecutionModeChanged response = ResponseExecutionModeChanged.create(mode);
	            
	            try {
					protocol.send(response);
				} catch (ProtocolException | IllegalStateException e) {
					log.error("onMessage", e);
				}
	            
	            if (mode == ApplicationExecutionMode.Exiting) {
	            	for (LCAShutdownEvent hl : listeners)
	            		hl.onShutdown();
                }
	        }
			else if (message instanceof EventSuspendApplication) {
				for (LCAShutdownEvent hl : listeners)
		            hl.onShutdown();
			}
		}
	}
}
