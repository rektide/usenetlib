/*******************************************************************************
 * Copyright (c) 2010 Gijs de Vries aka Janoz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gijs de Vries aka Janoz - initial API and implementation
 ******************************************************************************/
package com.janoz.usenet.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Throttler {

	private static final Log LOG = LogFactory.getLog(Throttler.class);
	
	private long interval;
	private long nextActionNotBefore = 0;
	
	public Throttler(long interval) {
		this.interval = interval;
	}
	
	public synchronized void throttle() {
		long wait = nextActionNotBefore - System.currentTimeMillis();
		setThrottleForNextAction();
		if (wait > 0) {
			try {
				LOG.debug("Waiting for " + wait +"ms.");
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				/* Left Blank */
			}
		}
	}
	public synchronized void throttleBig() {
		nextActionNotBefore += interval;
		throttle();
	}

	
	public synchronized void setThrottleForNextAction() {
		nextActionNotBefore = System.currentTimeMillis() + interval;
	}


	public synchronized long getNextActionNotBefore() {
		return nextActionNotBefore;
	}

}
