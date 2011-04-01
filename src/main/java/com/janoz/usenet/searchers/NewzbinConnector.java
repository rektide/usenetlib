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
package com.janoz.usenet.searchers;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.janoz.usenet.SearchException;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.searchers.categories.NewzbinCategory;
import com.janoz.usenet.suppliers.LazyNZBSupplier;

public interface NewzbinConnector extends LazyNZBSupplier{

	/**
	 * Maximum length of a newzBin search query
	 */
	int MAX_QUERY_LENGTH = 255;

	
	/**
	 * Fills in an NZB object. It handles user credentials and 
	 * returning errormessages. It also throttles the maximum amount 
	 * of request. If the maximum is reached and tries again later. 
	 * When requesting al lot of NZB files a call to this method can 
	 * take up to a minute because of this waiting period.
	 *  
	 * This method is synchronised because of the maximum amount 
	 * of requests that can be done within one minute. This class 
	 * should be the only instance requesting NZB files from newzbin.
	 * @param nzb An NZB object containing the NewzBin report id.
	 * @throws SearchException  when something went wrong in the 
	 * 			communication with newzbin.com.
	 */
	void fillNZBByReportId(NZB nzb) throws SearchException;

	/**
	 * @param query Search query
	 * @param category Category to search in (null for all)
	 * @param minSize minimal size in bytes
	 * @param maxSize maximal size in bytes
	 * @param newsgroups List of newsgroups to search in 
	 * @param newzbinQueryProps 
	 * @return a list of found (lazy)nzbs
	 * @throws SearchException
	 */
	List<NZB> search(String query, NewzbinCategory category, Integer minSize, Integer maxSize,
			Set<String> newsgroups, Properties newzbinQueryProps)
			throws SearchException;


}
