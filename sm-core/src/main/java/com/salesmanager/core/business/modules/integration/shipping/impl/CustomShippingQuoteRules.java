package com.salesmanager.core.business.modules.integration.shipping.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.drools.KnowledgeBase;
import org.drools.runtime.StatelessKnowledgeSession;
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
import static java.util.Arrays.asList;
import static java.util.Collections.max;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;


public class CustomShippingQuoteRules implements ShippingQuoteModule {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomShippingQuoteRules.class);
	
	
	private StatelessKnowledgeSession shippingPriceRule;
	
	public final static String MODULE_CODE = "customQuotesRules";
	
	private KnowledgeBase kbase;

	@Override
	public void validateModuleConfiguration(
			IntegrationConfiguration integrationConfiguration,
			MerchantStore store) throws IntegrationException {

    }

	@Override
	public CustomIntegrationConfiguration getCustomModuleConfiguration(
			MerchantStore store) throws IntegrationException {
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
        if (isBlank(delivery.getPostalCode())) {
            return null;
        }

        Double distance = null;

        if (quote != null) {
            if (quote.getQuoteInformations() != null) {
                if (quote.getQuoteInformations().containsKey(DISTANCE_KEY)) {
                    distance = (Double) quote.getQuoteInformations().get(DISTANCE_KEY);
                }
            }
        }
        Double volume = null;
        Double weight = 0D;
        Double size = null;
        for (PackageDetails pack : packages) {
            weight = weight + pack.getShippingWeight();
            Double tmpVolume = pack.getShippingHeight() * pack.getShippingLength() * pack.getShippingWidth();
            if (volume == null || tmpVolume.doubleValue() > volume.doubleValue()) {
                volume = tmpVolume;
            }
            List<Double> sizeList = new ArrayList<Double>();
            sizeList.add(pack.getShippingHeight());
            sizeList.add(pack.getShippingWeight());
            sizeList.add(pack.getShippingLength());
            Double maxSize = (Double) max(sizeList);
            if (size == null || maxSize.doubleValue() > size.doubleValue()) {
                size = maxSize.doubleValue();
            }
        }
        ShippingInputParameters inputParameters = new ShippingInputParameters();

        inputParameters.setWeight((long) weight.doubleValue());
        inputParameters.setCountry(delivery.getCountry().getIsoCode());
        inputParameters.setProvince("*");
        inputParameters.setModuleName(module.getCode());

        if (delivery.getZone().getCode() != null) {
            inputParameters.setProvince(delivery.getZone().getCode());
        }

        if (distance != null) {
            double ddistance = distance.doubleValue();
            long ldistance = (long) ddistance;
            inputParameters.setDistance(ldistance);
        }

        if (volume != null) {
            inputParameters.setVolume((long) volume.doubleValue());
        }

        List<ShippingOption> options = quote.getShippingOptions();

        if (options == null) {
            options = new ArrayList<ShippingOption>();
            quote.setShippingOptions(options);
        }


        LOGGER.debug("Setting input parameters " + inputParameters.toString());

        shippingPriceRule.execute(asList(new Object[]{inputParameters}));

        if (inputParameters.getPriceQuote() != null) {

            ShippingOption shippingOption = new ShippingOption();


            shippingOption.setOptionPrice(new BigDecimal(inputParameters.getPriceQuote()));
            shippingOption.setShippingModuleCode(MODULE_CODE);
            shippingOption.setOptionCode(MODULE_CODE);
            shippingOption.setOptionId(MODULE_CODE);

            options.add(shippingOption);
        }


        return options;


    }

	public StatelessKnowledgeSession getShippingPriceRule() {
		return shippingPriceRule;
	}

	public void setShippingPriceRule(StatelessKnowledgeSession shippingPriceRule) {
		this.shippingPriceRule = shippingPriceRule;
	}

	public KnowledgeBase getKbase() {
		return kbase;
	}

	public void setKbase(KnowledgeBase kbase) {
		this.kbase = kbase;
	}

}
