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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;

import com.janoz.usenet.RetrieveException;
import com.janoz.usenet.model.NZB;


/**
 * @author vriesgi
 *
 */
public class NZBGetProcessor extends WebbasedProcessor{

	private static final Log LOG = LogFactory.getLog(NZBGetProcessor.class);
	private boolean php5 = false;
	
	public void setPhp5(boolean php5){
		this.php5 = php5;
	}

	@Override
	public String getFilepostCommand() {
		return php5?"status.php5":"status.php";
	}

	@Override
	public String getFilepostField() {
		return "nzbfile";
	}

	@Override
	public List<NameValuePair> getFilepostOtherProps(NZB nzb) {
		return Collections.emptyList();
	}
		
	@Override
	public void login() throws IOException, RetrieveException {
		List<NameValuePair> fields = new ArrayList<NameValuePair>();
		if ((username != null) && (password != null)) {
			fields.add(new BasicNameValuePair("username",username));
			fields.add(new BasicNameValuePair("password",password));
			HttpResponse response = doGet(php5?"index.php5":"index.php", fields);
			if (isLoginResponse(response.getEntity().getContent())) {
				throw new RetrieveException("Authorization failed. Unable to login to NzbGetWeb.");
			}
			response.getEntity().consumeContent();
		}
	}
	
	@Override
	public void logout() throws IOException, RetrieveException {
		//do nothing
	}

	@Override
	public void validate() throws RetrieveException {
		//request status and 
		//content.indexOf("Please login") > -1;
		//read status cookie contents
	}
	
	@Override
	public void validateResponse(InputStream responseStream)
			throws RetrieveException {
		Cookie uploadStatus = super.getCookie("upload_status");
		if (uploadStatus != null) {
			try {
				String status = stripTags(URLDecoder.decode(uploadStatus.getValue(),"UTF8"));
				if (status.startsWith("Error")) {
					throw new RetrieveException("NzbGetWeb "+status.replace('\n',' '));
				}
			} catch (UnsupportedEncodingException e) {
				LOG.error("UTF8 encoding not available. This should have been impossible");
				throw new RuntimeException("UTF8 encoding not available. This should have been impossible");
			}
		} else {
			if (isLoginResponse(responseStream)) {
				throw new RetrieveException("Authorization required.");
			}
			throw new RetrieveException("Unknown response from NzbGetWeb.");
		}

		

	}

	private boolean isLoginResponse(InputStream stream){
		StringWriter sw = new StringWriter();
		int i;
		try {
			while ((i = stream.read()) >= 0){
				sw.write(i);
			}
		} catch (IOException e) {
			LOG.error("Error reading response.",e );
			throw new RetrieveException("Error reading response.", e);
		}
		return sw.toString().indexOf("Please login") > -1;
	}
	
	
	String stripTags(String source) {
		StringBuilder builder = new StringBuilder();
		//
		int start = 0;
		int end = source.indexOf('<');
		while (end >= 0) {
			builder.append(source.substring(start,end));
			start = source.indexOf('>', end) + 1;
			end = source.indexOf('<',start);
		}
		builder.append(source.substring(start,source.length()));
		return builder.toString();
	}

	//ReportId isn't supported
	@Override
	public boolean useReportId(NZB nzb) {
		return false;
	}
	@Override
	public String getReportIdCommand() {
		return null;
	}
	@Override
	public List<NameValuePair> getReportIdProps(NZB nzb) {
		return null;
	}

	@Override
	public boolean useUrl(NZB nzb) {
		return false;
	}
	@Override
	public String getUrlCommand() {
		return null;
	}
	@Override
	public List<NameValuePair> getUrlProps(NZB nzb) {
		return null;
	}
}
