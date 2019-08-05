package com.genesys.codesamples.psdk;

import java.util.Date;

import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.commons.protocol.ChannelState;
import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.configuration.protocol.types.CfgAppType;
import com.genesyslab.platform.management.protocol.MessageServerProtocol;
import com.genesyslab.platform.management.protocol.messageserver.*;
import com.genesyslab.platform.management.protocol.messageserver.requests.RequestLogMessage;

public class MessageServer extends Server {

	public MessageServer(ILogger log_, CfgConnInfo cfgConnInfo, CfgApplication cfgApplication, String applicationName, String clientHost, CfgAppType clientType, Integer clientId) {
        try {
            log = log_.createChildLogger("Message");
            log.debug("Constructor - entry");
            
            Endpoint endpoint = new Endpoint(cfgApplication.getServerInfo().getHost().getIPaddress(), 
            		Integer.parseInt(cfgApplication.getServerInfo().getPort()));
            MessageServerProtocol msgProtocol = new MessageServerProtocol(endpoint);
            msgProtocol.setClientName(applicationName);
            msgProtocol.setClientHost(clientHost);
            msgProtocol.setClientType(clientType.asInteger());
            msgProtocol.setClientId(clientId);
            Initialize(cfgConnInfo, cfgApplication, msgProtocol, null);
        }
        catch (Exception e) {
            log.error("Constructor", e);
        }

        log.debug("Constructor - exit");
    }
	
	@Override
	public void Start() {
		log.debug("Start - entry");

        try { 
            Connect();
        }
        catch (Exception e) {
            log.error("Start", e);
        }

        log.debug("Start - exit");

	}

	@Override
	public void Stop() {
		log.debug("Stop - entry");

        try {
            Disconnect();
        }
        catch (Exception e) {
            log.error("Stop", e);
        }

        log.debug("Stop - exit");

	}
	
	/**
	 * Send a message to message server
	 * @param entryId
	 * @param level
	 * @param category
	 * @param entryText
	 */
    public void SendMessage(int entryId, LogLevel level, LogCategory category, String entryText) {
        log.info("SendMessage - entry (" + entryText + ")");

        if (protocol.getState() == ChannelState.Opened) {
            try {
                RequestLogMessage request = RequestLogMessage.create(
                                    entryId,
                                    entryText,
                                    level);

                request.setEntryCategory(category);
                request.setTime(new Date());
                protocol.send(request);
            }
            catch (Exception ex) {
                log.error("SendMessage", ex);
            }
        }

        log.info("SendMessage - exit");
    }
}
