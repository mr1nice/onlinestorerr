package com.salesmanager.shop.utils;

import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.merchant.MerchantStore;

public interface ImageFilePath {

    public String getContextPath();
	
	
	public String getBasePath();

    public String buildStaticImageUtils(MerchantStore store, String imageName);

    public String buildStaticImageUtils(MerchantStore store, String type, String imageName);

    public String buildManufacturerImageUtils(MerchantStore store, Manufacturer manufacturer, String imageName);

    public String buildProductImageUtils(MerchantStore store, Product product, String imageName);

    public String buildProductImageUtils(MerchantStore store, String sku, String imageName);

    public String buildLargeProductImageUtils(MerchantStore store, String sku, String imageName);


    public String buildStoreLogoFilePath(MerchantStore store);
	
	/**
	 * Builds product property image url path
	 * @param store
	 * @param imageName
	 * @return
	 */
	public String buildProductPropertyImageUtils(MerchantStore store, String imageName);


    public String buildStaticContentFilePath(MerchantStore store, String fileName);


}
