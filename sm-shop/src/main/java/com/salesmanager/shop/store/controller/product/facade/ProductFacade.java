package com.salesmanager.shop.store.controller.product.facade;

import java.util.List;

import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.ProductCriteria;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.catalog.product.review.ProductReview;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.catalog.manufacturer.ReadableManufacturer;
import com.salesmanager.shop.model.catalog.product.PersistableProduct;
import com.salesmanager.shop.model.catalog.product.PersistableProductReview;
import com.salesmanager.shop.model.catalog.product.ProductPriceEntity;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.catalog.product.ReadableProductList;
import com.salesmanager.shop.model.catalog.product.ReadableProductReview;

public interface ProductFacade {
	
	PersistableProduct saveProduct(MerchantStore store, PersistableProduct product, Language language) throws Exception;

    ReadableProduct getProduct(MerchantStore store, Long id, Language language) throws Exception;


    ReadableProduct getProductByCode(MerchantStore store, String uniqueCode, Language language) throws Exception;

    ReadableProduct getProduct(MerchantStore store, String sku, Language language) throws Exception;

    ReadableProduct updateProductPrice(ReadableProduct product, ProductPriceEntity price, Language language) throws Exception;

    ReadableProduct updateProductQuantity(ReadableProduct product, int quantity, Language language) throws Exception;

    void deleteProduct(Product product) throws Exception;


    ReadableProductList getProductListsByCriterias(MerchantStore store, Language language, ProductCriteria criterias) throws Exception;


    ReadableProduct addProductToCategory(Category category, Product product, Language language) throws Exception;

    ReadableProduct removeProductFromCategory(Category category, Product product, Language language) throws Exception;


    void saveOrUpdateReview(PersistableProductReview review, MerchantStore store, Language language) throws Exception;

    void deleteReview(ProductReview review, MerchantStore store, Language language) throws Exception;

    List<ReadableProductReview> getProductReviews(Product product, MerchantStore store, Language language) throws Exception;

    void saveOrUpdateManufacturer(PersistableManufacturer manufacturer, MerchantStore store, Language language) throws Exception;
	
	/**
	 * Deletes a manufacturer
	 * @param manufacturer
	 * @param store
	 * @param language
	 * @throws Exception
	 */
	void deleteManufacturer(Manufacturer manufacturer, MerchantStore store, Language language) throws Exception;

    ReadableManufacturer getManufacturer(Long id, MerchantStore store, Language language) throws Exception;

    List<ReadableManufacturer> getAllManufacturers(MerchantStore store, Language language) throws Exception;
}
