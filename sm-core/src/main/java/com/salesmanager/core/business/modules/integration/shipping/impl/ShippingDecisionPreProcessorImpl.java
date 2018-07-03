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
import com.salesmanager.core.model.shipping.ShippingOrigin;
import com.salesmanager.core.model.shipping.ShippingQuote;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.constants.Constants;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.shipping.model.ShippingQuotePrePostProcessModule;

import static com.salesmanager.core.modules.constants.Constants.DISTANCE_KEY;
import static java.lang.System.out;
import static java.util.Arrays.asList;
import static java.util.Collections.max;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.slf4j.LoggerFactory.getLogger;

public class ShippingDecisionPreProcessorImpl implements ShippingQuotePrePostProcessModule {

    private static final Logger LOGGER = getLogger(ShippingDecisionPreProcessorImpl.class);

    private final static String MODULE_CODE = "shippingDecisionModule";

    private StatelessKnowledgeSession shippingMethodDecision;

    private KnowledgeBase kbase;

    @Override
    public void prePostProcessShippingQuotes(ShippingQuote quote,
                                             List<PackageDetails> packages, BigDecimal orderTotal,
                                             Delivery delivery, ShippingOrigin origin, MerchantStore store,
                                             IntegrationConfiguration globalShippingConfiguration,
                                             IntegrationModule currentModule,
                                             ShippingConfiguration shippingConfiguration,
                                             List<IntegrationModule> allModules, Locale locale)
            throws IntegrationException {


        notNull(delivery, "Delivery cannot be null");
        notNull(currentModule, "IntegrationModule cannot be null");
        notNull(delivery.getCountry(), "Delivery.country cannot be null");
        notNull(allModules, "List<IntegrationModule> cannot be null");
        notNull(packages, "packages cannot be null");
        notEmpty(packages, "packages cannot be empty");

        Double distance = null;

        if (quote != null) {
            //look if distance has been calculated
            if (quote.getQuoteInformations() != null) {
                if (quote.getQuoteInformations().containsKey(DISTANCE_KEY)) {
                    distance = (Double) quote.getQuoteInformations().get(DISTANCE_KEY);
                }
            }
        }

        //calculate volume (L x W x H)
        Double volume = null;
        Double weight = 0D;
        Double size = null;
        //calculate weight, volume and largest size
        for (PackageDetails pack : packages) {
            weight = weight + pack.getShippingWeight();
            Double tmpVolume = pack.getShippingHeight() * pack.getShippingLength() * pack.getShippingWidth();
            if (volume == null || tmpVolume.doubleValue() > volume.doubleValue()) { //take the largest volume
                volume = tmpVolume;
            }
            //largest size
            List<Double> sizeList = new ArrayList<Double>();
            sizeList.add(pack.getShippingHeight());
            sizeList.add(pack.getShippingLength());
            sizeList.add(pack.getShippingWidth());
            Double maxSize = (Double) max(sizeList);
            if (size == null || maxSize.doubleValue() > size.doubleValue()) {
                size = maxSize.doubleValue();
            }
        }

        //Build a ShippingInputParameters
        ShippingInputParameters inputParameters = new ShippingInputParameters();

        inputParameters.setWeight((long) weight.doubleValue());
        inputParameters.setCountry(delivery.getCountry().getIsoCode());
        if (delivery.getZone() != null) {
            inputParameters.setProvince(delivery.getZone().getCode());
        } else {
            inputParameters.setProvince(delivery.getState());
        }
        //inputParameters.setModuleName(currentModule.getCode());

        if (delivery.getZone().getCode() != null) {
            inputParameters.setProvince(delivery.getZone().getCode());
        }

        if (size != null) {
            inputParameters.setSize((long) size.doubleValue());
        }

        if (distance != null) {
            double ddistance = distance.doubleValue();
            long ldistance = (long) ddistance;
            inputParameters.setDistance(ldistance);
        }

        if (volume != null) {
            inputParameters.setVolume((long) volume.doubleValue());
        }

        LOGGER.debug("Setting input parameters " + inputParameters.toString());
        out.println(inputParameters.toString());

        shippingMethodDecision.execute(asList(new Object[]{inputParameters}));

        LOGGER.debug("Using shipping nodule " + inputParameters.getModuleName());

        if (!isBlank(inputParameters.getModuleName())) {
            for (IntegrationModule toBeUsed : allModules) {
                if (toBeUsed.getCode().equals(inputParameters.getModuleName())) {
                    quote.setCurrentShippingModule(toBeUsed);
                    break;
                }
            }
        }

    }

    public StatelessKnowledgeSession getShippingMethodDecision() {
        return shippingMethodDecision;
    }

    public void setShippingMethodDecision(StatelessKnowledgeSession shippingMethodDecision) {
        this.shippingMethodDecision = shippingMethodDecision;
    }

    public KnowledgeBase getKbase() {
        return kbase;
    }

    public void setKbase(KnowledgeBase kbase) {
        this.kbase = kbase;
    }

    @Override
    public String getModuleCode() {
        return MODULE_CODE;
    }


}
