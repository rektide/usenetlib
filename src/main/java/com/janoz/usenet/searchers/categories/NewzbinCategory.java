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

public enum NewzbinCategory {
	ALL("All",-1),
	UNKNOWN("Unknown",0), 
	ANIME("Anime",11), 
	APPS("Apps",1), 
	BOOKS("Books",13), 
	CONSOLES("Consoles",2), 
	DISCUSSIONS("Discussions",15), 
	EMULATION("Emulation",10), 
	GAMES("Games",4), 
	MISC("Misc",5), 
	MOVIES("Movies",6), 
	MUSIC("Music",7), 
	PDA("PDA",12),
	RESOURCES("Resources",14), 
	TV("TV",8);

	private final String label;
	private final int value;

	NewzbinCategory(String label,int value) {
		this.label = label;
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public String getLabel() {
		return label;
	}
}
