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

import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class UtilTest {

	
	@Test
	public void testSaveFilename() {
		assertEquals("a name WITH _ and -.com",Util.saveFileName("a name WITH _ and -.com"));
		assertEquals("what.Is.Left",Util.saveFileName("what:éIs\\//Left"));
		assertEquals("one.dot",Util.saveFileName("one.:é\\/.dot"));
		assertEquals("one.dot",Util.saveFileName("one.......dot"));
		assertEquals("Realy.weird",Util.saveFileName("Realy\n\tweird"));
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testImplodeEmpty() {
		assertEquals("",Util.implode(null, ","));
		assertEquals("",Util.implode(Collections.EMPTY_LIST, ","));
	}
	
	@Test
	public void testImplode() {
		assertEquals("Aap,Noot,Mies",Util.implode(Arrays.asList("Aap","Noot","Mies"), ","));
		assertEquals("Aap of noot of mies.",Util.implode(Arrays.asList("Aap","noot","mies."), " of "));
	}
}
