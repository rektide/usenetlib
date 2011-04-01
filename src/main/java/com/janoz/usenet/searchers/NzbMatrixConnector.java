package com.janoz.usenet.searchers;

import java.util.List;
import java.util.Set;

import com.janoz.usenet.SearchException;
import com.janoz.usenet.model.NZB;
import com.janoz.usenet.searchers.categories.MatrixCategory;
import com.janoz.usenet.suppliers.LazyNZBSupplier;

public interface NzbMatrixConnector extends LazyNZBSupplier {
	
	
	void fillNzbByMatrixId(NZB nzb) throws SearchException;
	
	List<NZB> search(String query, MatrixCategory category) throws SearchException;
	
	public Set<String> getSearchParams();

}
