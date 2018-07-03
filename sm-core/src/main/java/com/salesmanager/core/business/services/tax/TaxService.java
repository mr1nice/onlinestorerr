package com.salesmanager.core.business.services.tax;

import java.util.List;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.OrderSummary;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.tax.TaxConfiguration;
import com.salesmanager.core.model.tax.TaxItem;


public interface TaxService   {

    TaxConfiguration getTaxConfiguration(MerchantStore store)
            throws ServiceException;

    void saveTaxConfiguration(TaxConfiguration shippingConfiguration,
                              MerchantStore store) throws ServiceException;

	List<TaxItem> calculateTax(OrderSummary orderSummary, Customer customer,
							   MerchantStore store, Language language) throws ServiceException;


}
