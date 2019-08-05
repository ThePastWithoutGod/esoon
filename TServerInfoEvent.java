package com.genesys.codesamples.psdk;

import java.util.EventObject;

import com.genesyslab.platform.voice.protocol.tserver.TServerEvent;

public abstract class TServerInfoEvent extends EventObject {
	/**
	 * Keep the compiler happy
	 */
	private static final long serialVersionUID = -1L;
	
	/**
	 * Constructor
	 * @param source
	 */
	public TServerInfoEvent(Object source) {
		super(source);
	}
	
	/**
	 * The t-server event handler
	 */
	public abstract void onTServerEvent(TServerEvent tserverEvent);
}
