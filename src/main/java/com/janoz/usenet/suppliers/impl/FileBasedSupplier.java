/**
 * 
 */
package com.janoz.usenet.suppliers.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.janoz.usenet.LazyInitializationException;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.suppliers.LazyNZBSupplier;


/**
 * @author vriesgi
 *
 */
public class FileBasedSupplier implements LazyNZBSupplier {

	private static final Log LOG = LogFactory.getLog(FileBasedSupplier.class);

	private static final int BUFF_SIZE = 1024;

	private File directory;
	
	public FileBasedSupplier(File directory){
		this.directory = directory;
	}
	
	@Override
	public byte[] getData(NZB nzb){
		File nzbFile = new File(directory,nzb.getFilename());
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			InputStream input = new FileInputStream(nzbFile);
	        byte[] buffer = new byte[BUFF_SIZE];
	        int n;
	        while (-1 != (n = input.read(buffer))) {
	        	ba.write(buffer, 0, n);
	        }
			return ba.toByteArray();
		} catch (IOException e) {
			LOG.error("IO exception during fetching of the data.", e);
			throw new LazyInitializationException("Error loading data.",e);
		}
	}
}
