package com.salesmanager.shop.store.controller.items.facade;

import java.util.List;

import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.catalog.product.ReadableProductList;

public interface ProductItemsFacade {

    ReadableProductList listItemsByManufacturer(MerchantStore store, Language language, Long manufacturerId, int startCount, int maxCount) throws Exception;

    ReadableProductList listItemsByIds(MerchantStore store, Language language, List<Long> ids, int startCount, int maxCount) throws Exception;


    ReadableProductList listItemsByGroup(String group, MerchantStore store, Language language) throws Exception;

    ReadableProductList addItemToGroup(Product product, String group, MerchantStore store, Language language) throws Exception;

    ReadableProductList removeItemFromGroup(Product product, String group, MerchantStore store, Language language) throws Exception;
	
	

}
