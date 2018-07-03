package com.salesmanager.core.business.modules.integration.payment.impl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesmanager.core.business.services.system.MerchantLogService;
import com.salesmanager.core.business.utils.CreditCardUtils;
import com.salesmanager.core.business.utils.ProductPriceUtils;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.payments.CreditCardPayment;
import com.salesmanager.core.model.payments.Payment;
import com.salesmanager.core.model.payments.PaymentType;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.payments.TransactionType;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.model.system.MerchantLog;
import com.salesmanager.core.model.system.ModuleConfig;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;

import static com.salesmanager.core.business.exception.ServiceException.EXCEPTION_PAYMENT_DECLINED;
import static com.salesmanager.core.business.utils.CreditCardUtils.maskCardNumber;
import static com.salesmanager.core.model.payments.PaymentType.CREDITCARD;
import static com.salesmanager.core.model.payments.TransactionType.*;
import static com.salesmanager.core.modules.integration.IntegrationException.ERROR_VALIDATION_SAVE;
import static com.salesmanager.core.modules.integration.IntegrationException.TRANSACTION_EXCEPTION;
import static java.lang.String.valueOf;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class BeanStreamPayment implements PaymentModule {
	
	@Inject
	private ProductPriceUtils productPriceUtils;
	
	@Inject
	private MerchantLogService merchantLogService;
	

	
	private static final Logger LOGGER = LoggerFactory.getLogger(BeanStreamPayment.class);

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
		return processTransaction(store, customer, TransactionType.AUTHORIZE,
				amount,
				payment,
				configuration,
				module);
	}

	@Override
	public Transaction capture(MerchantStore store, Customer customer,
			Order order, Transaction capturableTransaction,
			IntegrationConfiguration configuration, IntegrationModule module)
			throws IntegrationException {


			try {


                String trnID = capturableTransaction.getTransactionDetails().get("TRANSACTIONID");

                String amnt = productPriceUtils.getAdminFormatedAmount(store, order.getTotal());

                StringBuilder messageString = new StringBuilder();
                messageString.append("requestType=BACKEND&");
                messageString.append("merchant_id=").append(configuration.getIntegrationKeys().get("merchantid")).append("&");
                messageString.append("trnType=").append("PAC").append("&");
                messageString.append("username=").append(configuration.getIntegrationKeys().get("username")).append("&");
                messageString.append("password=").append(configuration.getIntegrationKeys().get("password")).append("&");
                messageString.append("trnAmount=").append(amnt).append("&");
                messageString.append("adjId=").append(trnID).append("&");
                messageString.append("trnID=").append(trnID);

                LOGGER.debug("REQUEST SENT TO BEANSTREAM -> " + messageString.toString());


                Transaction response = this.sendTransaction(null, store, messageString.toString(), "PAC", CAPTURE, CREDITCARD, order.getTotal(), configuration, module);

                return response;

            } catch(Exception e) {
				
				if(e instanceof IntegrationException)
					throw (IntegrationException)e;
				throw new IntegrationException("Error while processing BeanStream transaction",e);
	
			} 

	}

	@Override
	public Transaction authorizeAndCapture(MerchantStore store, Customer customer,
			List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
			IntegrationConfiguration configuration, IntegrationModule module)
			throws IntegrationException {
		return processTransaction(
				store,
				customer,
				TransactionType.AUTHORIZECAPTURE,
				amount,
				payment,
				configuration,
				module);
	}

	@Override
	public Transaction refund(boolean partial, MerchantStore store, Transaction transaction,
			Order order, BigDecimal amount,
			IntegrationConfiguration configuration, IntegrationModule module)
			throws IntegrationException {

		
		
		
		HttpURLConnection conn = null;
		
		try {


            boolean bSandbox = false;
            if (configuration.getEnvironment().equals("TEST")) {
                bSandbox = true;
            }

            String server = "";


            ModuleConfig configs = module.getModuleConfigs().get("PROD");

            if (bSandbox == true) {
                configs = module.getModuleConfigs().get("TEST");
            }

            if (configs == null) {
                throw new IntegrationException("Module not configured for TEST or PROD");
            }


            server = new StringBuffer().append(

                    configs.getScheme()).append("://")
                    .append(configs.getHost())
                    .append(":")
                    .append(configs.getPort())
                    .append(configs.getUri()).toString();

            String trnID = transaction.getTransactionDetails().get("TRANSACTIONID");

            String amnt = productPriceUtils.getAdminFormatedAmount(store, amount);
            StringBuilder messageString = new StringBuilder();


            messageString.append("requestType=BACKEND&");
            messageString.append("merchant_id=").append(configuration.getIntegrationKeys().get("merchantid")).append("&");
            messageString.append("trnType=").append("R").append("&");
            messageString.append("username=").append(configuration.getIntegrationKeys().get("username")).append("&");
            messageString.append("password=").append(configuration.getIntegrationKeys().get("password")).append("&");
            messageString.append("trnOrderNumber=").append(transaction.getTransactionDetails().get("TRNORDERNUMBER")).append("&");
            messageString.append("trnAmount=").append(amnt).append("&");
            messageString.append("adjId=").append(trnID);

            LOGGER.debug("REQUEST SENT TO BEANSTREAM -> " + messageString.toString());


            URL postURL = new URL(server.toString());
            conn = (HttpURLConnection) postURL.openConnection();


            Transaction response = this.sendTransaction(null, store, messageString.toString(), "R", REFUND, CREDITCARD, amount, configuration, module);

            return response;

        } catch(Exception e) {
			
			if(e instanceof IntegrationException)
				throw (IntegrationException)e;
			throw new IntegrationException("Error while processing BeanStream transaction",e);

		} finally {
			
			
			if (conn != null) {
				try {
					conn.disconnect();
				} catch (Exception ignore) {
                }
			}
		}
		
		
		
	}
	
	
	private Transaction sendTransaction(
			String orderNumber,
			MerchantStore store,
			String transaction, 
			String beanstreamType, 
			TransactionType transactionType,
			PaymentType paymentType,
			BigDecimal amount,
			IntegrationConfiguration configuration,
			IntegrationModule module
			) throws IntegrationException {
		
		String agent = "Mozilla/4.0";
		String respText = "";
		Map<String,String> nvp = null;
		DataOutputStream output = null;
		DataInputStream in = null;
		BufferedReader is = null;
		HttpURLConnection conn =null;
		try {


            boolean bSandbox = false;
            if (configuration.getEnvironment().equals("TEST")) {
                bSandbox = true;
            }

            String server = "";

            ModuleConfig configs = module.getModuleConfigs().get("PROD");

            if (bSandbox == true) {
                configs = module.getModuleConfigs().get("TEST");
            }

            if (configs == null) {
                throw new IntegrationException("Module not configured for TEST or PROD");
            }


            server = new StringBuffer().append(

                    configs.getScheme()).append("://")
                    .append(configs.getHost())
                    .append(":")
                    .append(configs.getPort())
                    .append(configs.getUri()).toString();


            URL postURL = new URL(server.toString());
            conn = (HttpURLConnection) postURL.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", agent);

            conn.setRequestProperty("Content-Length",
                    valueOf(transaction.length()));
            conn.setRequestMethod("POST");
            output = new DataOutputStream(conn.getOutputStream());
            output.writeBytes(transaction);
            output.flush();
            in = new DataInputStream(conn.getInputStream());
            int rc = conn.getResponseCode();
            if (rc != -1) {
                is = new BufferedReader(new InputStreamReader(conn
                        .getInputStream()));
                String _line = null;
                while (((_line = is.readLine()) != null)) {
                    respText = respText + _line;
                }

                LOGGER.debug("BeanStream response -> " + respText.trim());

                nvp = formatUrlResponse(respText.trim());
            } else {
                throw new IntegrationException("Invalid response from BeanStream, return code is " + rc);
            }

            String transactionApproved = (String) nvp.get("TRNAPPROVED");
            String transactionId = (String) nvp.get("TRNID");
            String messageId = (String) nvp.get("MESSAGEID");
            String messageText = (String) nvp.get("MESSAGETEXT");
            String orderId = (String) nvp.get("TRNORDERNUMBER");
            String authCode = (String) nvp.get("AUTHCODE");
            String errorType = (String) nvp.get("ERRORTYPE");
            String errorFields = (String) nvp.get("ERRORFIELDS");
            if (!isBlank(orderNumber)) {
                nvp.put("INTERNALORDERID", orderNumber);
            }

            if (isBlank(transactionApproved)) {
                throw new IntegrationException("Required field transactionApproved missing from BeanStream response");
            }
            if (transactionApproved.equals("0")) {

                merchantLogService.save(
                        new MerchantLog(store,
                                "Can't process BeanStream message "
                                        + messageText + " return code id " + messageId));

                IntegrationException te = new IntegrationException(
                        "Can't process BeanStream message " + messageText);
                te.setExceptionType(EXCEPTION_PAYMENT_DECLINED);
                te.setMessageCode("message.payment.beanstream." + messageId);
                te.setErrorCode(TRANSACTION_EXCEPTION);
                throw te;
            }
            return this.parseResponse(transactionType, paymentType, nvp, amount);


        } catch(Exception e) {
			if(e instanceof IntegrationException) {
				throw (IntegrationException)e;
			}
			
			throw new IntegrationException("Error while processing BeanStream transaction",e);

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception ignore) {
                }
			}

			if (in != null) {
				try {
					in.close();
				} catch (Exception ignore) {
                }
			}

			if (output != null) {
				try {
					output.close();
				} catch (Exception ignore) {
                }
			}
			
			if (conn != null) {
				try {
					conn.disconnect();
				} catch (Exception ignore) {
                }
			}

		}

		
	}
	
	
	
	private Transaction processTransaction(MerchantStore store, Customer customer, TransactionType type,
			BigDecimal amount, Payment payment,
			IntegrationConfiguration configuration, IntegrationModule module) throws IntegrationException {
		

		
		
		
		boolean bSandbox = false;
		if (configuration.getEnvironment().equals("TEST")) {
            bSandbox = true;
        }

		String server = "";

		ModuleConfig configs = module.getModuleConfigs().get("PROD");

		if (bSandbox == true) {
			configs = module.getModuleConfigs().get("TEST");
		} 
		
		if(configs==null) {
			throw new IntegrationException("Module not configured for TEST or PROD");
		}
		

		server = new StringBuffer().append(
				
				configs.getScheme()).append("://")
				.append(configs.getHost())
						.append(":")
						.append(configs.getPort())
						.append(configs.getUri()).toString();
		
		HttpURLConnection conn = null;
		
		try {

            String uniqueId = randomUUID().toString();

            String orderNumber = uniqueId;

            String amnt = productPriceUtils.getAdminFormatedAmount(store, amount);


            StringBuilder messageString = new StringBuilder();

            String transactionType = "P";
            if (type == AUTHORIZE) {
                transactionType = "PA";
            } else if (type == AUTHORIZECAPTURE) {
                transactionType = "P";
            }

            CreditCardPayment creditCardPayment = (CreditCardPayment) payment;

            messageString.append("requestType=BACKEND&");
            messageString.append("merchant_id=").append(configuration.getIntegrationKeys().get("merchantid")).append("&");
            messageString.append("trnType=").append(transactionType).append("&");
            messageString.append("username=").append(configuration.getIntegrationKeys().get("username")).append("&");
            messageString.append("password=").append(configuration.getIntegrationKeys().get("password")).append("&");
            messageString.append("orderNumber=").append(orderNumber).append("&");
            messageString.append("trnCardOwner=").append(creditCardPayment.getCardOwner()).append("&");
            messageString.append("trnCardNumber=").append(creditCardPayment.getCreditCardNumber()).append("&");
            messageString.append("trnExpMonth=").append(creditCardPayment.getExpirationMonth()).append("&");
            messageString.append("trnExpYear=").append(creditCardPayment.getExpirationYear().substring(2)).append("&");
            messageString.append("trnCardCvd=").append(creditCardPayment.getCredidCardValidationNumber()).append("&");
            messageString.append("trnAmount=").append(amnt).append("&");

            StringBuilder nm = new StringBuilder();
            nm.append(customer.getBilling().getFirstName()).append(" ").append(customer.getBilling().getLastName());


            messageString.append("ordName=").append(nm.toString()).append("&");
            messageString.append("ordAddress1=").append(customer.getBilling().getAddress()).append("&");
            messageString.append("ordCity=").append(customer.getBilling().getCity()).append("&");

            String stateProvince = customer.getBilling().getState();
            if (customer.getBilling().getZone() != null) {
                stateProvince = customer.getBilling().getZone().getCode();
            }

            String countryName = customer.getBilling().getCountry().getIsoCode();

            messageString.append("ordProvince=").append(stateProvince).append("&");
            messageString.append("ordPostalCode=").append(customer.getBilling().getPostalCode().replaceAll("\\s", "")).append("&");
            messageString.append("ordCountry=").append(countryName).append("&");
            messageString.append("ordPhoneNumber=").append(customer.getBilling().getTelephone()).append("&");
            messageString.append("ordEmailAddress=").append(customer.getEmailAddress());


            StringBuffer messageLogString = new StringBuffer();


            messageLogString.append("requestType=BACKEND&");
            messageLogString.append("merchant_id=").append(configuration.getIntegrationKeys().get("merchantid")).append("&");
            messageLogString.append("trnType=").append(type).append("&");
            messageLogString.append("orderNumber=").append(orderNumber).append("&");
            messageLogString.append("trnCardOwner=").append(creditCardPayment.getCardOwner()).append("&");
            messageLogString.append("trnCardNumber=").append(maskCardNumber(creditCardPayment.getCreditCardNumber())).append("&");
            messageLogString.append("trnExpMonth=").append(creditCardPayment.getExpirationMonth()).append("&");
            messageLogString.append("trnExpYear=").append(creditCardPayment.getExpirationYear()).append("&");
            messageLogString.append("trnCardCvd=").append(creditCardPayment.getCredidCardValidationNumber()).append("&");
            messageLogString.append("trnAmount=").append(amnt).append("&");

            messageLogString.append("ordName=").append(nm.toString()).append("&");
            messageLogString.append("ordAddress1=").append(customer.getBilling().getAddress()).append("&");
            messageLogString.append("ordCity=").append(customer.getBilling().getCity()).append("&");


            messageLogString.append("ordProvince=").append(stateProvince).append("&");
            messageLogString.append("ordPostalCode=").append(customer.getBilling().getPostalCode()).append("&");
            messageLogString.append("ordCountry=").append(customer.getBilling().getCountry().getName()).append("&");
            messageLogString.append("ordPhoneNumber=").append(customer.getBilling().getTelephone()).append("&");
            messageLogString.append("ordEmailAddress=").append(customer.getEmailAddress());


            LOGGER.debug("REQUEST SENT TO BEANSTREAM -> " + messageLogString.toString());


            URL postURL = new URL(server.toString());
            conn = (HttpURLConnection) postURL.openConnection();


            Transaction response = this.sendTransaction(orderNumber, store, messageString.toString(), transactionType, type, payment.getPaymentType(), amount, configuration, module);

            return response;


        } catch(Exception e) {
			
			if(e instanceof IntegrationException)
				throw (IntegrationException)e;
			throw new IntegrationException("Error while processing BeanStream transaction",e);

		} finally {
			
			
			if (conn != null) {
				try {
					conn.disconnect();
				} catch (Exception ignore) {}
			}
		}

	}
	
	
	
	private Transaction parseResponse(TransactionType transactionType,
			PaymentType paymentType, Map<String,String> nvp,
			BigDecimal amount) throws Exception {


        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionDate(new Date());
        transaction.setTransactionType(transactionType);
        transaction.setPaymentType(CREDITCARD);
        transaction.getTransactionDetails().put("TRANSACTIONID", (String) nvp.get("TRNID"));
        transaction.getTransactionDetails().put("TRNAPPROVED", (String) nvp.get("TRNAPPROVED"));
        transaction.getTransactionDetails().put("TRNORDERNUMBER", (String) nvp.get("TRNORDERNUMBER"));
        transaction.getTransactionDetails().put("MESSAGETEXT", (String) nvp.get("MESSAGETEXT"));
        if (nvp.get("INTERNALORDERID") != null) {
            transaction.getTransactionDetails().put("INTERNALORDERID", (String) nvp.get("INTERNALORDERID"));
        }
        return transaction;

    }

	private Map formatUrlResponse(String payload) throws Exception {
		HashMap<String,String> nvp = new HashMap<String,String> ();
		StringTokenizer stTok = new StringTokenizer(payload, "&");
		while (stTok.hasMoreTokens()) {
			StringTokenizer stInternalTokenizer = new StringTokenizer(stTok
					.nextToken(), "=");
			if (stInternalTokenizer.countTokens() == 2) {
				String key = URLDecoder.decode(stInternalTokenizer.nextToken(),
						"UTF-8");
				String value = URLDecoder.decode(stInternalTokenizer
						.nextToken(), "UTF-8");
				nvp.put(key.toUpperCase(), value);
			}
		}
		return nvp;
	}

	@Override
	public void validateModuleConfiguration(
			IntegrationConfiguration integrationConfiguration,
			MerchantStore store) throws IntegrationException {


        List<String> errorFields = null;


        Map<String, String> keys = integrationConfiguration.getIntegrationKeys();
        if (keys == null || isBlank(keys.get("merchantid"))) {
            errorFields = new ArrayList<String>();
            errorFields.add("merchantid");
        }
        if (keys == null || isBlank(keys.get("username"))) {
            if (errorFields == null) {
                errorFields = new ArrayList<String>();
            }
            errorFields.add("username");
        }
        if (keys == null || isBlank(keys.get("password"))) {
            if (errorFields == null) {
                errorFields = new ArrayList<String>();
            }
            errorFields.add("password");
        }


        if (errorFields != null) {
            IntegrationException ex = new IntegrationException(ERROR_VALIDATION_SAVE);
            ex.setErrorFields(errorFields);
            throw ex;

        }


    }



}
