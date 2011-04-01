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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

public final class LogUtil {

	private static final Log LOG = LogFactory.getLog(LogUtil.class);
	
	private static final int BUFF_SIZE = 1024;
	
	private LogUtil() {
		//
	}
	
	public static void logResponseHeader(HttpResponse response) {
		if (LOG.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("\n");
			StatusLine statusLine = response.getStatusLine();
			sb.append("HttpResponse response = new BasicHttpResponse(new ProtocolVersion(\"");
			sb.append(statusLine.getProtocolVersion().getProtocol());
			sb.append("\",");
			sb.append(statusLine.getProtocolVersion().getMajor());
			sb.append(",");
			sb.append(statusLine.getProtocolVersion().getMinor());
			sb.append("),");
			sb.append(statusLine.getStatusCode());
			sb.append(",\"");
			sb.append(statusLine.getReasonPhrase());
			sb.append("\");\n");
	
			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				sb.append("response.addHeader(\"" + header.getName()+ "\",\"" + header.getValue() + "\");\n");
			}
			LOG.trace(sb.toString());
		}
		
	}
	
	public static InputStream dumpStreamAndReOffer(InputStream is) throws IOException{
		FileOutputStream fos = null; 
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			copyStream(is,os);
			byte[] data = os.toByteArray();
			fos = new FileOutputStream(File.createTempFile("dump", ".bin", new File(".")));
			fos.write(data);
			return new ByteArrayInputStream(data);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
		
	}
	
	private static void copyStream(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[BUFF_SIZE];
		int len;

		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}
		in.close();
		out.close();
	}
}
