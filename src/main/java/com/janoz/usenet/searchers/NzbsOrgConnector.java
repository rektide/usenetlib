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

import com.janoz.usenet.SearchException;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.searchers.categories.NzbsOrgsCategory;


public interface NzbsOrgConnector {

	/**
	 * @param query
	 * @param category 
	 * @param minSize
	 * @param maxSize
	 * @param newsgroups
	 * @param newzbinQueryProps
	 * @return a list of found (lazy)nzbs
	 * @throws SearchException
	 */
	List<NZB> search(String query, NzbsOrgsCategory category)
			throws SearchException;

}
