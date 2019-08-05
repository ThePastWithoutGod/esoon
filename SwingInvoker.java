package com.genesys.codesamples.psdk;

import javax.swing.SwingUtilities;
import com.genesyslab.platform.commons.threading.AsyncInvoker;

public class SwingInvoker implements AsyncInvoker {

	@Override
	public void dispose() {
	}

	@Override
	public void invoke(Runnable target) {
		SwingUtilities.invokeLater(target);
	}

}
