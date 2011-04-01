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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.janoz.usenet.RetrieveException;
import com.janoz.usenet.model.NZB;

/**
 * @author Gijs de Vries
 *
 */
public class SabnzbdProcessor extends WebbasedProcessor {
	
	private static final Log LOG = LogFactory.getLog(SabnzbdProcessor.class);
	
	private Integer activeness = -1;
	private String category;
	private String apiKey;
	
	public SabnzbdProcessor() {
		super();
	}
	
	/**
	 * @param activeness
	 * 			-1=Default
	 * 			 0=None
	 * 			 1=+Repair
	 * 			 2=+Unpack
	 * 			 3=+Delete
	 */
	public void setActiveness(Integer activeness) {
		this.activeness = activeness;
	}
		
	public void setCategory(String category) {
		this.category = category;
	}


	public void setApiKey(String apiKey) {
		this.apiKey = null;
		if (apiKey != null){
			apiKey = apiKey.trim();
			if (apiKey.length() >0 ){
				this.apiKey = apiKey;
			}
		} 
	}


	/**
	 * Validates the configuration. Requests a 0.4 api queue request.
	 * 
	 * warnings&output=json
	 * 
	 * 
	 * @throws RetrieveException When validation fails
	 */
	@Override
	public void validate() throws RetrieveException {
		HttpResponse response = null;
		try {
			List<NameValuePair> paramList = getAuthProps();
			paramList.add(new BasicNameValuePair("mode","warnings"));
			paramList.add(new BasicNameValuePair("output","json"));
			response = doGet("api",paramList);
			
			
			BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuilder resultBuilder = new StringBuilder();
			String line = null;
			while (null != (line = br.readLine())) {
				resultBuilder.append(line + "\n");
			}
			String result = resultBuilder.toString();
			if (LOG.isDebugEnabled()){
				LOG.debug("While validating, response was '" + result +"'.");
			}
			//0.4 error
			if (result.toLowerCase().startsWith("error")) {
				throw new RetrieveException("Error connection to Sabnzbd. Sabnzbd returned '" + result + "'.");
			}
			if (response.getStatusLine().getStatusCode() == 500) {
				throw new RetrieveException("Sabnzbd returned a server error (maybe configured authentication while server has not?)");
			}
			//0.5 error
			if (result.toLowerCase().contains("\"status\":false") 
					&& result.indexOf("\"error\":") > -1) {
				int start = result.indexOf("\"error\":") + 9;
				throw new RetrieveException("Error connection to Sabnzbd. Sabnzbd returned '" + result.substring(start, result.length()-2) + "'.");
			}
			
		} catch (IOException e) {
			throw new RetrieveException("Unable to reach Sabnzbd.", e);
		} finally {
			if (response != null && response.getEntity() != null) {
				try {
					response.getEntity().consumeContent();
				} catch (IOException e) {
					LOG.warn("IOError while cleaning up request",e);
				}
			}
		}
	}

	
	@Override
	public void validateResponse(InputStream responseStream) throws RetrieveException {
		String result = responseAsString(responseStream);
		if (LOG.isDebugEnabled()){
			LOG.debug("While sending report, response was '" + result +"'.");
		}
		
		if (!result.startsWith("ok")) {
			throw new RetrieveException("Sabnzbd returned an error '"+result+"'.");
		}
	}

	/**
	 * Tries to login with the configurated credentials
	 * @throws IOException If communication or login failed 
	 * @throws RetrieveException If login failed
	 */
	@Override
	public void login() throws IOException, RetrieveException {
		// DO NOTHING
	}

	@Override
	public void logout() throws IOException, RetrieveException {
		// DO NOTHING
	}
	
	
	@Override
	public boolean useUrl(NZB nzb) {
		// Always use URL when available
		return nzb.getUrl() != null;
	}

	@Override
	public String getUrlCommand() {
		return "api";
	}

	@Override
	public List<NameValuePair> getUrlProps(NZB nzb) {
		List<NameValuePair> result = getOtherProps();
		result.add(new BasicNameValuePair("mode", "addurl"));
		result.add(new BasicNameValuePair("name", nzb.getUrl()));
		result.add(new BasicNameValuePair("nzbname", nzb.getName()));
		return result;
	}

	@Override
	public String getFilepostCommand() {
		return "api";
	}



	@Override
	public String getFilepostField() {
		return "nzbfile";
	}

	@Override
	public List<NameValuePair> getFilepostOtherProps(NZB nzb) {
		List<NameValuePair> result = getOtherProps();
		result.add(new BasicNameValuePair("mode", "addfile"));
		return result;
	}

	@Override
	public boolean useReportId(NZB nzb) {
		//Always use reportId, unless not available; 
		return nzb.getReportId() != null;
	}


	@Override
	public String getReportIdCommand() {
		return "api";
	}

	@Override
	public List<NameValuePair> getReportIdProps(NZB nzb) {
		List<NameValuePair> result = getOtherProps();
		result.add(new BasicNameValuePair("mode", "addid"));
		result.add(new BasicNameValuePair("name", ""+nzb.getReportId()));
		return result;
	}

	private List<NameValuePair> getOtherProps() {
		List<NameValuePair> result = getAuthProps();
		result.add(new BasicNameValuePair("pp", activeness.toString()));
		if (category != null) {
			result.add(new BasicNameValuePair("cat", category));
		}
		return result;
	}
	
	private List<NameValuePair> getAuthProps() {
		List<NameValuePair> result = new ArrayList<NameValuePair>();
		if (apiKey != null) {
			result.add(new BasicNameValuePair("apikey", apiKey));
		}
		if (username != null) {
			result.add(new BasicNameValuePair("ma_username",username));
		}
		if (password != null) {
			result.add(new BasicNameValuePair("ma_password",password));
		}
		return result;
	}
}
