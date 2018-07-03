package com.salesmanager.core.business.modules.integration.payment.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.payments.Payment;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;

import static com.salesmanager.core.modules.integration.IntegrationException.ERROR_VALIDATION_SAVE;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class PayPalRestPayment implements PaymentModule {


    @Override
    public void validateModuleConfiguration(
            IntegrationConfiguration integrationConfiguration,
            MerchantStore store) throws IntegrationException {


        List<String> errorFields = null;
        Map<String, String> keys = integrationConfiguration.getIntegrationKeys();
        if (keys == null || isBlank(keys.get("client"))) {
            errorFields = new ArrayList<String>();
            errorFields.add("client");
        }

        if (keys == null || isBlank(keys.get("secret"))) {
            if (errorFields == null) {
                errorFields = new ArrayList<String>();
            }
            errorFields.add("secret");
        }


        if (errorFields != null) {
            IntegrationException ex = new IntegrationException(ERROR_VALIDATION_SAVE);
            ex.setErrorFields(errorFields);
            throw ex;

        }

    }

    @Override
    public Transaction initTransaction(MerchantStore store, Customer customer,
                                       BigDecimal amount, Payment payment,
                                       IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {
        return null;
    }

    @Override
    public Transaction authorize(MerchantStore store, Customer customer,
                                 List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
                                 IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {

        return null;


    }

    @Override
    public Transaction authorizeAndCapture(MerchantStore store,
                                           Customer customer, List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
                                           IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {
        return null;
    }

    @Override
    public Transaction refund(boolean partial, MerchantStore store,
                              Transaction transaction, Order order, BigDecimal amount,
                              IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {
        return null;
    }

    private String getAccessToken(String clientID, String clientSecret) throws Exception {

        return null;
    }

    @Override
    public Transaction capture(MerchantStore store, Customer customer,
                               Order order, Transaction capturableTransaction,
                               IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {
        return null;
    }

}
