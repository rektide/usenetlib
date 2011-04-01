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

import com.janoz.usenet.suppliers.LazyNZBSupplier;


/**
 * @author Gijs de Vries
 *
 */
public class LazyNZB extends NZB {

	private LazyNZBSupplier supplier;

	public LazyNZB(String filename, LazyNZBSupplier supplier) {
		super(filename);
		this.supplier = supplier;
	}

	@Override
	public byte[] getData() {
		byte[] data = super.getData();
		if (data == null) {
			data = supplier.getData(this);
			super.setData(data);
		}
		return data;
	}
	
	
}
