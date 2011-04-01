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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.janoz.usenet.RetrieveException;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.processors.NZBProcessor;


/**
 * @author Gijs de Vries
 *
 */
public class NZBSaveProcessor implements NZBProcessor {

	private File targetDir;
	
	public File getTargetDir() {
		return targetDir;
	}

	public void setTargetDir(String targetDir) {
		this.targetDir = new File(targetDir);
	}


	public void setTargetDir(File targetDir) {
		this.targetDir = targetDir;
	}


	
	/**
	 * Saves the NZB data to an nzb file in the configured directory.
	 * 
	 * @param nzb The NZB object to be saved
	 * 
	 */
	@Override
	public void processNZB(NZB nzb) throws RetrieveException {
		File nzbFile = new File(targetDir, nzb.getFilename());
		try {
			if (!nzbFile.createNewFile()) {
				throw new RetrieveException("File already existed in "
						+ "target directory.");
			}
			OutputStream os = new FileOutputStream(nzbFile);
			os.write(nzb.getData());
			os.close();
		} catch (IOException e) {
			throw new RetrieveException("Error writing '"
					+ nzb.getFilename() + "'.", e);
		}
	}

	@Override
	public void validate() throws RetrieveException {
		if (targetDir == null) {
			throw new RetrieveException("Targetdir not set.");
		}
		if (!targetDir.exists()) {
			throw new RetrieveException(targetDir.getAbsolutePath() 
					+ " doesn't exist.");
		}
		if (!targetDir.isDirectory()) {
			throw new RetrieveException(targetDir.getAbsolutePath() 
					+ " is not a directory.");
		}
		try {
			File tmpfile = File.createTempFile("tmp", "", targetDir);
			if (!tmpfile.delete()) {
				tmpfile.deleteOnExit();
			}
		} catch (IOException e) {
			throw new RetrieveException("Unable to create files in "
					+ targetDir.getAbsolutePath() + ".", e);
		}
	}

}
