package com.salesmanager.core.business.modules.integration.shipping.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesmanager.core.model.common.Delivery;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.shipping.PackageDetails;
import com.salesmanager.core.model.shipping.ShippingConfiguration;
import com.salesmanager.core.model.shipping.ShippingOption;
import com.salesmanager.core.model.shipping.ShippingOrigin;
import com.salesmanager.core.model.shipping.ShippingQuote;
import com.salesmanager.core.model.system.CustomIntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.constants.Constants;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.shipping.model.ShippingQuoteModule;

import static com.salesmanager.core.modules.constants.Constants.DISTANCE_KEY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.slf4j.LoggerFactory.getLogger;


public class PriceByDistanceShippingQuoteRules implements ShippingQuoteModule {

    private static final Logger LOGGER = getLogger(PriceByDistanceShippingQuoteRules.class);

    public final static String MODULE_CODE = "priceByDistance";

    @Override
    public void validateModuleConfiguration(
            IntegrationConfiguration integrationConfiguration,
            MerchantStore store) throws IntegrationException {
        // Not used

    }

    @Override
    public CustomIntegrationConfiguration getCustomModuleConfiguration(
            MerchantStore store) throws IntegrationException {
        // Not used
        return null;
    }

    @Override
    public List<ShippingOption> getShippingQuotes(ShippingQuote quote,
                                                  List<PackageDetails> packages, BigDecimal orderTotal,
                                                  Delivery delivery, ShippingOrigin origin, MerchantStore store,
                                                  IntegrationConfiguration configuration, IntegrationModule module,
                                                  ShippingConfiguration shippingConfiguration, Locale locale)
            throws IntegrationException {


        notNull(delivery, "Delivery cannot be null");
        notNull(delivery.getCountry(), "Delivery.country cannot be null");
        notNull(packages, "packages cannot be null");
        notEmpty(packages, "packages cannot be empty");

        //requires the postal code
        if (isBlank(delivery.getPostalCode())) {
            return null;
        }

        Double distance = null;

        if (quote != null) {
            //look if distance has been calculated
            if (quote.getQuoteInformations() != null) {
                if (quote.getQuoteInformations().containsKey(DISTANCE_KEY)) {
                    distance = (Double) quote.getQuoteInformations().get(DISTANCE_KEY);
                }
            }
        }

        if (distance == null) {
            return null;
        }

        //maximum distance TODO configure from admin
        if (distance > 150D) {
            return null;
        }

        List<ShippingOption> options = quote.getShippingOptions();

        if (options == null) {
            options = new ArrayList<ShippingOption>();
            quote.setShippingOptions(options);
        }

        BigDecimal price = null;
        BigDecimal total = null;

        if (distance <= 20) {
            price = new BigDecimal(69);//TODO from the admin
            total = new BigDecimal(distance).multiply(price);
        } else {
            price = new BigDecimal(3);//TODO from the admin
            total = new BigDecimal(distance).multiply(price);
        }


        ShippingOption shippingOption = new ShippingOption();


        shippingOption.setOptionPrice(total);
        shippingOption.setShippingModuleCode(MODULE_CODE);
        shippingOption.setOptionCode(MODULE_CODE);
        shippingOption.setOptionId(MODULE_CODE);

        options.add(shippingOption);


        return options;


    }

}