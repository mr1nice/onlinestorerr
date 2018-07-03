package com.salesmanager.core.business.services.payments;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.order.OrderService;
import com.salesmanager.core.business.services.reference.loader.ConfigurationModulesLoader;
import com.salesmanager.core.business.services.system.MerchantConfigurationService;
import com.salesmanager.core.business.services.system.ModuleConfigurationService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.OrderTotal;
import com.salesmanager.core.model.order.OrderTotalType;
import com.salesmanager.core.model.order.orderstatus.OrderStatus;
import com.salesmanager.core.model.order.orderstatus.OrderStatusHistory;
import com.salesmanager.core.model.payments.CreditCardPayment;
import com.salesmanager.core.model.payments.CreditCardType;
import com.salesmanager.core.model.payments.Payment;
import com.salesmanager.core.model.payments.PaymentMethod;
import com.salesmanager.core.model.payments.PaymentType;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.payments.TransactionType;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.model.system.MerchantConfiguration;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;
import com.salesmanager.core.modules.utils.Encryption;

import static com.salesmanager.core.business.constants.Constants.OT_REFUND_MODULE_CODE;
import static com.salesmanager.core.business.constants.Constants.OT_TOTAL_MODULE_CODE;
import static com.salesmanager.core.business.exception.ServiceException.EXCEPTION_VALIDATION;
import static com.salesmanager.core.business.services.reference.loader.ConfigurationModulesLoader.loadIntegrationConfigurations;
import static com.salesmanager.core.business.services.reference.loader.ConfigurationModulesLoader.toJSONString;
import static com.salesmanager.core.model.order.OrderTotalType.REFUND;
import static com.salesmanager.core.model.order.orderstatus.OrderStatus.*;
import static com.salesmanager.core.model.payments.CreditCardType.*;
import static com.salesmanager.core.model.payments.PaymentType.MONEYORDER;
import static com.salesmanager.core.model.payments.PaymentType.fromString;
import static com.salesmanager.core.model.payments.TransactionType.*;
import static java.lang.Character.getNumericValue;
import static java.lang.Integer.parseInt;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;


@Service("paymentService")
public class PaymentServiceImpl implements PaymentService {
	
	

	private final static String PAYMENT_MODULES = "PAYMENT";
	
	@Inject
	private MerchantConfigurationService merchantConfigurationService;
	
	@Inject
	private ModuleConfigurationService moduleConfigurationService;
	
	@Inject
	private TransactionService transactionService;
	
	@Inject
	private OrderService orderService;
	
	@Inject
	private CoreConfiguration coreConfiguration;
	
	@Inject
	@Resource(name="paymentModules")
	private Map<String,PaymentModule> paymentModules;
	
	@Inject
	private Encryption encryption;
	
	@Override
	public List<IntegrationModule> getPaymentMethods(MerchantStore store) throws ServiceException {
		
		List<IntegrationModule> modules =  moduleConfigurationService.getIntegrationModules(PAYMENT_MODULES);
		List<IntegrationModule> returnModules = new ArrayList<IntegrationModule>();
		
		for(IntegrationModule module : modules) {
			if(module.getRegionsSet().contains(store.getCountry().getIsoCode())
					|| module.getRegionsSet().contains("*")) {
				
				returnModules.add(module);
			}
		}
		
		return returnModules;
	}
	
	@Override
	public List<PaymentMethod> getAcceptedPaymentMethods(MerchantStore store) throws ServiceException {
		
		Map<String,IntegrationConfiguration> modules =  this.getPaymentModulesConfigured(store);

		List<PaymentMethod> returnModules = new ArrayList<PaymentMethod>();
		
		for(String module : modules.keySet()) {
			IntegrationConfiguration config = modules.get(module);
			if(config.isActive()) {

                IntegrationModule md = this.getPaymentMethodByCode(store, config.getModuleCode());
                if (md == null) {
                    continue;
                }
                PaymentMethod paymentMethod = new PaymentMethod();

                paymentMethod.setDefaultSelected(config.isDefaultSelected());
                paymentMethod.setPaymentMethodCode(config.getModuleCode());
                paymentMethod.setModule(md);
                paymentMethod.setInformations(config);

                PaymentType type = fromString(md.getType());
                paymentMethod.setPaymentType(type);
                returnModules.add(paymentMethod);
            }
		}
		
		return returnModules;
		
		
	}
	
	@Override
	public IntegrationModule getPaymentMethodByType(MerchantStore store, String type) throws ServiceException {
		List<IntegrationModule> modules =  getPaymentMethods(store);

		for(IntegrationModule module : modules) {
			if(module.getModule().equals(type)) {
				
				return module;
			}
		}
		
		return null;
	}
	
	@Override
	public IntegrationModule getPaymentMethodByCode(MerchantStore store,
			String code) throws ServiceException {
		List<IntegrationModule> modules =  getPaymentMethods(store);

		for(IntegrationModule module : modules) {
			if(module.getCode().equals(code)) {
				
				return module;
			}
		}
		
		return null;
	}
	
	@Override
	public IntegrationConfiguration getPaymentConfiguration(String moduleCode, MerchantStore store) throws ServiceException {

		Validate.notNull(moduleCode,"Module code must not be null");
		Validate.notNull(store,"Store must not be null");
		
		String mod = moduleCode.toLowerCase();
		
		Map<String,IntegrationConfiguration> configuredModules = getPaymentModulesConfigured(store);
		if(configuredModules!=null) {
			for(String key : configuredModules.keySet()) {
				if(key.equals(mod)) {
					return configuredModules.get(key);	
				}
			}
		}
		
		return null;
		
	}
	

	
	@Override
	public Map<String,IntegrationConfiguration> getPaymentModulesConfigured(MerchantStore store) throws ServiceException {
		
		try {
		
			Map<String,IntegrationConfiguration> modules = new HashMap<String,IntegrationConfiguration>();
			MerchantConfiguration merchantConfiguration = merchantConfigurationService.getMerchantConfiguration(PAYMENT_MODULES, store);
			if(merchantConfiguration!=null) {
				
				if(!StringUtils.isBlank(merchantConfiguration.getValue())) {
					
					String decrypted = encryption.decrypt(merchantConfiguration.getValue());
					modules = ConfigurationModulesLoader.loadIntegrationConfigurations(decrypted);
					
					
				}
			}
			return modules;
		
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}
	
	@Override
	public void savePaymentModuleConfiguration(IntegrationConfiguration configuration, MerchantStore store) throws ServiceException {
        try {

            String moduleCode = configuration.getModuleCode();
            PaymentModule module = (PaymentModule) paymentModules.get(moduleCode);
            if (module == null) {
                throw new ServiceException("Payment module " + moduleCode + " does not exist");
            }
            module.validateModuleConfiguration(configuration, store);

        } catch (IntegrationException ie) {
            throw ie;
        }

        try {
            Map<String, IntegrationConfiguration> modules = new HashMap<String, IntegrationConfiguration>();
            MerchantConfiguration merchantConfiguration = merchantConfigurationService.getMerchantConfiguration(PAYMENT_MODULES, store);
            if (merchantConfiguration != null) {
                if (!isBlank(merchantConfiguration.getValue())) {

                    String decrypted = encryption.decrypt(merchantConfiguration.getValue());

                    modules = loadIntegrationConfigurations(decrypted);
                }
            } else {
                merchantConfiguration = new MerchantConfiguration();
                merchantConfiguration.setMerchantStore(store);
                merchantConfiguration.setKey(PAYMENT_MODULES);
            }
            modules.put(configuration.getModuleCode(), configuration);

            String configs = toJSONString(modules);

            String encrypted = encryption.encrypt(configs);
            merchantConfiguration.setValue(encrypted);

            merchantConfigurationService.saveOrUpdate(merchantConfiguration);

        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
	
	@Override
	public void removePaymentModuleConfiguration(String moduleCode, MerchantStore store) throws ServiceException {
		
		

		try {
			Map<String,IntegrationConfiguration> modules = new HashMap<String,IntegrationConfiguration>();
			MerchantConfiguration merchantConfiguration = merchantConfigurationService.getMerchantConfiguration(PAYMENT_MODULES, store);
			if(merchantConfiguration!=null) {

				if(!StringUtils.isBlank(merchantConfiguration.getValue())) {
					
					String decrypted = encryption.decrypt(merchantConfiguration.getValue());
					modules = ConfigurationModulesLoader.loadIntegrationConfigurations(decrypted);
				}
				
				modules.remove(moduleCode);
				String configs =  ConfigurationModulesLoader.toJSONString(modules);
				
				String encrypted = encryption.encrypt(configs);
				merchantConfiguration.setValue(encrypted);
				
				merchantConfigurationService.saveOrUpdate(merchantConfiguration);
				
				
			} 
			
			MerchantConfiguration configuration = merchantConfigurationService.getMerchantConfiguration(moduleCode, store);
			
			if(configuration!=null) {

                merchantConfigurationService.delete(configuration);
            }

			
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	
	}
	

	


	@Override
	public Transaction processPayment(Customer customer,
			MerchantStore store, Payment payment, List<ShoppingCartItem> items, Order order)
			throws ServiceException {


        notNull(customer);
        notNull(store);
        notNull(payment);
        notNull(order);
        notNull(order.getTotal());

        payment.setCurrency(store.getCurrency());

        BigDecimal amount = order.getTotal();
        Map<String, IntegrationConfiguration> modules = this.getPaymentModulesConfigured(store);
        if (modules == null) {
            throw new ServiceException("No payment module configured");
        }

        IntegrationConfiguration configuration = modules.get(payment.getModuleName());

        if (configuration == null) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " is not configured");
        }

        if (!configuration.isActive()) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " is not active");
        }

        String sTransactionType = configuration.getIntegrationKeys().get("transaction");
        if (sTransactionType == null) {
            sTransactionType = AUTHORIZECAPTURE.name();
        }


        if (sTransactionType.equals(AUTHORIZE.name())) {
            payment.setTransactionType(AUTHORIZE);
        } else {
            payment.setTransactionType(AUTHORIZECAPTURE);
        }


        PaymentModule module = this.paymentModules.get(payment.getModuleName());

        if (module == null) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " does not exist");
        }

        if (payment instanceof CreditCardPayment && "true".equals(coreConfiguration.getProperty("VALIDATE_CREDIT_CARD"))) {
            CreditCardPayment creditCardPayment = (CreditCardPayment) payment;
            validateCreditCard(creditCardPayment.getCreditCardNumber(), creditCardPayment.getCreditCard(), creditCardPayment.getExpirationMonth(), creditCardPayment.getExpirationYear());
        }

        IntegrationModule integrationModule = getPaymentMethodByCode(store, payment.getModuleName());
        TransactionType transactionType = valueOf(sTransactionType);
        if (transactionType == null) {
            transactionType = payment.getTransactionType();
            if (transactionType.equals(CAPTURE.name())) {
                throw new ServiceException("This method does not allow to process capture transaction. Use processCapturePayment");
            }
        }

        Transaction transaction = null;
        if (transactionType == AUTHORIZE) {
            transaction = module.authorize(store, customer, items, amount, payment, configuration, integrationModule);
        } else if (transactionType == AUTHORIZECAPTURE) {
            transaction = module.authorizeAndCapture(store, customer, items, amount, payment, configuration, integrationModule);
        } else if (transactionType == INIT) {
            transaction = module.initTransaction(store, customer, amount, payment, configuration, integrationModule);
        }


        if (transactionType != INIT) {
            transactionService.create(transaction);
        }

        if (transactionType == AUTHORIZECAPTURE) {
            order.setStatus(ORDERED);
            if (payment.getPaymentType().name() != MONEYORDER.name()) {
                order.setStatus(PROCESSED);
            }
        }

        return transaction;


    }
	
	@Override
	public PaymentModule getPaymentModule(String paymentModuleCode) throws ServiceException {
		return paymentModules.get(paymentModuleCode);
	}
	
	@Override
	public Transaction processCapturePayment(Order order, Customer customer,
			MerchantStore store)
			throws ServiceException {


        notNull(customer);
        notNull(store);
        notNull(order);
        Map<String, IntegrationConfiguration> modules = this.getPaymentModulesConfigured(store);
        if (modules == null) {
            throw new ServiceException("No payment module configured");
        }

        IntegrationConfiguration configuration = modules.get(order.getPaymentModuleCode());

        if (configuration == null) {
            throw new ServiceException("Payment module " + order.getPaymentModuleCode() + " is not configured");
        }

        if (!configuration.isActive()) {
            throw new ServiceException("Payment module " + order.getPaymentModuleCode() + " is not active");
        }


        PaymentModule module = this.paymentModules.get(order.getPaymentModuleCode());

        if (module == null) {
            throw new ServiceException("Payment module " + order.getPaymentModuleCode() + " does not exist");
        }


        IntegrationModule integrationModule = getPaymentMethodByCode(store, order.getPaymentModuleCode());
        Transaction trx = transactionService.getCapturableTransaction(order);
        if (trx == null) {
            throw new ServiceException("No capturable transaction for order id " + order.getId());
        }
        Transaction transaction = module.capture(store, customer, order, trx, configuration, integrationModule);
        transaction.setOrder(order);


        transactionService.create(transaction);


        OrderStatusHistory orderHistory = new OrderStatusHistory();
        orderHistory.setOrder(order);
        orderHistory.setStatus(PROCESSED);
        orderHistory.setDateAdded(new Date());

        orderService.addOrderStatusHistory(order, orderHistory);

        order.setStatus(PROCESSED);
        orderService.saveOrUpdate(order);

        return transaction;


    }

	@Override
	public Transaction processRefund(Order order, Customer customer,
			MerchantStore store, BigDecimal amount)
			throws ServiceException {


        notNull(customer);
        notNull(store);
        notNull(amount);
        notNull(order);
        notNull(order.getOrderTotal());


        BigDecimal orderTotal = order.getTotal();

        if (amount.doubleValue() > orderTotal.doubleValue()) {
            throw new ServiceException("Invalid amount, the refunded amount is greater than the total allowed");
        }


        String module = order.getPaymentModuleCode();
        Map<String, IntegrationConfiguration> modules = this.getPaymentModulesConfigured(store);
        if (modules == null) {
            throw new ServiceException("No payment module configured");
        }

        IntegrationConfiguration configuration = modules.get(module);

        if (configuration == null) {
            throw new ServiceException("Payment module " + module + " is not configured");
        }

        PaymentModule paymentModule = this.paymentModules.get(module);

        if (paymentModule == null) {
            throw new ServiceException("Payment module " + paymentModule + " does not exist");
        }

        boolean partial = false;
        if (amount.doubleValue() != order.getTotal().doubleValue()) {
            partial = true;
        }

        IntegrationModule integrationModule = getPaymentMethodByCode(store, module);
        Transaction refundable = transactionService.getRefundableTransaction(order);

        if (refundable == null) {
            throw new ServiceException("No refundable transaction for this order");
        }

        Transaction transaction = paymentModule.refund(partial, store, refundable, order, amount, configuration, integrationModule);
        transaction.setOrder(order);
        transactionService.create(transaction);

        OrderTotal refund = new OrderTotal();
        refund.setModule(OT_REFUND_MODULE_CODE);
        refund.setText(OT_REFUND_MODULE_CODE);
        refund.setTitle(OT_REFUND_MODULE_CODE);
        refund.setOrderTotalCode(OT_REFUND_MODULE_CODE);
        refund.setOrderTotalType(REFUND);
        refund.setValue(amount);
        refund.setSortOrder(100);
        refund.setOrder(order);

        order.getOrderTotal().add(refund);
        orderTotal = orderTotal.subtract(amount);
        Set<OrderTotal> totals = order.getOrderTotal();
        for (OrderTotal total : totals) {
            if (total.getModule().equals(OT_TOTAL_MODULE_CODE)) {
                total.setValue(orderTotal);
            }
        }


        order.setTotal(orderTotal);
        order.setStatus(REFUNDED);


        OrderStatusHistory orderHistory = new OrderStatusHistory();
        orderHistory.setOrder(order);
        orderHistory.setStatus(REFUNDED);
        orderHistory.setDateAdded(new Date());
        order.getOrderHistory().add(orderHistory);

        orderService.saveOrUpdate(order);

        return transaction;
    }
	
	@Override
	public void validateCreditCard(String number, CreditCardType creditCard, String month, String date)
	throws ServiceException {

		try {
			Integer.parseInt(month);
			Integer.parseInt(date);
		} catch (NumberFormatException nfe) {
			ServiceException ex = new ServiceException(ServiceException.EXCEPTION_VALIDATION,"Invalid date format","messages.error.creditcard.dateformat");
			throw ex;
		}
		
		if (StringUtils.isBlank(number)) {
			ServiceException ex = new ServiceException(ServiceException.EXCEPTION_VALIDATION,"Invalid card number","messages.error.creditcard.number");
			throw ex;
		}
		
		Matcher m = Pattern.compile("[^\\d\\s.-]").matcher(number);
		
		if (m.find()) {
			ServiceException ex = new ServiceException(ServiceException.EXCEPTION_VALIDATION,"Invalid card number","messages.error.creditcard.number");
			throw ex;
		}
		
		Matcher matcher = Pattern.compile("[\\s.-]").matcher(number);
		
		number = matcher.replaceAll("");
		validateCreditCardDate(Integer.parseInt(month), Integer.parseInt(date));
		validateCreditCardNumber(number, creditCard);
	}

	private void validateCreditCardDate(int m, int y) throws ServiceException {
        Calendar cal = new GregorianCalendar();
        int monthNow = cal.get(MONTH) + 1;
        int yearNow = cal.get(YEAR);
        if (yearNow > y) {
            ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid date format", "messages.error.creditcard.dateformat");
            throw ex;
        }
        if (yearNow == y && monthNow > m) {
            ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid date format", "messages.error.creditcard.dateformat");
            throw ex;
        }

    }

    @Deprecated
    private void validateCreditCardNumber(String number, CreditCardType creditCard)
	throws ServiceException {
        if (MASTERCARD.equals(creditCard.name())) {
            if (number.length() != 16
                    || parseInt(number.substring(0, 2)) < 51
                    || parseInt(number.substring(0, 2)) > 55) {
                ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid card number", "messages.error.creditcard.number");
                throw ex;
            }
        }

        if (VISA.equals(creditCard.name())) {
            if ((number.length() != 13 && number.length() != 16)
                    || parseInt(number.substring(0, 1)) != 4) {
                ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid card number", "messages.error.creditcard.number");
                throw ex;
            }
        }

        if (AMEX.equals(creditCard.name())) {
            if (number.length() != 15
                    || (parseInt(number.substring(0, 2)) != 34 &&
                    parseInt(number.substring(0, 2)) != 37)) {
                ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid card number", "messages.error.creditcard.number");
                throw ex;
            }
        }

        if (DINERS.equals(creditCard.name())) {
            if (number.length() != 14
                    || ((parseInt(number.substring(0, 2)) != 36 &&
                    parseInt(number.substring(0, 2)) != 38)
                    && parseInt(number.substring(0, 3)) < 300 ||
                    parseInt(number.substring(0, 3)) > 305)) {
                ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid card number", "messages.error.creditcard.number");
                throw ex;
            }
        }

        if (DISCOVERY.equals(creditCard.name())) {
            if (number.length() != 16
                    || parseInt(number.substring(0, 5)) != 6011) {
                ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid card number", "messages.error.creditcard.number");
                throw ex;
            }
        }

        luhnValidate(number);
    }

    // system for checking the validity of an entry.
    // All major credit cards use numbers that will
    // pass the Luhn check. Also, all of them are based
    // on MOD 10.
    @Deprecated
    private void luhnValidate(String numberString)
            throws ServiceException {
        char[] charArray = numberString.toCharArray();
        int[] number = new int[charArray.length];
        int total = 0;

        for (int i = 0; i < charArray.length; i++) {
            number[i] = getNumericValue(charArray[i]);
        }

        for (int i = number.length - 2; i > -1; i -= 2) {
            number[i] *= 2;

            if (number[i] > 9)
                number[i] -= 9;
        }

        for (int i = 0; i < number.length; i++)
            total += number[i];

        if (total % 10 != 0) {
            ServiceException ex = new ServiceException(EXCEPTION_VALIDATION, "Invalid card number", "messages.error.creditcard.number");
            throw ex;
        }

    }

	@Override
	public Transaction initTransaction(Order order, Customer customer, Payment payment, MerchantStore store) throws ServiceException {

        notNull(store);
        notNull(payment);
        notNull(order);
        notNull(order.getTotal());

        payment.setCurrency(store.getCurrency());

        BigDecimal amount = order.getTotal();
        Map<String, IntegrationConfiguration> modules = this.getPaymentModulesConfigured(store);
        if (modules == null) {
            throw new ServiceException("No payment module configured");
        }

        IntegrationConfiguration configuration = modules.get(payment.getModuleName());

        if (configuration == null) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " is not configured");
        }

        if (!configuration.isActive()) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " is not active");
        }

        PaymentModule module = this.paymentModules.get(order.getPaymentModuleCode());

        if (module == null) {
            throw new ServiceException("Payment module " + order.getPaymentModuleCode() + " does not exist");
        }

        IntegrationModule integrationModule = getPaymentMethodByCode(store, payment.getModuleName());

        Transaction transaction = module.initTransaction(store, customer, amount, payment, configuration, integrationModule);

        return transaction;
    }

	@Override
	public Transaction initTransaction(Customer customer, Payment payment, MerchantStore store) throws ServiceException {

        notNull(store);
        notNull(payment);
        notNull(payment.getAmount());

        payment.setCurrency(store.getCurrency());

        BigDecimal amount = payment.getAmount();
        Map<String, IntegrationConfiguration> modules = this.getPaymentModulesConfigured(store);
        if (modules == null) {
            throw new ServiceException("No payment module configured");
        }

        IntegrationConfiguration configuration = modules.get(payment.getModuleName());

        if (configuration == null) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " is not configured");
        }

        if (!configuration.isActive()) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " is not active");
        }

        PaymentModule module = this.paymentModules.get(payment.getModuleName());

        if (module == null) {
            throw new ServiceException("Payment module " + payment.getModuleName() + " does not exist");
        }

        IntegrationModule integrationModule = getPaymentMethodByCode(store, payment.getModuleName());

        Transaction transaction = module.initTransaction(store, customer, amount, payment, configuration, integrationModule);

        transactionService.save(transaction);

        return transaction;
    }


	


}
