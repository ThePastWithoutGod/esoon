package com.genesys.codesamples.psdk;

import java.util.*;

import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.voice.protocol.*;
import com.genesyslab.platform.voice.protocol.tserver.*;
import com.genesyslab.platform.voice.protocol.tserver.events.*;

public class TServer extends Server {

	private ConnectionId connID;
	private ConnectionId consultConnID;
    private TServerMessageHandler tServerMessageHandler = new TServerMessageHandler();
    private List<TServerInfoEvent> listeners = new ArrayList<TServerInfoEvent>();
    
    public TServer(ILogger log_, CfgConnInfo cfgConnInfo, CfgApplication cfgApplication, String applicationName) {
        try {
            log = log_.createChildLogger("TServer");
            log.debug("Constructor - entry");
            
            Endpoint endpoint = new Endpoint(cfgApplication.getServerInfo().getHost().getIPaddress(), 
            		Integer.parseInt(cfgApplication.getServerInfo().getPort()));
            TServerProtocol voiceProtocol = new TServerProtocol(endpoint);
            voiceProtocol.setClientName(applicationName);
            voiceProtocol.setInvoker(new SwingInvoker());
            Initialize(cfgConnInfo, cfgApplication, voiceProtocol, null);
            
            voiceProtocol.addChannelListener(new ChannelListener() {

    			public void onChannelClosed(ChannelClosedEvent arg0) {
    				log.error("Lost connection");
    			}

    			public void onChannelError(ChannelErrorEvent arg0) {
    			}

    			public void onChannelOpened(EventObject arg0) {
    			}
    		});
        }
        catch (Exception e) {
            log.error("Constructor", e);
        }

        log.debug("Constructor - exit");
    }
    
    public ConnectionId getConnID() {
    	return connID;
    }
    
    public void setConnID(ConnectionId connID) {
    	this.connID = connID;
    }
    
    public ConnectionId getConsultConnID() {
    	return consultConnID;
    }
    
    public void setConsultConnID(ConnectionId consultConnID) {
    	this.consultConnID = consultConnID;
    }
    
	@Override
	public void Start() {
		log.debug("Start - entry");
		protocol.setMessageHandler(tServerMessageHandler);
        Connect();
        log.debug("Start - exit");
	}

	@Override
	public void Stop() {
		log.debug("Stop - entry");
        Disconnect();
        log.debug("Stop - exit");

	}
	
	public void addListener(TServerInfoEvent toAdd) {
        listeners.add(toAdd);
    }
	
	public void removeListener(TServerInfoEvent toAdd) {
        listeners.remove(toAdd);
    }
	
	private class TServerMessageHandler implements MessageHandler {

		@Override
		public void onMessage(Message message) {
			log.debug("Incoming Message: " + message);

			switch (message.messageId()) {
				case EventRinging.ID:
					EventRinging eventRinging = (EventRinging) message;
					connID = eventRinging.getConnID();
					consultConnID = null;
					break;

				case EventDialing.ID:
					EventDialing eventDialing = (EventDialing)message;
				
					if (eventDialing.getCallType() != CallType.Consult)
                        connID = eventDialing.getConnID();
                    else
                    	consultConnID = eventDialing.getConnID();
					break;
					
				case EventEstablished.ID:
                    EventEstablished eventEstablished = (EventEstablished) message;
                    
                    if (eventEstablished.getCallType() != CallType.Consult)
						connID = eventEstablished.getConnID();
                    else
                    	consultConnID = eventEstablished.getConnID();
                    break;
			}
			
			if (message instanceof TServerEvent) {
				TServerEvent info = (TServerEvent)message;
	            
	            for (TServerInfoEvent hl : listeners)
            		hl.onTServerEvent(info);
	        }
		}
	}
}
