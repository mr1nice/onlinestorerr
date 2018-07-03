package com.salesmanager.core.business.services.payments;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.payments.CreditCardType;
import com.salesmanager.core.model.payments.Payment;
import com.salesmanager.core.model.payments.PaymentMethod;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;

public interface PaymentService {



	public List<IntegrationModule> getPaymentMethods(MerchantStore store)
			throws ServiceException;

	Map<String, IntegrationConfiguration> getPaymentModulesConfigured(
			MerchantStore store) throws ServiceException;
	
	Transaction processPayment(Customer customer, MerchantStore store, Payment payment, List<ShoppingCartItem> items, Order order) throws ServiceException;
	Transaction processRefund(Order order, Customer customer, MerchantStore store, BigDecimal amount) throws ServiceException;

    IntegrationModule getPaymentMethodByType(MerchantStore store, String type)
            throws ServiceException;

    IntegrationModule getPaymentMethodByCode(MerchantStore store, String name)
            throws ServiceException;

    void savePaymentModuleConfiguration(IntegrationConfiguration configuration,
                                        MerchantStore store) throws ServiceException;

    void validateCreditCard(String number, CreditCardType creditCard, String month, String date)
            throws ServiceException;

    IntegrationConfiguration getPaymentConfiguration(String moduleCode,
                                                     MerchantStore store) throws ServiceException;

	void removePaymentModuleConfiguration(String moduleCode, MerchantStore store)
			throws ServiceException;

	Transaction processCapturePayment(Order order, Customer customer,
			MerchantStore store)
			throws ServiceException;

    Transaction initTransaction(Order order, Customer customer, Payment payment, MerchantStore store) throws ServiceException;

    Transaction initTransaction(Customer customer, Payment payment, MerchantStore store) throws ServiceException;

	List<PaymentMethod> getAcceptedPaymentMethods(MerchantStore store)
			throws ServiceException;

    PaymentModule getPaymentModule(String paymentModuleCode)
            throws ServiceException;

}