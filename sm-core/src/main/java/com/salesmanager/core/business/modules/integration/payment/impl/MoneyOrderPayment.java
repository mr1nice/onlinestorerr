package com.salesmanager.core.business.modules.integration.payment.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.payments.Payment;
import com.salesmanager.core.model.payments.PaymentType;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.payments.TransactionType;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;

import static com.salesmanager.core.model.payments.PaymentType.MONEYORDER;
import static com.salesmanager.core.model.payments.TransactionType.AUTHORIZECAPTURE;
import static com.salesmanager.core.modules.integration.IntegrationException.ERROR_VALIDATION_SAVE;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class MoneyOrderPayment implements PaymentModule {

    @Override
    public void validateModuleConfiguration(
            IntegrationConfiguration integrationConfiguration,
            MerchantStore store) throws IntegrationException {

        List<String> errorFields = null;


        Map<String, String> keys = integrationConfiguration.getIntegrationKeys();
        if (keys == null || isBlank(keys.get("address"))) {
            errorFields = new ArrayList<String>();
            errorFields.add("address");
        }

        if (errorFields != null) {
            IntegrationException ex = new IntegrationException(ERROR_VALIDATION_SAVE);
            ex.setErrorFields(errorFields);
            throw ex;

        }


        return;

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
    public Transaction authorizeAndCapture(MerchantStore store, Customer customer,
                                           List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
                                           IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {


        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionDate(new Date());
        transaction.setTransactionType(AUTHORIZECAPTURE);
        transaction.setPaymentType(MONEYORDER);


        return transaction;


    }

    @Override
    public Transaction refund(boolean partial, MerchantStore store, Transaction transaction,
                              Order order, BigDecimal amount,
                              IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {
        throw new IntegrationException("Transaction not supported");
    }

    @Override
    public Transaction capture(MerchantStore store, Customer customer,
                               Order order, Transaction capturableTransaction,
                               IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {
        return null;
    }

}