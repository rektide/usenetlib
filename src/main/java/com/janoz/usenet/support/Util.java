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

import java.util.Collection;
import java.util.Iterator;

public final class Util {

	private Util(){
		///not intended to be instantiated
	}
	
	/**
	 * Converts a string to a save filename. Only digits, standard letters, 
	 * dashes, spases and the underscore ([-_ a-zA-Z0-9]) are preserved. Other
	 * characters are replaced by dots (.). Multiple dots are folded into one dot.
	 * 
	 * @param src the source String
	 * @return the converted String
	 */
	public static String saveFileName(String src) {
		return src
			//.replaceAll(" ","_")
			.replaceAll("[^\\w-_ ]+", ".");
	}
	
	/**
	 * Implodes a collection of strings.
	 * @param list a collection of strings
	 * @param separator the separater to be used
	 * @return a single string containing all elements separated by 
	 * 		the separator or an empty string if the collection was 
	 * 		null or empty.
	 */
	public static String implode(Collection<String> list, 
			String separator) {
		if (list == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		Iterator<String> i = list.iterator();
		while (i.hasNext()) {
			sb.append(i.next());
			if (i.hasNext()) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}
}
