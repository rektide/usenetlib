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
package com.janoz.usenet.model;

import java.util.Date;

/**
 * @author Gijs de Vries
 *
 */
public class NZB {

	private byte[] data;
	private String filename;
	//reportId, matrixId and name aren't manditory
	private Integer reportId = null;
	private Integer matrixId = null;
	private String name;
	private Date postDate;
	private String url;

	public NZB(String filename) {
		this.filename = filename;
	}
	
	public byte[] getData() {
//		if (this.data == null) {
//			return null;
//		} else {
//			int length = this.data.length;
//			byte[] result = new byte[length];
//			System.arraycopy(this.data, 0, result, 0, length);
//			return result;
//		}
		return data;
	}

	public void setData(byte[] data) {
//		if (data == null) {
//			this.data = null;
//		} else {
//			this.data = data.clone();
//		}
		this.data=data;
	}

	public String getFilename() {
		if (filename.endsWith(".nzb")) {
			return filename;
		} else {
			return filename + ".nzb";
		}
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getName() {
		return name == null ? filename : name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getReportId() {
		return reportId;
	}

	public void setReportId(Integer reportId) {
		this.reportId = reportId;
	}

	public Integer getMatrixId() {
		return matrixId;
	}

	public void setMatrixId(Integer matrixId) {
		this.matrixId = matrixId;
	}

	public Date getPostDate() {
		return postDate == null?null:(Date)postDate.clone();
	}

	public void setPostDate(Date postDate) {
		this.postDate = postDate == null?null:(Date)postDate.clone();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
