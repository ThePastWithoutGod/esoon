package com.genesys.codesamples.psdk;

import java.util.EventObject;

import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.applicationblocks.warmstandby.*;
import com.genesyslab.platform.commons.connection.configuration.ClientADDPOptions.AddpTraceMode;
import com.genesyslab.platform.commons.connection.configuration.PropertyConfiguration;
import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.configuration.protocol.types.*;

public abstract class Server {
	private WarmStandbyService warmStandbyService;
    protected ClientChannel protocol;
    protected ILogger log;

    public EventObject Connected;
    public EventObject Disconnected;
    public EventObject Received;

    public abstract void Start();
    public abstract void Stop();

    protected void Initialize(CfgConnInfo cfgConnInfo, CfgApplication cfgApplication, ClientChannel protocol, Endpoint endpoint) {
        this.protocol = protocol;

        PropertyConfiguration addpConfig = new PropertyConfiguration();
        addpConfig.setUseAddp(cfgConnInfo.getConnProtocol() == "addp");
        addpConfig.setAddpServerTimeout(cfgConnInfo.getTimoutRemote());
        addpConfig.setAddpClientTimeout(cfgConnInfo.getTimoutLocal());
        CfgTraceMode cfgTraceMode = cfgConnInfo.getMode();

        if (cfgTraceMode != null) {
        	if (cfgTraceMode == CfgTraceMode.CFGTMBoth)
        		addpConfig.setAddpTraceMode(AddpTraceMode.Both);
        	else if (cfgTraceMode == CfgTraceMode.CFGTMLocal)
        		addpConfig.setAddpTraceMode(AddpTraceMode.Local);
        	else if (cfgTraceMode == CfgTraceMode.CFGTMRemote)
        		addpConfig.setAddpTraceMode(AddpTraceMode.Remote);
        }

        // set up the standby connection
        if (endpoint == null) {
            endpoint = new Endpoint(cfgApplication.getServerInfo().getHost().getIPaddress(), 
            		Integer.parseInt(cfgApplication.getServerInfo().getPort()), addpConfig);
            protocol.setEndpoint(endpoint);
        }

        Endpoint backupEndpoint;

        if (cfgApplication.getServerInfo().getBackupServer() != null) {
            backupEndpoint = new Endpoint(cfgApplication.getServerInfo().getBackupServer().getServerInfo().getHost().getIPaddress(), 
            		Integer.parseInt(cfgApplication.getServerInfo().getBackupServer().getServerInfo().getPort()), addpConfig);
        }
        else {
            // use the primary as the "backup"
            backupEndpoint = new Endpoint(cfgApplication.getServerInfo().getHost().getIPaddress(), 
            		Integer.parseInt(cfgApplication.getServerInfo().getPort()), addpConfig);
        }

        WarmStandbyConfiguration standbyWarmStandbyConfig = new WarmStandbyConfiguration(endpoint, backupEndpoint);
        standbyWarmStandbyConfig.setTimeout(cfgApplication.getServerInfo().getTimeout() * 1000);
        standbyWarmStandbyConfig.setAttempts((short)(int)cfgApplication.getServerInfo().getAttempts());
        warmStandbyService = new WarmStandbyService(protocol);
        warmStandbyService.applyConfiguration(standbyWarmStandbyConfig);
    }

    /// <summary>
    /// Set up the event handlers and connect to a Genesys server
    /// </summary>
    protected void Connect() {
    	try {
    		
	        if (protocol != null) {
	            if (warmStandbyService != null && warmStandbyService.getState() == WarmStandbyState.OFF)
	                warmStandbyService.start();
	
	            if (protocol.getState() == ChannelState.Closed)
	            	protocol.beginOpen();
	        } 
    	}
    	catch (ProtocolException e) {
    		log.error("Connect", e);
        }
    }

	/// <summary>
    /// Close the connection to a Genesys server
    /// </summary>
    protected void Disconnect() {
        if (protocol != null) {
            if (warmStandbyService != null && warmStandbyService.getState() != WarmStandbyState.OFF)
                warmStandbyService.stop();

            if (protocol.getState() != ChannelState.Closed)
            	protocol.beginClose();
        }
    }
}
