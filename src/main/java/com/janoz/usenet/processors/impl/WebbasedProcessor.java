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
package com.janoz.usenet.processors.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.janoz.usenet.RetrieveException;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.processors.NZBProcessor;


/**
 * @author Gijs de Vries
 *
 */
public abstract class WebbasedProcessor implements NZBProcessor {

	private static final Log LOG = LogFactory.getLog(WebbasedProcessor.class);
			
	protected String username = null;
	protected String password = null;

	private String serverProtocol = "http";
	private String serverAddress = null;
	private Integer serverPort = null;
	private String rootDir = "";
	private boolean useReportId = false;
	private boolean useUrl = false;
	
	private HttpClient httpClient;
	private CookieStore cookieStore;
	private HttpContext context;
	
	
	public WebbasedProcessor() {
		BasicHttpParams params = new BasicHttpParams();
		params.setParameter(CookieSpecPNames.DATE_PATTERNS,
		         Arrays.asList(
		        		 "EEE, dd-MMM-yyyy HH:mm:ss z", 
		        		 "EEE, dd MMM yyyy HH:mm:ss z"));
		httpClient = new DefaultHttpClient(params);
		cookieStore = new BasicCookieStore();
		context = new BasicHttpContext();
		context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
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

	public void setUseReportId(boolean useReportId) {
		this.useReportId = useReportId;
	}
	
	public void setUseUrl(boolean useUrl) {
		this.useUrl = useUrl;
	}
	
	public Cookie getCookie(String name) {
		for (Cookie cookie : cookieStore.getCookies()) {
			if (cookie.getName().equals(name)) {
				return cookie;
			}
		}
		return null;
	}
	
	public List<Cookie> getCookies() {
		return cookieStore.getCookies();
	}

	@Override
	public void processNZB(NZB nzb) throws RetrieveException {
		try {
			login();
			List<NameValuePair> fields = null;
			String command = null;
			if (useReportId(nzb)){
				fields = getReportIdProps(nzb);
				command = getReportIdCommand();
			} else if (useUrl(nzb)) {
				fields = getUrlProps(nzb);
				command = getUrlCommand();
			}
			if (fields != null) {
				//Do the request				
				HttpResponse response = null;
				try {
					response = doGet(command, fields);
					validateResponse(response.getEntity().getContent());
				} finally {
					if (response != null) {
						response.getEntity().consumeContent();
					}
				}
			} else { // no url or reportId
				//post file using POST
				fields = getFilepostOtherProps(nzb);
				command = getFilepostCommand();
				MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
				ContentBody cb = new InputStreamBody(new ByteArrayInputStream(nzb.getData()), nzb.getFilename());
				entity.addPart(getFilepostField(), cb);
				HttpResponse response = null;
				try {
					response = doPost(command, fields, entity);
					validateResponse(response.getEntity().getContent());
				} finally {
					if (response != null) {
						response.getEntity().consumeContent();
					}
				}
			}
			logout();
		}catch (IOException e) {
			throw new RetrieveException("Error posting nzb file", e);
		}
	}


	public boolean useReportId(NZB nzb){
		return useReportId && nzb.getReportId()!=null;
	}

	public boolean useUrl(NZB nzb){
		return useUrl && nzb.getUrl()!=null;
	}

	
	public abstract void login() throws IOException, RetrieveException;
	public abstract void logout() throws IOException, RetrieveException;
	
	public abstract String getFilepostCommand();
	public abstract String getFilepostField();
	public abstract List<NameValuePair> getFilepostOtherProps(NZB nzb);
	public abstract String getUrlCommand();
	public abstract List<NameValuePair> getUrlProps(NZB nzb);
	public abstract String getReportIdCommand();
	public abstract List<NameValuePair> getReportIdProps(NZB nzb);
	
	public void validateResponse(InputStream responseStream) throws RetrieveException{
		LOG.info("Response assumed valid.");
		if (LOG.isTraceEnabled()) {
			BufferedReader br = new BufferedReader(new InputStreamReader(responseStream));
			String line = null;
			try {
				while (null != (line = br.readLine())) {
					LOG.trace(line);
				}
			} catch (IOException e) {
				throw new RetrieveException("Error reading response.",e);
			}
		}
	}
	
	
	protected HttpResponse doGet(String command, List<NameValuePair> fields) throws IOException {
		HttpGet request = new HttpGet(constructURI(command, fields));
//		LOG.debug("Request  : " + request.getRequestLine());
		HttpResponse response = httpClient.execute(request,context);
//		LOG.debug("Response : " + response.getStatusLine().getStatusCode() + " : " + response.getStatusLine().getReasonPhrase());
		return response;
	}

	protected HttpResponse doPost(String command, List<NameValuePair> fields, HttpEntity entity) throws IOException {
		HttpPost request = new HttpPost(constructURI(command,fields));
		request.setEntity(entity);
//		LOG.debug("Request  : " + request.getRequestLine());
		HttpResponse response = httpClient.execute(request,context);
//		LOG.debug("Response : " + response.getStatusLine().getStatusCode() + " : " + response.getStatusLine().getReasonPhrase());
		return response;
	}


	private URI constructURI(String command, List<NameValuePair> fields)
			throws IOException {
		
		try {
			String url = 
				(((rootDir == null) || (rootDir.length()==0)) ?
						"" : "/" + rootDir)
				+ "/" + command;
			String params = null;
			if (fields != null) {
				params =  URLEncodedUtils.format(fields, "UTF-8");
			}
			return URIUtils.createURI(serverProtocol, serverAddress, serverPort, url, 
				    params, null);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	protected String responseAsString(InputStream responseStream) {
		StringWriter sw = new StringWriter();
		int i;
		try {
			while ((i = responseStream.read()) >= 0){
				sw.write(i);
			}
		} catch (IOException e) {
			LOG.error("Error reading response.",e );
			throw new RetrieveException("Error reading response.", e);
		}
		String result =sw.getBuffer().toString().trim();
		return result;
	}
}
