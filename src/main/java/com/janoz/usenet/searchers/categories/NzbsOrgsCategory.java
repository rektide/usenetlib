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
package com.janoz.usenet.searchers.categories;

public enum NzbsOrgsCategory {
	ALL("All Categories","0"),
	CONSOLE("Console","t6"),
	CONSOLE_NDS("Console-NDS","19"),
	CONSOLE_PSP("Console-PSP","16"),
	CONSOLE_WII("Console-Wii","17"),
	CONSOLE_XBOX("Console-XBox","8"),
	CONSOLE_XBOX360("Console-XBox360","20"),
	MOVIES("Movies","t2"),
	MOVIES_DVD("Movies-DVD","9"),
	MOVIES_WMV_HD("Movies-WMV-HD","12"),
	MOVIES_X264("Movies-x264","4"),
	MOVIES_XVID("Movies-XviD","2"),
	MUSIC("Music","t3"),
	MUSIC_MP3("Music-MP3","5"),
	MUSIC_VIDEO("Music-Video","10"),
	PC("PC","t5"),
	PC_0DAY("PC-0day","7"),
	PC_ISO("PC-ISO","6"),
	PC_MAC("PC-Mac","15"),
	TV("TV","t1"),
	TV_DVD("TV-DVD","11"),
	TV_H264("TV-H264","22"),
	TV_SWE("TV-SWE","24"),
	TV_X264("TV-x264","14"),
	TV_XVID("TV-XviD","1"),
	XXX("XXX","t4"),
	XXX_DVD("XXX-DVD","13"),
	XXX_WMV("XXX-WMV","21"),
	XXX_X264("XXX-x264","23"),
	XXX_XVID("XXX-XviD","3");
	
	
	private final String label;
	private final String value;
	
	NzbsOrgsCategory (String label, String value) {
		this.value = value;
		this.label = label;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getLabel() {
		return label;
	}
}
