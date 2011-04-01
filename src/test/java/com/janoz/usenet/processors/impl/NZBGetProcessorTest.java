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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import com.janoz.usenet.processors.impl.NZBGetProcessor;

public class NZBGetProcessorTest {

	NZBGetProcessor subject;
	
	
	@Before
	public void setup() {
		subject = new NZBGetProcessor();
	}
	
	
	
	
	@Test
	public void testStripTags() {
 		assertEquals("Error:\nCheck the path and the permissions for the upload directory (option nzbdir", subject.stripTags("<b><font color=red>Error:</font></b>\nCheck the path and the permissions for the upload directory (option <b>nzbdir</b>"));
 		assertEquals("", subject.stripTags(""));
 		assertEquals("File upload OK ", subject.stripTags("<b><font color=green>File upload OK </font></b><br>"));
	}
}
