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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHttpResponse;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.janoz.usenet.LazyInitializationException;
import com.janoz.usenet.SearchException;
import com.janoz.usenet.model.NZB;

public class NewzbinConnectorTest {

	private static final byte[] SOME_BYTES = "someRandomBytes".getBytes();
	private static String SEARCH_SUCCESS = "searchSuccess.bin"; 
	private static String SEARCH_SPECIALCHARS = "SpecialCharResult.bin";
	
	NewzbinConnectorImpl subject;
	HttpClient clientMock;
	HttpEntity entityMock;
	
	@Before
	public void setup() throws Exception {
		subject = new NewzbinConnectorImpl();
		clientMock = createMock(HttpClient.class);
		entityMock = createMock(HttpEntity.class);
		Field field = NewzbinConnectorImpl.class.getDeclaredField("client");
		field.setAccessible(true);
		field.set(subject, clientMock);
		
		subject.setUsername("USER");
		subject.setPassword("PASS");

	}
	
	
	@Test
	public void testValidationNoCredentials() throws Exception {
		subject.setUsername(null);
		subject.setPassword(null);
		try {
			subject.validate();
			fail();
		} catch (SearchException se) {
			//
		}
		subject.setUsername("USER");
		subject.setPassword(null);

		try {
			subject.validate();
			fail();
		} catch (SearchException se) {
			//
		}
		subject.setUsername(null);
		subject.setPassword("PASS");
		try {
			subject.validate();
			fail();
		} catch (SearchException se) {
			//
		}
	}


	@Test
	public void testValidationFail() throws Exception {
		Capture<HttpPost> postCapture = recordHttpExecute(recordUnauthorized());
		
		replay(clientMock,entityMock);

		try {
			subject.validate();
			fail("validation should fail with invalid credentials");
		} catch (SearchException se) {
			//expected
			assertEquals("Incorrect Newzbin credentials for 'USER' while fetching 'dummy.nzb'(1).", se.getMessage());
		}
		verify(clientMock,entityMock);
		String request = getRequestContent(postCapture);
		Properties params = extractUrlencodedParams(request);
		assertEquals("USER",params.getProperty("username"));
		assertEquals("PASS",params.getProperty("password"));
	}

	@Test
	public void testValidationSuccess() throws Exception {
		Capture<HttpPost> postCapture = recordHttpExecute(recordNotFound());
		
		replay(clientMock,entityMock);
		
		subject.validate();
		
		verify(clientMock,entityMock);
		String request = getRequestContent(postCapture);
		Properties params = extractUrlencodedParams(request);
		assertEquals("USER",params.getProperty("username"));
		assertEquals("PASS",params.getProperty("password"));
	}

	
	@Test
	public void testGetDataNonNewzbinNZB() throws Exception {
		NZB nzb = new NZB("test.nzb");
		nzb.setReportId(null);
		try {
			subject.getData(nzb);
			fail("Should have thrown an exception");
		} catch (LazyInitializationException se) {
			//expected
		}
	}

	@Test
	public void testGetDataWithError() throws Exception {
		
		Capture<HttpPost> postCapture = recordHttpExecute(recordNotFound()); 
	
		replay(clientMock,entityMock);
		try {
			NZB nzb = new NZB("test.nzb");
			nzb.setName("NAME");
			nzb.setReportId(Integer.valueOf(10));
			subject.getData(nzb);
			fail("Should have thrown an exception");
		} catch (LazyInitializationException se) {
			assertEquals("'NAME'(10) doesn't exsist.",se.getCause().getMessage());
		}
		verify(clientMock,entityMock);
		assertDirectNzb(getRequestContent(postCapture), 10);
	}

	@Test
	public void testGetDataSuccess() throws Exception {
		Capture<HttpPost> postCapture = recordHttpExecute(recordFound(), SOME_BYTES);
		replay(clientMock,entityMock);

		NZB nzb = new NZB("test.nzb");
		nzb.setReportId(Integer.valueOf(10));
		assertArrayEquals(SOME_BYTES,subject.getData(nzb));
		
		verify(clientMock,entityMock);
		assertArrayEquals(SOME_BYTES,nzb.getData());
		assertDirectNzb(getRequestContent(postCapture), 10);
	}

	@Test
	public void testFillNzbOk() throws Exception{
		Capture<HttpPost> postCapture = recordHttpExecute(recordFound(), SOME_BYTES);
		replay(clientMock,entityMock);

		NZB nzb = new NZB("test.nzb");
		nzb.setReportId(Integer.valueOf(10));
		subject.fillNZBByReportId(nzb);

		verify(clientMock,entityMock);
		assertArrayEquals(SOME_BYTES,nzb.getData());
		assertDirectNzb(getRequestContent(postCapture), 10);
	}

	@Test
	public void testFillNoReportId() throws Exception{
		replay(clientMock,entityMock);

		NZB nzb = new NZB("test.nzb");
		nzb.setReportId(null);
		try {
			subject.fillNZBByReportId(nzb);
			fail();
		} catch (SearchException e) {
			assertEquals("No ReportId. Unable to retrieve data using Newzbin.", e.getMessage());
		}
		verify(clientMock,entityMock);
	}

	
	@Test
	public void testFillNotFound() throws Exception{
		Capture<HttpPost> postCapture = recordHttpExecute(recordNotFound());
		replay(clientMock,entityMock);
		try {
			NZB nzb = new NZB("test.nzb");
			nzb.setName("NAME");
			nzb.setReportId(Integer.valueOf(10));
			subject.fillNZBByReportId(nzb);
			fail("Should have thrown an exception");
		} catch (SearchException se) {
			assertEquals("'NAME'(10) doesn't exsist.",se.getMessage());
		}
		verify(clientMock,entityMock);
		assertDirectNzb(getRequestContent(postCapture), 10);
	}
	@Test
	public void testFillNzbRetry() throws Exception{
		Capture<HttpPost> postCapture1 = recordHttpExecute(recordRetryResponse());
		Capture<HttpPost> postCapture2 = recordHttpExecute(recordFound(), SOME_BYTES);
		replay(clientMock,entityMock);

		NZB nzb = new NZB("test.nzb");
		nzb.setReportId(Integer.valueOf(10));
		
		interruptThisThreadIn(100);
		subject.fillNZBByReportId(nzb);

		verify(clientMock,entityMock);
		assertArrayEquals(SOME_BYTES,nzb.getData());
		assertDirectNzb(getRequestContent(postCapture1), 10);
		assertDirectNzb(getRequestContent(postCapture2), 10);
	}

	@Test
	public void testFill400() throws Exception {
		testFillError(400,"Bad Request, please supply all parameters");
	}
	@Test
	public void testFill402() throws Exception {
		testFillError(402,"Payment Required, not Premium");
	}
	@Test
	public void testFill500() throws Exception {
		testFillError(500,"Internal Server Error, please report to Administrator");
	}
	@Test
	public void testFill503() throws Exception {
		testFillError(503,"Service Unavailable, site is currently down");
	}
	
	public void testFillError(int code, String message) throws Exception{
		recordHttpExecute(reocordOtherError(code, message));
		replay(clientMock,entityMock);
		try {
			NZB nzb = new NZB("test.nzb");
			nzb.setName("NAME");
			nzb.setReportId(Integer.valueOf(10));
			subject.fillNZBByReportId(nzb);
			fail("Should have thrown an exception");
		} catch (SearchException se) {
			//OK
		}
		verify(clientMock,entityMock);
	}

	
	@Test
	public void testFillNzbNonApiResponse() throws Exception{
		Capture<HttpPost> postCapture = recordHttpExecute(recordNonApiResponse());
		replay(clientMock,entityMock);
		try {
			NZB nzb = new NZB("test.nzb");
			nzb.setName("NAME");
			nzb.setReportId(Integer.valueOf(10));
			subject.fillNZBByReportId(nzb);
			fail("Should have thrown an exception");
		} catch (SearchException se) {
			//Expected
		}
		verify(clientMock,entityMock);
		assertDirectNzb(getRequestContent(postCapture), 10);
		
	}

	@Test
	public void testSearch() throws Exception {
		Capture<HttpPost> postCapture = recordHttpExecute(recordSearchResponse(), SEARCH_SPECIALCHARS);
		replay(clientMock,entityMock);

		List<NZB> results = subject.search("The query", null, 100, 1000, 
				null,
				null);
		
		verify(clientMock,entityMock);
		// nzb
		assertSpecialCharSearchResult(results);
		
		String request = getRequestContent(postCapture);
		Properties params = extractUrlencodedParams(request);
		//credentials
		assertEquals("USER",params.getProperty("username"));
		assertEquals("PASS",params.getProperty("password"));
		//search
		assertEquals("The+query",params.getProperty("query"));
		assertEquals("100",params.getProperty("u_post_larger_than"));
		assertEquals("1000",params.getProperty("u_post_smaller_than"));
		//default
		assertEquals("1",params.getProperty("u_completions"));
		assertEquals("p",params.getProperty("fpn"));
		assertEquals("ps_edit_date",params.getProperty("sort"));
		assertEquals("desc",params.getProperty("order"));
		assertEquals("0",params.getProperty("retention"));

		assertEquals(10,params.size());
	}
	@Test
	public void testSearchEmpty() throws Exception {
		recordHttpExecute(recordSearchResponseEmpty());
		replay(clientMock,entityMock);

		List<NZB> results = subject.search("The query", null, 100, 1000, 
				null,
				null);
		
		verify(clientMock,entityMock);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testSearchWithNewsgroups() throws Exception {
		Capture<HttpPost> postCapture = recordHttpExecute(recordSearchResponse(), SEARCH_SPECIALCHARS);
		replay(clientMock,entityMock);

		List<NZB> results = subject.search("The query", null, 100, 1000, 
				new HashSet<String>(Arrays.asList("aap","noot","mies")), 
				null);		
		
		verify(clientMock,entityMock);
		assertSpecialCharSearchResult(results);

		String request = getRequestContent(postCapture);
		Properties params = extractUrlencodedParams(request);
		List<String> newsgroups = Arrays.asList(params.getProperty("group").split("%2B"));
		assertEquals(3,newsgroups.size());
		assertTrue(newsgroups.contains("aap"));
		assertTrue(newsgroups.contains("noot"));
		assertTrue(newsgroups.contains("mies"));
	}	

	@Test
	public void testSearchWithDefaultNewsgroups() throws Exception {
		subject.setNewsgroups(new HashSet<String>(Arrays.asList("aap","noot","mies")));
		Capture<HttpPost> postCapture = recordHttpExecute(recordSearchResponse(), SEARCH_SPECIALCHARS);
		replay(clientMock,entityMock);

		List<NZB> results = subject.search("The query", null, 100, 1000, 
				null, 
				null);		
		
		verify(clientMock,entityMock);
		assertSpecialCharSearchResult(results);

		String request = getRequestContent(postCapture);
		Properties params = extractUrlencodedParams(request);
		List<String> newsgroups = Arrays.asList(params.getProperty("group").split("%2B"));
		assertEquals(3,newsgroups.size());
		assertTrue(newsgroups.contains("aap"));
		assertTrue(newsgroups.contains("noot"));
		assertTrue(newsgroups.contains("mies"));
	}
	
	@Test
	public void testSearchWithProperties() throws Exception {
		Properties otherProps = new Properties();
		otherProps.setProperty("new", "aValue");
		otherProps.setProperty("sort", "anotherNewValue");
		Capture<HttpPost> postCapture = recordHttpExecute(recordSearchResponse(), SEARCH_SPECIALCHARS);
		replay(clientMock,entityMock);

		List<NZB> results = subject.search("The query", null, 100, 1000, 
				null, 
				otherProps);		
		
		verify(clientMock,entityMock);
		assertSpecialCharSearchResult(results);

		String request = getRequestContent(postCapture);
		Properties params = extractUrlencodedParams(request);
		assertEquals("1",params.getProperty("u_completions"));
		assertEquals("p",params.getProperty("fpn"));
		assertEquals("anotherNewValue",params.getProperty("sort"));
		assertEquals("desc",params.getProperty("order"));
		assertEquals("0",params.getProperty("retention"));
		assertEquals("aValue", params.getProperty("new"));
	}
	

	@Test
	public void testSearch401() throws Exception {
		testSearchError(401,"Unauthorised");
	}
	@Test
	public void testSearch402() throws Exception {
		testSearchError(402,"Payment Required");
	}
	@Test
	public void testSearch500() throws Exception {
		testSearchError(500,"Internal Server Error");
	}
	@Test
	public void testSearch503() throws Exception {
		testSearchError(503,"Service Unavailable");
	}
	@Test
	public void testSearch550() throws Exception {
		testSearchError(550,"Missing q parameter");
	}
	
	public void testSearchError(int code, String message) throws Exception{
		recordHttpExecute(reocordSearchOtherError(code, message));
		replay(clientMock,entityMock);
		try {
			subject.search("The query", null, 100, 1000, 
					null,
					null);
			fail("Should have thrown an exception");
		} catch (SearchException se) {
			//OK
		}
		verify(clientMock,entityMock);	
	}


	private void interruptThisThreadIn(final long milis) {
		final Thread t = Thread.currentThread();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(milis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				t.interrupt();
			}
		}).start();
	}
	
	
	private Capture<HttpPost> recordHttpExecute(HttpResponse response) throws IOException,
			ClientProtocolException {
		return recordHttpExecute(response, (InputStream)null);
	}
	
	private Capture<HttpPost> recordHttpExecute(HttpResponse response, String filename) throws IOException,
			ClientProtocolException {
		
		URL resource = this.getClass().getClassLoader().getResource("newzbin/"+filename);
		return recordHttpExecute(response, new FileInputStream(resource.toString().substring(6)));
	}
	
	private Capture<HttpPost> recordHttpExecute(HttpResponse response,byte[] retunedData) throws IOException,
			ClientProtocolException {
		return recordHttpExecute(response, new ByteArrayInputStream(retunedData));
	}

	private Capture<HttpPost> recordHttpExecute(HttpResponse response,InputStream result) throws IOException,
			ClientProtocolException {
		Capture<HttpPost> postCapture = new Capture<HttpPost>(); 
		expect(clientMock.execute(capture(postCapture))).andReturn(response);
		if (result != null)
			expect(entityMock.getContent()).andReturn(result);
		entityMock.consumeContent();
		return postCapture;
	}

	private String getRequestContent(Capture<HttpPost> postCapture)
			throws IOException {
		HttpPost post = postCapture.getValue();
		String request = readStream(post.getEntity().getContent());
		return request;
	}		
	
	
	
	private String readStream(InputStream is) throws IOException{
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		return new String(bytes,"UTF8");
	}
	
	private Properties extractUrlencodedParams(String request) {
		Properties result = new Properties();
		for(String set:request.split("&")) {
			String[] keyVal = set.split("=");
			result.setProperty(keyVal[0], keyVal[1]);
		}
		return result;
	}

	private HttpResponse recordUnauthorized() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),400,"Bad Request");
		recordHeaders(response,null,null,"401","Unauthorised, check username/password?");
		response.setEntity(entityMock);
		return response;
	}


	private HttpResponse recordNotFound() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),400,"Bad Request");
		recordHeaders(response,null,null,"404","Not Found, data doesn't exist?");
		response.setEntity(entityMock);
		return response;
	}
	
	private HttpResponse reocordOtherError(int code, String message) {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),400,"Bad Request");
		recordHeaders(response,null,null,""+code,message);
		response.setEntity(entityMock);
		return response;
	}

	private HttpResponse recordFound() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),200,"OK");
		recordHeaders(response,
				"Discussion Digest: microsoft.public.it.dotnet.asp (2010-02-17)",
				"Discussions",
				"200",
				"OK, NZB content follows");
		response.setEntity(entityMock);
		return response;
	}

	private HttpResponse recordRetryResponse() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),400,"OK");
		
		recordHeaders(response, null,null,"450","Try Later, wait 10 seconds for counter to reset");
		response.setEntity(entityMock);
		return response;
	}
	
	private HttpResponse recordNonApiResponse() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),200,"OK");
		recordHeaders(response, null,null,null,null);
		response.setEntity(entityMock);
		return response;
	}

	private HttpResponse recordSearchResponse() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),200,"OK");
		recordHeaders(response, null,null,null,null);
		response.setEntity(entityMock);
		return response;
	}

	private HttpResponse recordSearchResponseEmpty() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),204,"No Content");
		recordHeaders(response, null,null,null,null);
		response.setEntity(entityMock);
		return response;
	}
	
	private HttpResponse recordSearchRetryResponse() {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),450,"Try Later, wait 10 seconds for counter to reset");
		
		recordHeaders(response, null,null,null,null);
		response.setEntity(entityMock);
		return response;
	}

	private HttpResponse reocordSearchOtherError(int code, String message) {
		HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1),code,message);
		recordHeaders(response,null,null,null,null);
		response.setEntity(entityMock);
		return response;
	}



	private void recordHeaders(HttpResponse response, String name, String category, String code, String text) {
		response.addHeader("Date","Thu, 14 Jan 1977 20:49:19 GMT");
		response.addHeader("Server","Apache");
		if (name != null) 
			response.addHeader("X-DNZB-Name",name);
		if (category != null)
			response.addHeader("X-DNZB-Category",category);
		if (code != null)
			response.addHeader("X-DNZB-RCode",code);
		if (text != null)
			response.addHeader("X-DNZB-RText",text);
		response.addHeader("Vary","User-Agent,Accept-Encoding");
		response.addHeader("Content-Encoding","gzip");
		response.addHeader("Connection","close");
		response.addHeader("Transfer-Encoding","chunked");
		response.addHeader("Content-Type","text/html; charset=ISO-8859-1");
	}


	private void assertDirectNzb(String request, int reportId) {
		Properties params = extractUrlencodedParams(request);
		assertEquals("USER",params.getProperty("username"));
		assertEquals("PASS",params.getProperty("password"));
		assertEquals(""+reportId,params.getProperty("reportid"));
	}

	private void assertSpecialCharSearchResult(List<NZB> results) {
		assertEquals(1, results.size());
		assertEquals("Christopher Paolini - Eragon 2/3 - Der Auftrag des Ältesten", results.get(0).getName());
		assertEquals(Integer.valueOf(5785827),results.get(0).getReportId());
	}
	

	
}
