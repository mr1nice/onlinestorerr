package com.salesmanager.core.business.services.catalog.product.relationship;

import java.util.List;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.common.generic.SalesManagerEntityService;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.relationship.ProductRelationship;
import com.salesmanager.core.model.catalog.product.relationship.ProductRelationshipType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;

public interface ProductRelationshipService extends
		SalesManagerEntityService<Long, ProductRelationship> {

	void saveOrUpdate(ProductRelationship relationship) throws ServiceException;

    List<ProductRelationship> getByType(MerchantStore store, Product product,
                                        ProductRelationshipType type, Language language) throws ServiceException;

    List<ProductRelationship> getByType(MerchantStore store, Product product,
                                        ProductRelationshipType type)
            throws ServiceException;

    List<ProductRelationship> getByType(MerchantStore store,
                                        ProductRelationshipType type) throws ServiceException;

	List<ProductRelationship> listByProduct(Product product)
			throws ServiceException;

	List<ProductRelationship> getByType(MerchantStore store,
			ProductRelationshipType type, Language language)
			throws ServiceException;

    List<ProductRelationship> getGroups(MerchantStore store);

    void addGroup(MerchantStore store, String groupName) throws ServiceException;

	List<ProductRelationship> getByGroup(MerchantStore store, String groupName)
			throws ServiceException;

	void deleteGroup(MerchantStore store, String groupName)
			throws ServiceException;

	void deactivateGroup(MerchantStore store, String groupName)
			throws ServiceException;

	void activateGroup(MerchantStore store, String groupName)
			throws ServiceException;

	List<ProductRelationship> getByGroup(MerchantStore store, String groupName,
			Language language) throws ServiceException;

}
