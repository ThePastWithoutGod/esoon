package com.genesys.codesamples.psdk;

import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.configuration.protocol.ConfServerProtocol;

public class ConfigServer extends Server {

	public ConfigServer(ILogger log_, CfgConnInfo cfgConnInfo, CfgApplication cfgApplication, ConfServerProtocol configProtocol, Endpoint configEndpoint)
    {
        try
        {
            log = log_.createChildLogger("ConfigServer");
            log.debug("Constructor - entry");
            Initialize(cfgConnInfo, cfgApplication, configProtocol, configEndpoint);
        }
        catch (Exception e)
        {
        	log.error("Constructor", e);
        }

        log.debug("Constructor - exit");
    }
	
	@Override
	public void Start() {
		log.debug("Start - entry");
        Connect();
        log.debug("Start - exit");
	}

	@Override
	public void Stop() {
		log.debug("Stop - entry");
        Disconnect();
        log.debug("Stop - exit");
	}

}
