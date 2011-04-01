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
package com.janoz.usenet.searchers.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.janoz.usenet.SearchException;
import com.janoz.usenet.model.LazyNZB;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.searchers.NzbsOrgConnector;
import com.janoz.usenet.searchers.categories.NzbsOrgsCategory;
import com.janoz.usenet.suppliers.impl.UrlBasedSupplier;
import com.janoz.usenet.support.Throttler;
import com.janoz.usenet.support.Util;


public class NzbsOrgConnectorImpl implements NzbsOrgConnector{

	private static final Log LOG = LogFactory.getLog(NzbsOrgConnectorImpl.class);
	
	private static final int BUFFSIZE = 1024;
	private static final int RETRY_ATTEMPTS = 3;
	private static final Throttler THROTTLER = new Throttler(2000);

	private Object builderSemaphore = new Object();
	
	private String userId;
	private String passwordHash;
	private int retention=0;


	
	public void setUserId(String userId) {
		this.userId = userId;
	}



	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}



	public void setRetention(int retention) {
		this.retention = retention;
	}

	private List<NZB> fetchFeed(Collection<NameValuePair> params) throws SearchException, DOMException {
		THROTTLER.throttle();
		Document document = null;
		try{
			List<NameValuePair> qparams = new ArrayList<NameValuePair>();
			qparams.addAll(params);
			qparams.add(new BasicNameValuePair("dl", "1"));
			URI uri = getUri("rss.php", qparams);
			synchronized (builderSemaphore) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				DocumentBuilder builder = factory.newDocumentBuilder();
				int attempts = RETRY_ATTEMPTS;
				boolean retry;
				do try {
					retry = false;
					document = builder.parse(uri.toString());
				} catch (IOException ioe) {
					if (attempts == 0 || !ioe.getMessage().startsWith("Server returned HTTP response code: 503")) {
						throw ioe;
					} else {
						attempts--;
						retry = true;
						THROTTLER.throttleBig();
					}
				} while (retry);
			}
		} catch (IOException ioe) {
			throw new SearchException("Error connecting to Nzbs.org.",ioe);
		} catch (SAXException se) {
			throw new SearchException("Error parsing rss from Nzbs.org.",se);
		} catch (ParserConfigurationException pce) {
			throw new SearchException("Error configuring XML parser.",pce);
		} catch (URISyntaxException e) {
			throw new SearchException("Error parsing URI.",e);
		}
		Node rss = document.getFirstChild();
		if (!"rss".equalsIgnoreCase(rss.getNodeName())){
			throw new SearchException("Result was not RSS but " + rss.getNodeName());
		}
		Node channel = rss.getFirstChild();
		while (channel != null && "#text".equals(channel.getNodeName())) {
			channel = channel.getNextSibling();
		}
		NodeList list = channel.getChildNodes();
		DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.CANADA);
		List<NZB> result = new ArrayList<NZB>();
		UrlBasedSupplier supplier = new UrlBasedSupplier();
		supplier.setThrottler(THROTTLER);
		for (int i=0; i < list.getLength();i++) {
			Node n= list.item(i);
			if ("item".equals(n.getNodeName())) {
				LazyNZB nzb = new LazyNZB("tmpName",supplier);
				try {
					for (int j=0; j< n.getChildNodes().getLength(); j++) {
						Node n2 = n.getChildNodes().item(j);
						if ("title".equalsIgnoreCase(n2.getNodeName())){
							nzb.setName(n2.getTextContent());
							nzb.setFilename(Util.saveFileName(n2.getTextContent()) + ".nzb");
						}
						if ("pubdate".equalsIgnoreCase(n2.getNodeName())){
							nzb.setPostDate(df.parse(n2.getTextContent()));
						}
						if ("link".equalsIgnoreCase(n2.getNodeName())){
							nzb.setUrl(n2.getTextContent());
						}
					}
					result.add(nzb);
				} catch (ParseException e) {
					LOG.info("Skipping " +nzb.getName()+" because of date error.",e);
				}
			}
		}
		THROTTLER.setThrottleForNextAction();
		return result;
	}

	@Override
	public List<NZB> search(String query, NzbsOrgsCategory category)
			throws SearchException {
		Collection<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("action", "search"));
		params.add(new BasicNameValuePair("q", query));
		if (retention > 0) {
			params.add(new BasicNameValuePair("age",""+retention));
		}
		if (category != null) {
			params.add(new BasicNameValuePair("catid",category.getValue()));
		}
		return fetchFeed(params);
	}

	private static String VALID_STRING =   "NZB not found";
	private static String INVALID_STRING = "Not logged in";

	public void validate() throws SearchException {
		THROTTLER.throttle();
		InputStreamReader isr = null;
		try{
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("action", "getnzb"));
			params.add(new BasicNameValuePair("nzbid", ""+0));
			URI uri = getUri("index.php", params);
			isr = new InputStreamReader(
					(InputStream)uri.toURL().getContent());

		    char[] buffer = new char[BUFFSIZE];
		    int numRead;
		    int trueCount = 0;
		    int falseCount = 0;
		    while((numRead = isr.read(buffer)) > 0) {
		        for (int c = 0; c < numRead; c++) {
		            if (buffer[c] == VALID_STRING.charAt(trueCount)){
		                trueCount++;
		            } else {
		            	trueCount = 0;
		            }
		            if (trueCount == VALID_STRING.length()) return;
		            if (buffer[c] == INVALID_STRING.charAt(falseCount)){
		                falseCount++;
		            } else {
		            	falseCount = 0;
		            }
		            if (falseCount == INVALID_STRING.length()) {
		            	throw new SearchException("Invalid credentials.");
		            }
		        }
		    }
		    throw new SearchException("Unable to determine validity.");
		} catch (IOException ioe) {
			throw new SearchException("Error connecting to Nzbs.org.",ioe);
		} catch (URISyntaxException e) {
			throw new SearchException("Error parsing URI.",e);
		} finally {
			THROTTLER.setThrottleForNextAction();
			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {
					/* dan niet */
				}
			}
		}
	}

	/**
	 * 
	 * @param action
	 * @param params Parameters WILL BE MODIFIED
	 * @return
	 * @throws URISyntaxException
	 */
	private URI getUri(String action, List<NameValuePair> params) throws URISyntaxException {
		params.add(new BasicNameValuePair("i", userId));
		params.add(new BasicNameValuePair("h", passwordHash));
		return URIUtils.createURI("http", "nzbs.org", 80, action, 
				URLEncodedUtils.format(params, "UTF-8"), null);
	}
}
