package com.salesmanager.core.business.services.search;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.search.SearchKeywords;
import com.salesmanager.core.model.search.SearchResponse;

public interface SearchService {

    void index(MerchantStore store, Product product) throws ServiceException;

    void deleteIndex(MerchantStore store, Product product)
            throws ServiceException;

    SearchKeywords searchForKeywords(String collectionName,
                                     String jsonString, int entriesCount) throws ServiceException;

    SearchResponse search(MerchantStore store, String languageCode, String jsonString,
                          int entriesCount, int startIndex) throws ServiceException;

    void initService();

}
