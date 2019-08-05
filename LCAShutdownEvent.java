package com.genesys.codesamples.psdk;

import java.util.EventObject;

public abstract class LCAShutdownEvent extends EventObject {

	/**
	 * Keep the compiler happy
	 */
	private static final long serialVersionUID = -1L;
	
	/**
	 * Constructor
	 * @param source
	 */
	public LCAShutdownEvent(Object source) {
		super(source);
	}
	
	/**
	 * The shutdown handler
	 */
	public abstract void onShutdown();
}
