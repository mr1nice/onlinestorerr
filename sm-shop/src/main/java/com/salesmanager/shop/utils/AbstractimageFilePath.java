package com.salesmanager.shop.utils;

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;

import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.constants.Constants;

import static com.salesmanager.core.model.content.FileContentType.*;
import static com.salesmanager.shop.constants.Constants.*;
import static org.apache.commons.lang3.StringUtils.isBlank;


public abstract class AbstractimageFilePath implements ImageFilePath {


	public abstract String getBasePath();

	public abstract void setBasePath(String basePath);
	
	protected static final String CONTEXT_PATH = "CONTEXT_PATH";

    public @Resource(name = "shopizer-properties")
    Properties properties = new Properties();

	
	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

    public String buildStaticImageUtils(MerchantStore store, String imageName) {
        StringBuilder imgName = new StringBuilder().append(getBasePath()).append(FILES_URI).append(SLASH).append(store.getCode()).append(SLASH).append(IMAGE.name()).append(SLASH);
        if (!isBlank(imageName)) {
            imgName.append(imageName);
        }
        return imgName.toString();

    }

    public String buildStaticImageUtils(MerchantStore store, String type, String imageName) {
        StringBuilder imgName = new StringBuilder().append(getBasePath()).append(FILES_URI).append(SLASH).append(store.getCode()).append(SLASH).append(type).append(SLASH);
        if (!isBlank(imageName)) {
            imgName.append(imageName);
        }
        return imgName.toString();

    }

    public String buildManufacturerImageUtils(MerchantStore store, Manufacturer manufacturer, String imageName) {
        return new StringBuilder().append(getBasePath()).append(SLASH).append(store.getCode()).append(SLASH).
                append(MANUFACTURER.name()).append(SLASH)
                .append(manufacturer.getId()).append(SLASH)
                .append(imageName).toString();
    }

    public String buildProductImageUtils(MerchantStore store, Product product, String imageName) {
        return new StringBuilder().append(getBasePath()).append(PRODUCTS_URI).append(SLASH).append(store.getCode()).append(SLASH).append(SLASH)
                .append(product.getSku()).append(SLASH).append(SMALL_IMAGE).append(SLASH).append(imageName).toString();
    }

    public String buildProductImageUtils(MerchantStore store, String sku, String imageName) {
        return new StringBuilder().append(getBasePath()).append(PRODUCTS_URI).append(SLASH).append(store.getCode()).append(SLASH)
                .append(sku).append(SLASH).append(SMALL_IMAGE).append(SLASH).append(imageName).toString();
    }

    public String buildLargeProductImageUtils(MerchantStore store, String sku, String imageName) {
        return new StringBuilder().append(getBasePath()).append(SLASH).append(store.getCode()).append(SLASH)
                .append(sku).append(SLASH).append(SMALL_IMAGE).append(SLASH).append(imageName).toString();
    }


    public String buildStoreLogoFilePath(MerchantStore store) {
        return new StringBuilder().append(getBasePath()).append(FILES_URI).append(SLASH).append(store.getCode()).append(SLASH).append(LOGO).append(SLASH)
                .append(store.getStoreLogo()).toString();
    }

    public String buildProductPropertyImageFilePath(MerchantStore store, String imageName) {
        return new StringBuilder().append(getBasePath()).append(SLASH).append(store.getCode()).append(SLASH).append(PROPERTY).append(SLASH)
                .append(imageName).toString();
    }
	
	public String buildProductPropertyImageUtils(MerchantStore store, String imageName) {
		return new StringBuilder().append(getBasePath()).append(Constants.FILES_URI).append(Constants.SLASH).append(store.getCode()).append("/").append(FileContentType.PROPERTY).append("/")
				.append(imageName).toString();
	}

    public String buildStaticContentFilePath(MerchantStore store, String fileName) {
        return new StringBuilder().append(getBasePath()).append(FILES_URI).append(SLASH).append(store.getCode()).append(SLASH).append(fileName).toString();
    }
	

	
	


}
