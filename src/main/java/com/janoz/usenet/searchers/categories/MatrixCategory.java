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

public enum MatrixCategory {
	
	ALL("All","0"),
	MOVIES ("Movies", "movies-all"), 
	MOVIES_DVD ("Movies: DVD", "1"),
	MOVIES_DIVX_XVID ("Movies: Divx/Xvid", "2"), 
	MOVIES_BRRIP ("Movies: BRRip", "54"), 
	MOVIES_HD_X264 ("Movies: HD (x264)", "42"), 
	MOVIES_HD_IMG ("Movies: HD (Image)", "50"), 
	MOVIES_WMV_HD ("Movies: WMV-HD", "48"), 
	MOVIES_SVCD_VCD ("Movies: SVCD/VCD", "3"), 
	MOVIES_OTHER ("Movies: Other", "4"), 
	TV ("TV", "tv-all"), 
	TV_DVD ("TV: DVD", "5"), 
	TV_DIVX_XVID ("TV: Divx/Xvid", "6"), 
	TV_HD ("TV: HD", "41"), 
	TV_SPORT_ENT ("TV: Sport/Ent", "7"), 
	TV_OTHER ("TV: Other", "8"), 
	DOCUMENTARIES ("Documentaries", "docu-all"), 
	DOCUMENTARIES_STD ("Documentaries: STD", "9"), 
	DOCUMENTARIES_HD ("Documentaries: HD", "53"), 
	GAMES ("Games", "games-all"), 
	GAMES_PC ("Games: PC", "10"), 
	GAMES_PS2 ("Games: PS2", "11"), 
	GAMES_PS3 ("Games: PS3", "43"), 
	GAMES_PSP ("Games: PSP", "12"), 
	GAMES_XBOX ("Games: XBox", "13"), 
	GAMES_XBOX360 ("Games: XBox 360", "14"), 
	GAMES_XBOX360_OTHER ("Games: XBox 360 (other)", "56"), 
	GAMES_PS1 ("Games: PS1", "15"), 
	GAMES_DREAMCAST ("Games: Dreamcast", "16"), 
	GAMES_WII ("Games: Wii", "44"), 
	GAMES_WII_VC ("Games: Wii VC", "51"), 
	GAMES_DS ("Games: DS", "45"), 
	GAMES_GAMECUBE ("Games: GameCube", "46"), 
	GAMES_OTHER ("Games: Other", "17"), 
	APPS ("Apps", "apps-all"), 
	APPS_PC ("Apps: PC", "18"), 
	APPS_MAC ("Apps: Mac", "19"), 
	APPS_PORTABLE ("Apps: Portable", "51"), 
	APPS_LINUX ("Apps: Linux", "20"), 
	APPS_PHONE ("Apps: Phone", "55"), 
	APPS_OTHER ("Apps: Other", "21"), 
	MUSIC ("Music", "music-all"), 
	MUSIC_ALBUMS ("Music: MP3 Albums", "22"), 
	MUSIC_SINGLES ("Music: MP3 Singles", "47"), 
	MUSIC_LOSSLESS ("Music: Lossless", "23"), 
	MUSIC_DVD ("Music: DVD", "24"), 
	MUSIC_VIDEO ("Music: Video", "25"), 
	MUSIC_OTHER ("Music: Other", "27"), 
	ANIME ("Anime", "28"), 
	OTHER ("Other", "other-all"), 
	OTHER_AUDIO_BOOKS ("Other: Audio Books", "49"), 
	OTHER_EMULATION ("Other: Emulation", "33"), 
	OTHER_PPC ("Other: PPC/PDA", "34"), 
	OTHER_RADIO ("Other: Radio", "26"), 
	OTHER_EBOOK ("Other: E-Books", "36"), 
	OTHER_IMAGES ("Other: Images", "37"), 
	OTHER_MOBILE ("Other: Mobile Phone", "38"), 
	OTHER_PARS_FILLS ("Other: Extra Pars/Fills", "39"), 
	OTHER_OTHER ("Other: Other", "40");
	
	private final String label;
	private final String value;
	
	MatrixCategory(String label, String value) {
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
