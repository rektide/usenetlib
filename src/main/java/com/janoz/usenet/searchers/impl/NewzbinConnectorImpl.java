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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.janoz.usenet.LazyInitializationException;
import com.janoz.usenet.SearchException;
import com.janoz.usenet.model.LazyNZB;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.searchers.NewzbinConnector;
import com.janoz.usenet.searchers.categories.NewzbinCategory;
import com.janoz.usenet.support.GzipDecompressingEntity;
import com.janoz.usenet.support.LogUtil;
import com.janoz.usenet.support.Util;

/**
 * @author Gijs de Vries
 *
 */
public class NewzbinConnectorImpl implements NewzbinConnector {
	
	private static final Log LOG = LogFactory.getLog(NewzbinConnectorImpl.class);
	private static final int BUFF_SIZE = 1024;
	
	private static final String NEWZBIN_ENCODING = "ISO-8859-1"; 

	private static final int RESULT_OK = 200;
	private static final int RESULT_EMPTY = 204;
	private static final int RESULT_BAD_REQUEST = 400;
	private static final int RESULT_UNAUTHORIZED = 401;
	private static final int RESULT_PAYMENT_REQUIRED = 402;
	private static final int RESULT_NOT_FOUND = 404;
	private static final int RESULT_TO_MANY_REQUESTS = 450;
	private static final int RESULT_INTERNAL_SERVER_ERROR = 500;
	private static final int RESULT_SERVICE_UNAVAILABLE = 503;
	
	private static final long MINUTE = 60 * 1000; // in milisec
		
	private HttpClient client;
	private String serverAddress = "www.newzbin.com";
	private Integer serverPort = 443;
	private String serverProtocol = "https";
	private String username = "";
	private String password = "";
	private int retention;
	private Set<String> defaultNewsgroups;

	private Properties defaultNewzbinQueryProps;

	public NewzbinConnectorImpl() {
		client = GzipDecompressingEntity.addTo(new DefaultHttpClient());
		defaultNewzbinQueryProps = new Properties();
		defaultNewzbinQueryProps.setProperty("fpn","p");
		defaultNewzbinQueryProps.setProperty("sort","ps_edit_date");
		defaultNewzbinQueryProps.setProperty("order","desc");
		defaultNewzbinQueryProps.setProperty("u_completions","1");
	}
	
	
	public void setPassword(String password) {
		this.password = password;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public void setServerPort(Integer serverPort) {
		this.serverPort = serverPort;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setRetention(int retention) {
		this.retention = retention;
	}
	
	public void setNewsgroups(Set<String> newsgroups) {
		this.defaultNewsgroups = newsgroups;
	}
	
	public void setNewzbinQueryProps(Properties newzbinQueryProps) {
		this.defaultNewzbinQueryProps = newzbinQueryProps;
	}

	@Override
	public synchronized void fillNZBByReportId(NZB nzb) throws SearchException{
		if (nzb.getReportId() == null) {
			throw new SearchException("No ReportId. Unable to retrieve data using Newzbin.");
		}
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("reportid", "" + nzb.getReportId()));
		
		HttpResponse response = null;
		try {
			boolean success = true;
			do { 
				HttpPost method = new HttpPost(constructURL("/api/dnzb/"));
				method.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
				response = client.execute(method);
				if (LOG.isDebugEnabled()) {
					LogUtil.logResponseHeader(response);
					Header[] headers;
					headers = response.getHeaders("X-DNZB-RText");
					LOG.debug("X-DNZB-RText : " 
							+ (headers.length == 0?"null":headers[0].getValue()));
					headers = response.getHeaders("X-DNZB-RCode");
					LOG.debug("X-DNZB-RCode : " 
							+ (headers.length == 0?"null":headers[0].getValue()));
				}
				int status = -1;
				try {
					status = Integer.parseInt(
						response.getHeaders("X-DNZB-RCode")[0].getValue());
				} catch (Exception e) {
					LOG.warn("Error getting X-DNZB-RCode.",e);
				}
				switch (status) {
					case RESULT_OK:
						success = true;
						break;
					case RESULT_BAD_REQUEST:
						LOG.error("Bad directNZB request. Specifications "
								+ "might have changed.");
						throw new SearchException("Bad directNZB request. "
								+ "Specifications might have changed. Occured while fetching '"+nzb.getName()+"'("+nzb.getReportId()+").");
					case RESULT_UNAUTHORIZED:
						//Unauthorised, check username/password?
						LOG.error("Newzbin login failed");
						throw new SearchException(
								"Incorrect Newzbin credentials for '"
								+ username + "' while fetching '"+nzb.getName()+"'("+nzb.getReportId()+").");
					case RESULT_PAYMENT_REQUIRED:
						//Payment Required, not Premium
						LOG.error("Payment needed");
						throw new SearchException("No premium account while fetching '"+nzb.getName()+"'("+nzb.getReportId()+").");
					case RESULT_NOT_FOUND:
						//Not Found, data doesn't exist?
						throw new SearchException("'"+nzb.getName()+"'("+nzb.getReportId()+")"
								+ " doesn't exsist.");
					case RESULT_TO_MANY_REQUESTS:
						success = false;
						response.getEntity().consumeContent();
						try {
							LOG.info("To many request. Waiting to try "
									+ "again....");
							Thread.sleep(MINUTE);
						} catch (InterruptedException e) {
							LOG.info("Waiting interupted");
						}
						break;
					case RESULT_INTERNAL_SERVER_ERROR:
						throw new SearchException("Internal newzbin "
								+ "server error while fetching '"+nzb.getName()+"'("+nzb.getReportId()+").");
					case RESULT_SERVICE_UNAVAILABLE:
						throw new SearchException("Newzbin.com currently "
								+ "down while fetching '"+nzb.getName()+"'("+nzb.getReportId()+")");
					default:
						//Service Unavailable, site is currently down 
						throw new SearchException("Got unknown returncode "
								+ status + " from newzbin.com for '"+nzb.getName()+"'("+nzb.getReportId()+").");
				}
				
			} while (!success);
			if (LOG.isInfoEnabled()) {
				LOG.info("fetching content");
			}
			String reportName = 
				response.getHeaders("X-DNZB-Name")[0].getValue();
			nzb.setName(reportName);
			nzb.setFilename(
					Util.saveFileName(reportName) + ".nzb");
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			InputStream input = response.getEntity().getContent();
	        byte[] buffer = new byte[BUFF_SIZE];
	        int n;
	        while (-1 != (n = input.read(buffer))) {
	        	ba.write(buffer, 0, n);
	        }
			nzb.setData(ba.toByteArray());
		} catch (IOException e) {
			throw new SearchException("IO error requesting NZB data for '"+nzb.getName()+"'("+nzb.getReportId()+").", e);
		} finally {
			if (response != null) {
				try {
					response.getEntity().consumeContent();
				} catch (IOException e) {
					LOG.warn("Error while cleaning up request.",e);
				}
			}
		}
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized List<NZB> search(String query,
			NewzbinCategory category,
			Integer minSize, Integer maxSize, 
			Set<String> newsgroupsParam, Properties newzbinQueryPropsParam) 
			throws SearchException {
		//Fill with defaults
		Set<String> newsgroups;
		if (newsgroupsParam == null) {
			newsgroups = this.defaultNewsgroups;
		} else {
			newsgroups = newsgroupsParam;
		}
		Properties newzbinQueryProps;
		if (newzbinQueryPropsParam == null) {
			newzbinQueryProps = this.defaultNewzbinQueryProps;
		} else {
			Properties tmpProps = new Properties(this.defaultNewzbinQueryProps);
			tmpProps.putAll(newzbinQueryPropsParam);
			newzbinQueryProps = tmpProps;
		}
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("retention", ""+retention));
		if (newsgroups != null && !newsgroups.isEmpty()) {
			params.add(new BasicNameValuePair("group",Util.implode(newsgroups, "+")));
		}
		if (category != null) {
			params.add(new BasicNameValuePair("category", ""+category.getValue()));
		}
		if (minSize != null) {
			params.add(new BasicNameValuePair("u_post_larger_than",""+minSize));
		}
		if (maxSize != null) {
			params.add(new BasicNameValuePair("u_post_smaller_than",""+maxSize));
		}
		params.add(new BasicNameValuePair("query",query));
		Enumeration<String> keys = (Enumeration<String>) newzbinQueryProps.propertyNames();
		String label = null;
		String value = null;
		while (keys.hasMoreElements()) {
			label = keys.nextElement();
			value = newzbinQueryProps.getProperty(label); 
			if (value != null && value.trim().length() > 0) {
				params.add(new BasicNameValuePair(label, value));
			}
		}
		HttpResponse response = null;
		BufferedReader br = null;
		try {
			HttpPost method = new HttpPost(constructURL("/api/reportfind/"));
			method.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			response = client.execute(method);
			if (LOG.isTraceEnabled()) {
				LogUtil.logResponseHeader(response);
			}

			int status = response.getStatusLine().getStatusCode();
			switch (status) {
			
				case RESULT_OK:
					break;
				case RESULT_EMPTY:
					return Collections.emptyList();
				case RESULT_UNAUTHORIZED:
					//Unauthorised, check username/password?
					LOG.error("Newzbin login failed");
					throw new SearchException(
							"Incorrect Newzbin credentials for '"
							+ username + "' while searching.");
				case RESULT_PAYMENT_REQUIRED:
					//Payment Required, not Premium
					LOG.error("Payment needed");
					throw new SearchException("No premium account while searching.");
				case RESULT_INTERNAL_SERVER_ERROR:
					throw new SearchException("Internal newzbin "
							+ "server error while searching.");
				case RESULT_SERVICE_UNAVAILABLE:
					throw new SearchException("Newzbin.com currently "
							+ "down while searching.");
				default:
					//Service Unavailable, site is currently down 
					throw new SearchException("Got unknown returncode "
							+ status + " from newzbin.com while searching.");
			}
			//we got a result
			List<NZB> result = new ArrayList<NZB>();
			InputStream is = response.getEntity().getContent();
			
			
			//is = LogUtil.dumpStreamAndReOffer(is);
			
			
			
			br = new BufferedReader(
					new InputStreamReader(is,NEWZBIN_ENCODING));
			br.readLine(); // read amount;
			String line;
			String[] fields;
			LazyNZB nzb;
			while (null != (line = br.readLine())) {
				fields = line.split("\t"); //REPORTID \t SIZE \t NAME
				nzb = new LazyNZB(Util.saveFileName(fields[2]) + ".nzb",this);
				nzb.setName(fields[2]);
				nzb.setReportId(Integer.parseInt(fields[0]));
				result.add(nzb);
			}
			return result;
		} catch (IOException e) {
			throw new SearchException("IO error during search.", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					LOG.warn("Error while cleaning up request.",e);
				}
			}
			if (response != null) {
				try {
					response.getEntity().consumeContent();
				} catch (IOException e) {
					LOG.warn("Error while cleaning up request.",e);
				}
			}
		}
		
		
	}
	
	@Override
	public byte[] getData(NZB nzb) {
		if (nzb.getReportId() == null) {
			throw new LazyInitializationException("Unable to fetch data. Not a Newzbin NZB (no reportId).");
		}
		try {
			fillNZBByReportId(nzb);
			return nzb.getData();
		} catch (SearchException e) {
			LOG.error("Error fetching lazy data", e);
			throw new LazyInitializationException("Error fetching newzbindata for reportId "+nzb.getReportId() +".", e);
		}
	}
	
	/**
	 * Validates the configuration.
	 * 
	 * @throws SearchException if configuration isn't ok
	 */
	public void validate() throws SearchException {
		if (username == null || password == null) {
			throw new SearchException("No username and/or password for newzbin configured.");
		}
		try {
			//this is supposed to throw an exception.
			NZB nzb = new NZB("dummy.nzb");
			nzb.setReportId(1);
			fillNZBByReportId(nzb);
		} catch (SearchException e) {
			//If its something else then a 404 something is wrong
			if (e.getMessage().endsWith("doesn't exsist.")) {
				return;
			} else { 
				throw e;
			}
		}
	}
	
	private URI constructURL(String path) throws MalformedURLException {
		
		try {
			return new URL(serverProtocol,serverAddress,serverPort,path).toURI();
		} catch (URISyntaxException e) {
			throw new MalformedURLException(e.getMessage());
		}
	}
}
