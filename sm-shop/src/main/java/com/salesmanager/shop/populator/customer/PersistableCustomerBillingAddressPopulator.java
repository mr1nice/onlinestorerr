package com.salesmanager.shop.populator.customer;

import org.apache.commons.lang.StringUtils;

import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.business.utils.AbstractDataPopulator;
import com.salesmanager.shop.model.customer.Address;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PersistableCustomerBillingAddressPopulator extends AbstractDataPopulator<Address, Customer>
{

    @Override
    public Customer populate( Address source, Customer target, MerchantStore store, Language language )
        throws ConversionException {


        target.getBilling().setFirstName(source.getFirstName());
        target.getBilling().setLastName(source.getLastName());

        if (isNotBlank(source.getAddress())) {
            target.getBilling().setAddress(source.getAddress());
        }

        if (isNotBlank(source.getCity())) {
            target.getBilling().setCity(source.getCity());
        }

        if (isNotBlank(source.getCompany())) {
            target.getBilling().setCompany(source.getCompany());
        }

        if (isNotBlank(source.getPhone())) {
            target.getBilling().setTelephone(source.getPhone());
        }

        if (isNotBlank(source.getPostalCode())) {
            target.getBilling().setPostalCode(source.getPostalCode());
        }

        if (isNotBlank(source.getStateProvince())) {
            target.getBilling().setState(source.getStateProvince());
        }

        return target;

    }

    @Override
    protected Customer createTarget()
    {
         return null;
    }

   

}
