package com.genesys.codesamples.psdk;

import java.util.EventObject;

import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;

public abstract class StatServerInfoEvent extends EventObject {
	/**
	 * Keep the compiler happy
	 */
	private static final long serialVersionUID = -1L;
	
	/**
	 * Constructor
	 * @param source
	 */
	public StatServerInfoEvent(Object source) {
		super(source);
	}
	
	/**
	 * The stat event handler
	 */
	public abstract void onStatInfoEvent(EventInfo eventInfo);
}
