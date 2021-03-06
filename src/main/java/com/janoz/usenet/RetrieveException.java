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
package com.janoz.usenet;

public class RetrieveException extends RuntimeException {

	private static final long serialVersionUID = -6709575615296117728L;


	public RetrieveException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetrieveException(String message) {
		super(message);
	}

}
