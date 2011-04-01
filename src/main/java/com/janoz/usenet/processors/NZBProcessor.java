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
package com.janoz.usenet.processors;

import com.janoz.usenet.RetrieveException;
import com.janoz.usenet.model.NZB;

/**
 * 
 * @author Gijs de Vries
 *
 */
public interface NZBProcessor {

	/**
	 * Processes an NZB objects. This might be downloading
	 * it or storing it.
	 * @param nzb The to be processed NZB object.
	 * @throws RetrieveException 
	 */
	void processNZB(NZB nzb) throws RetrieveException;
	
	/**
	 * Validates the configuration of this object. Throws an exception 
	 * if the configuration has errors.
	 * @throws RetrieveException 
	 */
	void validate() throws RetrieveException;

}
