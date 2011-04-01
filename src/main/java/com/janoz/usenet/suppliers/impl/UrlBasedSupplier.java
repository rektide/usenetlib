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
package com.janoz.usenet.suppliers.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.janoz.usenet.LazyInitializationException;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.suppliers.LazyNZBSupplier;
import com.janoz.usenet.support.Throttler;

public class UrlBasedSupplier implements LazyNZBSupplier {
	private static final Log LOG = LogFactory.getLog(UrlBasedSupplier.class);

	private Throttler throttler;
	
	
	
	@Override
	public byte[] getData(NZB nzb) {
		if (nzb.getUrl() == null) {
			throw new LazyInitializationException("Unable to fetch data. URL missing.");
		}
		if (throttler != null) {
			throttler.throttle();
		}
		InputStream is = null;
		try {
			URLConnection connection = new URL(nzb.getUrl()).openConnection();
	    	is = connection.getInputStream();
	    	int contentLength = connection.getContentLength();
			ByteArrayOutputStream baos;
			if (contentLength != -1) {
			    baos = new ByteArrayOutputStream(contentLength);
			} else {
			    baos = new ByteArrayOutputStream(16384); // Pick some appropriate size
			}
			
			byte[] buf = new byte[512];
			while (true) {
			    int len = is.read(buf);
			    if (len == -1) {
			        break;
			    }
			    baos.write(buf, 0, len);
			}
			return baos.toByteArray();
		} catch (MalformedURLException e) {
			LOG.error("Error in URL.", e);
			throw new LazyInitializationException("Error fetching NZB with URL  "+nzb.getUrl() +".", e);
		} catch (IOException e) {
			LOG.error("IO error fetching lazy data.", e);
			throw new LazyInitializationException("IO error fetching NZB with URL  "+nzb.getUrl() +".", e);
		} finally {
			if (throttler != null) {
				throttler.setThrottleForNextAction();
			}
			if (is != null ) {
				try {
					is.close();
				} catch (IOException e) {
					LOG.info("error closing stream.",e);
				}
			}
		}
	}

	public void setThrottler(Throttler throttler) {
		this.throttler = throttler;
	}

}
