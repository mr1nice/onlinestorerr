package com.salesmanager.core.business.modules.integration.payment.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesmanager.core.business.utils.ProductPriceUtils;
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
import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Refund;

import static com.salesmanager.core.business.exception.ServiceException.EXCEPTION_PAYMENT_DECLINED;
import static com.salesmanager.core.business.exception.ServiceException.EXCEPTION_VALIDATION;
import static com.salesmanager.core.model.payments.PaymentType.CREDITCARD;
import static com.salesmanager.core.model.payments.TransactionType.AUTHORIZE;
import static com.salesmanager.core.model.payments.TransactionType.CAPTURE;
import static com.salesmanager.core.modules.integration.IntegrationException.ERROR_VALIDATION_SAVE;
import static com.salesmanager.core.modules.integration.IntegrationException.TRANSACTION_EXCEPTION;
import static com.stripe.Stripe.apiKey;
import static com.stripe.model.Charge.create;
import static com.stripe.model.Charge.retrieve;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class StripePayment implements PaymentModule {
	
	@Inject
	private ProductPriceUtils productPriceUtils;

	
	private final static String AUTHORIZATION = "Authorization";
	private final static String TRANSACTION = "Transaction";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StripePayment.class);
	
	@Override
	public void validateModuleConfiguration(
			IntegrationConfiguration integrationConfiguration,
			MerchantStore store) throws IntegrationException {


        List<String> errorFields = null;


        Map<String, String> keys = integrationConfiguration.getIntegrationKeys();
        if (keys == null || isBlank(keys.get("secretKey"))) {
            errorFields = new ArrayList<String>();
            errorFields.add("secretKey");
        }
        if (keys == null || isBlank(keys.get("publishableKey"))) {
            if (errorFields == null) {
                errorFields = new ArrayList<String>();
            }
            errorFields.add("publishableKey");
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
		
		Transaction transaction = new Transaction();
		try {


            String apiKey = configuration.getIntegrationKeys().get("secretKey");

            if (payment.getPaymentMetaData() == null || isBlank(apiKey)) {
                IntegrationException te = new IntegrationException(
                        "Can't process Stripe, missing payment.metaData");
                te.setExceptionType(TRANSACTION_EXCEPTION);
                te.setMessageCode("message.payment.error");
                te.setErrorCode(TRANSACTION_EXCEPTION);
                throw te;
            }

            String token = payment.getPaymentMetaData().get("stripe_token");

            if (isBlank(token)) {
                IntegrationException te = new IntegrationException(
                        "Can't process Stripe, missing stripe token");
                te.setExceptionType(TRANSACTION_EXCEPTION);
                te.setMessageCode("message.payment.error");
                te.setErrorCode(TRANSACTION_EXCEPTION);
                throw te;
            }


            String amnt = productPriceUtils.getAdminFormatedAmount(store, amount);

            String strAmount = valueOf(amnt);
            strAmount = strAmount.replace(".", "");

            Map<String, Object> chargeParams = new HashMap<String, Object>();
            chargeParams.put("amount", strAmount);
            chargeParams.put("capture", false);
            chargeParams.put("currency", store.getCurrency().getCode());
            chargeParams.put("source", token);
            chargeParams.put("description", new StringBuilder().append(TRANSACTION).append(" - ").append(store.getStorename()).toString());

            apiKey = apiKey;


            Charge ch = create(chargeParams);


            transaction.setAmount(amount);
            transaction.setTransactionDate(new Date());
            transaction.setTransactionType(AUTHORIZE);
            transaction.setPaymentType(CREDITCARD);
            transaction.getTransactionDetails().put("TRANSACTIONID", token);
            transaction.getTransactionDetails().put("TRNAPPROVED", ch.getStatus());
            transaction.getTransactionDetails().put("TRNORDERNUMBER", ch.getId());
            transaction.getTransactionDetails().put("MESSAGETEXT", null);

        } catch (Exception e) {
			
			throw buildException(e);

		} 
		
		return transaction;

		
	}

	@Override
	public Transaction capture(MerchantStore store, Customer customer,
			Order order, Transaction capturableTransaction,
			IntegrationConfiguration configuration, IntegrationModule module)
			throws IntegrationException {


			Transaction transaction = new Transaction();
			try {


                String apiKey = configuration.getIntegrationKeys().get("secretKey");

                if (isBlank(apiKey)) {
                    IntegrationException te = new IntegrationException(
                            "Can't process Stripe, missing payment.metaData");
                    te.setExceptionType(TRANSACTION_EXCEPTION);
                    te.setMessageCode("message.payment.error");
                    te.setErrorCode(TRANSACTION_EXCEPTION);
                    throw te;
                }

                String chargeId = capturableTransaction.getTransactionDetails().get("TRNORDERNUMBER");

                if (isBlank(chargeId)) {
                    IntegrationException te = new IntegrationException(
                            "Can't process Stripe capture, missing TRNORDERNUMBER");
                    te.setExceptionType(TRANSACTION_EXCEPTION);
                    te.setMessageCode("message.payment.error");
                    te.setErrorCode(TRANSACTION_EXCEPTION);
                    throw te;
                }


                apiKey = apiKey;

                Charge ch = retrieve(chargeId);
                ch.capture();


                transaction.setAmount(order.getTotal());
                transaction.setOrder(order);
                transaction.setTransactionDate(new Date());
                transaction.setTransactionType(CAPTURE);
                transaction.setPaymentType(CREDITCARD);
                transaction.getTransactionDetails().put("TRANSACTIONID", capturableTransaction.getTransactionDetails().get("TRANSACTIONID"));
                transaction.getTransactionDetails().put("TRNAPPROVED", ch.getStatus());
                transaction.getTransactionDetails().put("TRNORDERNUMBER", ch.getId());
                transaction.getTransactionDetails().put("MESSAGETEXT", null);


                return transaction;

            } catch (Exception e) {
				
				throw buildException(e);

			}  

	}

	@Override
	public Transaction authorizeAndCapture(MerchantStore store, Customer customer,
			List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
			IntegrationConfiguration configuration, IntegrationModule module)
			throws IntegrationException {
		
		String apiKey = configuration.getIntegrationKeys().get("secretKey");

		if(payment.getPaymentMetaData()==null || StringUtils.isBlank(apiKey)) {
			IntegrationException te = new IntegrationException(
					"Can't process Stripe, missing payment.metaData");
			te.setExceptionType(IntegrationException.TRANSACTION_EXCEPTION);
			te.setMessageCode("message.payment.error");
			te.setErrorCode(IntegrationException.TRANSACTION_EXCEPTION);
			throw te;
		}
		
		String token = payment.getPaymentMetaData().get("stripe_token");
		
		if(StringUtils.isBlank(token)) {
			IntegrationException te = new IntegrationException(
					"Can't process Stripe, missing stripe token");
			te.setExceptionType(IntegrationException.TRANSACTION_EXCEPTION);
			te.setMessageCode("message.payment.error");
			te.setErrorCode(IntegrationException.TRANSACTION_EXCEPTION);
			throw te;
		}
		


		Transaction transaction = new Transaction();
		try {

            String amnt = productPriceUtils.getAdminFormatedAmount(store, amount);


            String strAmount = valueOf(amnt);
            strAmount = strAmount.replace(".", "");

            Map<String, Object> chargeParams = new HashMap<String, Object>();
            chargeParams.put("amount", strAmount);
            chargeParams.put("capture", true);
            chargeParams.put("currency", store.getCurrency().getCode());
            chargeParams.put("source", token);
            chargeParams.put("description", new StringBuilder().append(TRANSACTION).append(" - ").append(store.getStorename()).toString());

            apiKey = apiKey;


            Charge ch = create(chargeParams);


            transaction.setAmount(amount);
            transaction.setTransactionDate(new Date());
            transaction.setTransactionType(AUTHORIZE);
            transaction.setPaymentType(CREDITCARD);
            transaction.getTransactionDetails().put("TRANSACTIONID", token);
            transaction.getTransactionDetails().put("TRNAPPROVED", ch.getStatus());
            transaction.getTransactionDetails().put("TRNORDERNUMBER", ch.getId());
            transaction.getTransactionDetails().put("MESSAGETEXT", null);

        } catch (Exception e) {
			
			throw buildException(e);
	
		} 
		
		return transaction;  
		
	}

	@Override
	public Transaction refund(boolean partial, MerchantStore store, Transaction transaction,
			Order order, BigDecimal amount,
			IntegrationConfiguration configuration, IntegrationModule module)
			throws IntegrationException {
		
		
		
		String apiKey = configuration.getIntegrationKeys().get("secretKey");

		if(StringUtils.isBlank(apiKey)) {
			IntegrationException te = new IntegrationException(
					"Can't process Stripe, missing payment.metaData");
			te.setExceptionType(IntegrationException.TRANSACTION_EXCEPTION);
			te.setMessageCode("message.payment.error");
			te.setErrorCode(IntegrationException.TRANSACTION_EXCEPTION);
			throw te;
		}

		try {


            String trnID = transaction.getTransactionDetails().get("TRNORDERNUMBER");

            String amnt = productPriceUtils.getAdminFormatedAmount(store, amount);

            apiKey = apiKey;

            String strAmount = valueOf(amnt);
            strAmount = strAmount.replace(".", "");

            Map params = new HashMap();
            params.put("amount", strAmount);
            Charge ch = retrieve(trnID);
            Refund re = ch.getRefunds().create(params);

            transaction = new Transaction();
            transaction.setAmount(order.getTotal());
            transaction.setOrder(order);
            transaction.setTransactionDate(new Date());
            transaction.setTransactionType(CAPTURE);
            transaction.setPaymentType(CREDITCARD);
            transaction.getTransactionDetails().put("TRANSACTIONID", transaction.getTransactionDetails().get("TRANSACTIONID"));
            transaction.getTransactionDetails().put("TRNAPPROVED", re.getReason());
            transaction.getTransactionDetails().put("TRNORDERNUMBER", re.getId());
            transaction.getTransactionDetails().put("MESSAGETEXT", null);

            return transaction;


        } catch(Exception e) {
			
			throw buildException(e);

		} 
		
		
		
	}
	
	private IntegrationException buildException(Exception ex) {
		
		
	if(ex instanceof CardException) {
        CardException e = (CardException) ex;


        String declineCode = e.getDeclineCode();

        if ("card_declined".equals(declineCode)) {
            IntegrationException te = new IntegrationException(
                    "Can't process stripe message " + e.getMessage());
            te.setExceptionType(EXCEPTION_PAYMENT_DECLINED);
            te.setMessageCode("message.payment.declined");
            te.setErrorCode(TRANSACTION_EXCEPTION);
            return te;
        }

        if ("invalid_number".equals(declineCode)) {
            IntegrationException te = new IntegrationException(
                    "Can't process stripe message " + e.getMessage());
            te.setExceptionType(EXCEPTION_VALIDATION);
            te.setMessageCode("messages.error.creditcard.number");
            te.setErrorCode(EXCEPTION_VALIDATION);
            return te;
        }

        if ("invalid_expiry_month".equals(declineCode)) {
            IntegrationException te = new IntegrationException(
                    "Can't process stripe message " + e.getMessage());
            te.setExceptionType(EXCEPTION_VALIDATION);
            te.setMessageCode("messages.error.creditcard.dateformat");
            te.setErrorCode(EXCEPTION_VALIDATION);
            return te;
        }

        if ("invalid_expiry_year".equals(declineCode)) {
            IntegrationException te = new IntegrationException(
                    "Can't process stripe message " + e.getMessage());
            te.setExceptionType(EXCEPTION_VALIDATION);
            te.setMessageCode("messages.error.creditcard.dateformat");
            te.setErrorCode(EXCEPTION_VALIDATION);
            return te;
        }

        if ("invalid_cvc".equals(declineCode)) {
            IntegrationException te = new IntegrationException(
                    "Can't process stripe message " + e.getMessage());
            te.setExceptionType(EXCEPTION_VALIDATION);
            te.setMessageCode("messages.error.creditcard.cvc");
            te.setErrorCode(EXCEPTION_VALIDATION);
            return te;
        }

        if ("incorrect_number".equals(declineCode)) {
            IntegrationException te = new IntegrationException(
                    "Can't process stripe message " + e.getMessage());
            te.setExceptionType(EXCEPTION_VALIDATION);
            te.setMessageCode("messages.error.creditcard.number");
            te.setErrorCode(EXCEPTION_VALIDATION);
            return te;
        }

        if ("incorrect_cvc".equals(declineCode)) {
            IntegrationException te = new IntegrationException(
                    "Can't process stripe message " + e.getMessage());
            te.setExceptionType(EXCEPTION_VALIDATION);
            te.setMessageCode("messages.error.creditcard.cvc");
            te.setErrorCode(EXCEPTION_VALIDATION);
            return te;
        }


    } else if (ex instanceof InvalidRequestException) {
		LOGGER.error("InvalidRequest error with stripe", ex.getMessage());
		InvalidRequestException e =(InvalidRequestException)ex;
		IntegrationException te = new IntegrationException(
				"Can't process Stripe, missing invalid payment parameters");
		te.setExceptionType(IntegrationException.TRANSACTION_EXCEPTION);
		te.setMessageCode("message.payment.error");
		te.setErrorCode(IntegrationException.TRANSACTION_EXCEPTION);
		return te;
		
	} else if (ex instanceof AuthenticationException) {
        LOGGER.error("Authentication error with stripe", ex.getMessage());
        AuthenticationException e = (AuthenticationException) ex;
        IntegrationException te = new IntegrationException(
                "Can't process Stripe, missing invalid payment parameters");
        te.setExceptionType(TRANSACTION_EXCEPTION);
        te.setMessageCode("message.payment.error");
        te.setErrorCode(TRANSACTION_EXCEPTION);
        return te;

    } else if (ex instanceof APIConnectionException) {
        LOGGER.error("API connection error with stripe", ex.getMessage());
        APIConnectionException e = (APIConnectionException) ex;
        IntegrationException te = new IntegrationException(
                "Can't process Stripe, missing invalid payment parameters");
        te.setExceptionType(TRANSACTION_EXCEPTION);
        te.setMessageCode("message.payment.error");
        te.setErrorCode(TRANSACTION_EXCEPTION);
        return te;
    } else if (ex instanceof StripeException) {
        LOGGER.error("Error with stripe", ex.getMessage());
        StripeException e = (StripeException) ex;
        IntegrationException te = new IntegrationException(
                "Can't process Stripe authorize, missing invalid payment parameters");
        te.setExceptionType(TRANSACTION_EXCEPTION);
        te.setMessageCode("message.payment.error");
        te.setErrorCode(TRANSACTION_EXCEPTION);
        return te;


    } else if (ex instanceof Exception) {
		LOGGER.error("Stripe module error", ex.getMessage());
		if(ex instanceof IntegrationException) {
			return (IntegrationException)ex;
		} else {
			IntegrationException te = new IntegrationException(
					"Can't process Stripe authorize, exception", ex);
			te.setExceptionType(IntegrationException.TRANSACTION_EXCEPTION);
			te.setMessageCode("message.payment.error");
			te.setErrorCode(IntegrationException.TRANSACTION_EXCEPTION);
			return te;
		}


	} else {
		LOGGER.error("Stripe module error", ex.getMessage());
		IntegrationException te = new IntegrationException(
				"Can't process Stripe authorize, exception", ex);
		te.setExceptionType(IntegrationException.TRANSACTION_EXCEPTION);
		te.setMessageCode("message.payment.error");
		te.setErrorCode(IntegrationException.TRANSACTION_EXCEPTION);
		return te;
	}
	return null;

		
	}
	
	



}
