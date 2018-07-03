package com.salesmanager.core.business.services.shipping;

import java.util.List;
import java.util.Map;


import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.model.common.Delivery;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.shipping.PackageDetails;
import com.salesmanager.core.model.shipping.ShippingConfiguration;
import com.salesmanager.core.model.shipping.ShippingMetaData;
import com.salesmanager.core.model.shipping.ShippingOption;
import com.salesmanager.core.model.shipping.ShippingProduct;
import com.salesmanager.core.model.shipping.ShippingQuote;
import com.salesmanager.core.model.shipping.ShippingSummary;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.CustomIntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;



public interface ShippingService {

    public List<String> getSupportedCountries(MerchantStore store)
            throws ServiceException;

	public  void setSupportedCountries(MerchantStore store,
			List<String> countryCodes) throws ServiceException;

    public List<IntegrationModule> getShippingMethods(MerchantStore store)
            throws ServiceException;


    Map<String, IntegrationConfiguration> getShippingModulesConfigured(
            MerchantStore store) throws ServiceException;

    void saveShippingQuoteModuleConfiguration(IntegrationConfiguration configuration,
                                              MerchantStore store) throws ServiceException;

    ShippingConfiguration getShippingConfiguration(MerchantStore store)
            throws ServiceException;

    void saveShippingConfiguration(ShippingConfiguration shippingConfiguration,
                                   MerchantStore store) throws ServiceException;

	void removeShippingQuoteModuleConfiguration(String moduleCode,
			MerchantStore store) throws ServiceException;

    List<PackageDetails> getPackagesDetails(List<ShippingProduct> products,
                                            MerchantStore store) throws ServiceException;

    ShippingQuote getShippingQuote(Long shoppingCartId, MerchantStore store, Delivery delivery,
                                   List<ShippingProduct> products, Language language)
            throws ServiceException;


    IntegrationConfiguration getShippingConfiguration(String moduleCode,
                                                      MerchantStore store) throws ServiceException;


    CustomIntegrationConfiguration getCustomShippingConfiguration(
            String moduleCode, MerchantStore store) throws ServiceException;

    void saveCustomShippingConfiguration(String moduleCode,
                                         CustomIntegrationConfiguration shippingConfiguration,
                                         MerchantStore store) throws ServiceException;

    void removeCustomShippingQuoteModuleConfiguration(String moduleCode,
                                                      MerchantStore store) throws ServiceException;

    ShippingSummary getShippingSummary(MerchantStore store, ShippingQuote shippingQuote,
                                       ShippingOption selectedShippingOption) throws ServiceException;

    List<Country> getShipToCountryList(MerchantStore store, Language language)
            throws ServiceException;

    boolean requiresShipping(List<ShoppingCartItem> items, MerchantStore store) throws ServiceException;

    ShippingMetaData getShippingMetaData(MerchantStore store) throws ServiceException;

    boolean hasTaxOnShipping(MerchantStore store) throws ServiceException;


}