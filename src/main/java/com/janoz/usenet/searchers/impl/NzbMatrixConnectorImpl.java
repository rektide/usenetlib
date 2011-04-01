package com.janoz.usenet.searchers.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.janoz.usenet.LazyInitializationException;
import com.janoz.usenet.SearchException;
import com.janoz.usenet.model.LazyNZB;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.searchers.NzbMatrixConnector;
import com.janoz.usenet.searchers.categories.MatrixCategory;
import com.janoz.usenet.support.GzipDecompressingEntity;
import com.janoz.usenet.support.Throttler;
import com.janoz.usenet.support.Util;

public class NzbMatrixConnectorImpl implements NzbMatrixConnector{
	
	private static final Log LOG = LogFactory.getLog(NzbMatrixConnectorImpl.class);
	private static final String CMD_SEARCH = "/v1.1/search.php";
	private static final String CMD_FETCH = "/v1.1/download.php";
	private static final String CMD_DETAILS = "/v1.1/details.php";
	
	private static final long WAIT_PERIOD = 15 * 1000;
	
	private static final Throttler DOWNLOAD_THROTTLER = new Throttler(11 * 1000); //10 secs + slack
	private static final Throttler SEARCH_THROTTLER = new Throttler(31 * 1000); //30 secs + slack

	private static final String NZB_CONTENT_TYPE = "application/x-nzb";

	//private DefaultHttpClient httpclient = new DefaultHttpClient();
	private String serverAddress = "api.nzbmatrix.com";
	private Integer serverPort = 443;
	private String serverProtocol = "https";
	private String apiKey;
	private String username;
	private int retention;
	
	//storage
	private Set<String> searchParams; 
	
	public NzbMatrixConnectorImpl() {
		//
	}
	
	public HttpClient getClient() {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		GzipDecompressingEntity.addTo(httpclient);
		return httpclient;
	}
	
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public void setServerPort(Integer serverPort) {
		this.serverPort = serverPort;
	}

	public void setServerProtocol(String serverProtocol) {
		this.serverProtocol = serverProtocol;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	
	public void setRetention(int retention) {
		this.retention = retention;
	}

	
	
	@Override
	public Set<String> getSearchParams() {
		return searchParams;
	}

	public void setSearchParams(Set<String> searchParams) {
		this.searchParams = searchParams;
	}

	public void validate() throws SearchException{
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("id","0"));
		HttpClient httpclient = getClient();
		try {
			HttpEntity result = doRequest(httpclient,CMD_DETAILS, params);
			String content;
			content = EntityUtils.toString(result);
			if (content.startsWith("error:no_nzb_found")
					|| content.startsWith("error:invalid_nzbid")) {
				return;
			}
			translateError(content);
			throw new SearchException("Unknown response. Content was:\n" + content);
		} catch (IOException e) {
			throw new SearchException("IO error connecting to NZBMatrix." ,e);
		} catch (URISyntaxException e) {
			throw new SearchException("Error converting URL." ,e);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	@Override
	public void fillNzbByMatrixId(NZB nzb) throws SearchException {
		DOWNLOAD_THROTTLER.throttle();
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("id",""+nzb.getMatrixId()));
		HttpClient httpclient = getClient();
		try {
			boolean retry;
			do {
				retry = false;
				HttpEntity result = doRequest(httpclient,CMD_FETCH, params);
				if (result.getContentType().getValue().equals(NZB_CONTENT_TYPE)) {
					nzb.setData(EntityUtils.toByteArray(result));
				} else {
					String content;
					content = EntityUtils.toString(result);
					if (checkAndHandleWaitError(content)) {
						retry = true;
					} else {
						//try other errors;
						translateError(content);
						throw new SearchException("No NZB content type. Content was:\n" + content);
					}
				} 
			} while (retry);
		} catch (IOException e) {
			throw new SearchException("Error communicating with NzbMatrix.",e);
		} catch (URISyntaxException e) {
			throw new SearchException("Error constructing uri.",e);
		} finally {
			DOWNLOAD_THROTTLER.setThrottleForNextAction();
			httpclient.getConnectionManager().shutdown();
		}
	}

	/*
	search={SEARCH TERM} = Search Term
	&catid={CATEGORYID} = OPTIONAL, if left blank all categories are searched, category ID used from site.
	&num={MAX RESULTS} = OPTIONAL, if left blank a maximum of 5 results will display, 5 is the maximum number of results that can be produced.
	&age={MAX AGE} = OPTIONAL, if left blank full site retention will be used. Age must be number of "days" eg 200
	&region={SEE DESC} = OPTIONAL, if left blank results will not be limited 1 = PAL, 2 = NTSC, 3 = FREE
	&group={SEE DESC} = OPTIONAL, if left blank all groups will be searched, format is full group name "alt.binaries.X"
	&username={USERNAME} = Your account username.
	&apikey={APIKEY}
	*/
	@Override
	public List<NZB> search(String query, MatrixCategory category) throws SearchException {
		SEARCH_THROTTLER.throttle();
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("search",query));
		if (retention > 0) {
			params.add(new BasicNameValuePair("age", ""+retention));
		}
		if (category != null) {
			params.add(new BasicNameValuePair("catid", category.getValue()));
		}
		HttpClient httpclient = getClient();
		try {
			boolean retry;
			String content = null;
			do{
				retry = false;
				LOG.debug("Searching '" + query + "' in category " + category);
				HttpEntity result = doRequest(httpclient,CMD_SEARCH, params);
				content = EntityUtils.toString(result);
				if (content.startsWith("error:nothing_found")) {
					return Collections.emptyList();
				} 
				if (checkAndHandleWaitError(content)) {
					retry = true;
				} else {
					//try other errors;
					translateError(content);
				}
			} while (retry);
			return parseSearchResult(content);
			
		} catch (IOException ioe) {
			throw new SearchException("Error communicating with NzbMatrix.",ioe);
		} catch (URISyntaxException e) {
			throw new SearchException("Error constructing uri.",e);
		} finally {
			SEARCH_THROTTLER.setThrottleForNextAction();
			httpclient.getConnectionManager().shutdown();
		}
	}

	@Override
	public byte[] getData(NZB nzb) {
		if (nzb.getMatrixId() == null) {
			throw new LazyInitializationException("Unable to fetch data. Not a NzbMatrix NZB (no matrixId).");
		}
		try {
			fillNzbByMatrixId(nzb);
			return nzb.getData();
		} catch (SearchException e) {
			LOG.error("Error fetching lazy data", e);
			throw new LazyInitializationException("Error fetching NZB for MatrixId "+nzb.getMatrixId() +".", e);
		}
	}

	
	private HttpEntity doRequest(HttpClient httpclient, String command, List<NameValuePair> params) throws IOException, URISyntaxException {
		URI uri = constructUrl(command, params);
		HttpGet get = new HttpGet(uri);
    	HttpResponse response = httpclient.execute(get);
    	return response.getEntity();
	}
	
	private URI constructUrl(String command, List<NameValuePair> params)
			throws URISyntaxException {
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.addAll(params);
		qparams.add(new BasicNameValuePair("username", username));
		qparams.add(new BasicNameValuePair("apikey", apiKey));
		URI uri = URIUtils.createURI(serverProtocol, serverAddress, serverPort, command, 
			    URLEncodedUtils.format(qparams, "UTF-8"), null);
		return uri;
	}


	private static final String ERROR_WAIT_PREFIX = "error:please_wait_";
	/**
	 * Will check for a wait error. If found, will wait and return true;
	 * @param message
	 * @return true when it was a wait error. 
	 */
	private boolean checkAndHandleWaitError(String message) {
		if (message.startsWith(ERROR_WAIT_PREFIX)) {
			long period;
			try {
				period = Integer.parseInt(message.substring(ERROR_WAIT_PREFIX.length()));
				period = period * 1000;
			} catch (NumberFormatException nfe) {
				period = WAIT_PERIOD;
			}
			try {
				LOG.debug("Still got flooding error. Pausing for "+period+"ms");
				Thread.sleep(WAIT_PERIOD);
			} catch (InterruptedException e) {
				LOG.debug("Sleep because of wait error interrupted.");
			} 
			return true;
		}
		return false;
	}
	
	private void translateError(String response) throws SearchException{
		if (response.startsWith("error:invalid_login")) throw new SearchException("There is a problem with the username you have provided.");
		if (response.startsWith("error:invalid_api")) throw new SearchException("There is a problem with the API Key you have provided.");
		if (response.startsWith("error:invalid_nzbid")) throw new SearchException("There is a problem with the NZBid supplied.");
		if (response.startsWith("error:vip_only")) throw new SearchException("You need to be VIP or higher to access.");
		if (response.startsWith("error:disabled_account")) throw new SearchException("User Account Disabled.");
		if (response.startsWith("error:x_daily_limit")) throw new SearchException("You have reached the daily download limit of x.");
		if (response.startsWith("error:no_nzb_found")) throw new SearchException("No NZB found.");

		//The following errors should be handled differently
		if (response.startsWith("error:please_wait_")) throw new SearchException("Please wait x seconds before retry.");
		if (response.startsWith("error:nothing_found")) throw new SearchException("No Results Found.");
		if (response.startsWith("error:")) throw new SearchException("Unknown " + response);

	}
	
	private List<NZB> parseSearchResult(String searchdata) {
//		NZBID:610606;
//		NZBNAME:Lost S05 DVDRip XviD TL;
//		LINK:nzbmatrix.com/nzb-details.php?id=610606&hit=1;
//		SIZE:7313282826.24;
//		INDEX_DATE:2010-03-31 08:13:09;
//		USENET_DATE:2010-03-31 2:00:14;
//		CATEGORY:TV &amp;gt; Divx/Xvid;
//		GROUP:alt.binaries.multimedia;
//		COMMENTS:2;
//		HITS:175;
//		NFO:yes;
//		REGION
		DateFormat df = new SimpleDateFormat("yyyy-MM-d HH:mm:ss");
		int start = 0;
		int end = searchdata.indexOf('\n');
		NZB nzb = new LazyNZB("",this);
		List<NZB> result = new ArrayList<NZB>();
		while (end >= 0) {
			String line = searchdata.substring(start,end);
			if ("|".equals(line)) {
				if (nzb.getMatrixId()!= null
						&& nzb.getName() != null 
						&& nzb.getPostDate() != null) {
					result.add(nzb);
				} else {
					LOG.error("Skipping incomplete result: " +
							"\nname : " + nzb.getName() +
							"\nid   : " + nzb.getMatrixId());
				}
				nzb = new LazyNZB("",this);
			} else if (line.endsWith(";")) {
				String label = line.substring(0,line.indexOf(':'));
				String value = line.substring(label.length()+1,line.indexOf(';'));
				if ("NZBID".equals(label)) {
					try {
						nzb.setMatrixId(Integer.parseInt(value));
					} catch (NumberFormatException e) {
						LOG.error("Unable to parse MatrixId '"+value+"'.",e);
					}
				} else if ("USENET_DATE".equals(label)) {
					try {
						nzb.setPostDate(df.parse(value));
					} catch (java.text.ParseException e) {
						LOG.error("Unable to parse date '"+value+"'.",e);
					}
				} else if ("NZBNAME".equals(label)) {
					nzb.setName(value);
					nzb.setFilename(Util.saveFileName(value) + ".nzb");
				}
			} else {
				LOG.debug("Unknown content in search result : '" +line+ "'");
			}
			start = end +1;
			end = searchdata.indexOf('\n',start);
		}
		return result;
	}
	
}