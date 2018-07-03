package com.salesmanager.core.business.modules.integration.payment.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.salesmanager.core.model.payments.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.OrderTotal;
import com.salesmanager.core.model.order.OrderTotalSummary;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;

import urn.ebay.api.PayPalAPI.DoCaptureReq;
import urn.ebay.api.PayPalAPI.DoCaptureRequestType;
import urn.ebay.api.PayPalAPI.DoCaptureResponseType;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentReq;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentRequestType;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentResponseType;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsReq;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsRequestType;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsResponseType;
import urn.ebay.api.PayPalAPI.PayPalAPIInterfaceServiceService;
import urn.ebay.api.PayPalAPI.RefundTransactionReq;
import urn.ebay.api.PayPalAPI.RefundTransactionRequestType;
import urn.ebay.api.PayPalAPI.RefundTransactionResponseType;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutReq;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutRequestType;
import urn.ebay.api.PayPalAPI.SetExpressCheckoutResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.CompleteCodeType;
import urn.ebay.apis.eBLBaseComponents.DoExpressCheckoutPaymentRequestDetailsType;
import urn.ebay.apis.eBLBaseComponents.PaymentDetailsItemType;
import urn.ebay.apis.eBLBaseComponents.PaymentDetailsType;
import urn.ebay.apis.eBLBaseComponents.PaymentInfoType;
import urn.ebay.apis.eBLBaseComponents.RefundType;
import urn.ebay.apis.eBLBaseComponents.SetExpressCheckoutRequestDetailsType;

import static com.salesmanager.core.business.constants.Constants.*;
import static com.salesmanager.core.business.exception.ServiceException.EXCEPTION_TRANSACTION_DECLINED;
import static com.salesmanager.core.model.payments.PaymentType.PAYPAL;
import static com.salesmanager.core.model.payments.TransactionType.*;
import static com.salesmanager.core.modules.integration.IntegrationException.ERROR_VALIDATION_SAVE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;
import static org.slf4j.LoggerFactory.getLogger;
import static urn.ebay.apis.eBLBaseComponents.CompleteCodeType.NOTCOMPLETE;
import static urn.ebay.apis.eBLBaseComponents.CurrencyCodeType.fromValue;
import static urn.ebay.apis.eBLBaseComponents.PaymentActionCodeType.AUTHORIZATION;
import static urn.ebay.apis.eBLBaseComponents.PaymentActionCodeType.SALE;
import static urn.ebay.apis.eBLBaseComponents.RefundType.FULL;
import static urn.ebay.apis.eBLBaseComponents.RefundType.PARTIAL;

public class PayPalExpressCheckoutPayment implements PaymentModule {

    private static final Logger LOGGER = getLogger(PayPalExpressCheckoutPayment.class);


    @Inject
    private PricingService pricingService;

    @Inject
    private CoreConfiguration coreConfiguration;

    @Override
    public void validateModuleConfiguration(
            IntegrationConfiguration integrationConfiguration,
            MerchantStore store) throws IntegrationException {


        List<String> errorFields = null;

        //validate integrationKeys['account']
        Map<String, String> keys = integrationConfiguration.getIntegrationKeys();
        if (keys == null || isBlank(keys.get("api"))) {
            errorFields = new ArrayList<String>();
            errorFields.add("api");
        }

        if (keys == null || isBlank(keys.get("username"))) {
            if (errorFields == null) {
                errorFields = new ArrayList<String>();
            }
            errorFields.add("username");
        }

        if (keys == null || isBlank(keys.get("signature"))) {
            if (errorFields == null) {
                errorFields = new ArrayList<String>();
            }
            errorFields.add("signature");
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

        throw new IntegrationException("Not imlemented");
    }

    @Override
    public Transaction authorize(MerchantStore store, Customer customer,
                                 List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
                                 IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {


        PaypalPayment paypalPayment = (PaypalPayment) payment;
        notNull(paypalPayment.getPaymentToken(), "A paypal payment token is required to process this transaction");

        return processTransaction(store, customer, items, amount, paypalPayment, configuration, module);


    }

    public Transaction initPaypalTransaction(MerchantStore store,
                                             List<ShoppingCartItem> items, OrderTotalSummary summary, Payment payment,
                                             IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {

        notNull(configuration, "Configuration must not be null");
        notNull(payment, "Payment must not be null");
        notNull(summary, "OrderTotalSummary must not be null");


        try {


            PaymentDetailsType paymentDetails = new PaymentDetailsType();
            if (configuration.getIntegrationKeys().get("transaction").equalsIgnoreCase(AUTHORIZECAPTURE.name())) {
                paymentDetails.setPaymentAction(SALE);
            } else {
                paymentDetails.setPaymentAction(AUTHORIZATION);
            }


            List<PaymentDetailsItemType> lineItems = new ArrayList<PaymentDetailsItemType>();

            for (ShoppingCartItem cartItem : items) {

                PaymentDetailsItemType item = new PaymentDetailsItemType();
                BasicAmountType amt = new BasicAmountType();
                amt.setCurrencyID(fromValue(payment.getCurrency().getCode()));
                amt.setValue(pricingService.getStringAmount(cartItem.getFinalPrice().getFinalPrice(), store));
                int itemQuantity = cartItem.getQuantity();
                item.setQuantity(itemQuantity);
                item.setName(cartItem.getProduct().getProductDescription().getName());
                item.setAmount(amt);
                lineItems.add(item);

            }


            List<OrderTotal> orderTotals = summary.getTotals();
            BigDecimal tax = null;
            for (OrderTotal total : orderTotals) {

                if (total.getModule().equals(OT_SHIPPING_MODULE_CODE)) {
                    BasicAmountType shipping = new BasicAmountType();
                    shipping.setCurrencyID(fromValue(store.getCurrency().getCode()));
                    shipping.setValue(pricingService.getStringAmount(total.getValue(), store));
                    paymentDetails.setShippingTotal(shipping);
                }

                if (total.getModule().equals(OT_HANDLING_MODULE_CODE)) {
                    BasicAmountType handling = new BasicAmountType();
                    handling.setCurrencyID(fromValue(store.getCurrency().getCode()));
                    handling.setValue(pricingService.getStringAmount(total.getValue(), store));
                    paymentDetails.setHandlingTotal(handling);
                }

                if (total.getModule().equals(OT_TAX_MODULE_CODE)) {
                    if (tax == null) {
                        tax = new BigDecimal("0");
                    }
                    tax = tax.add(total.getValue());
                }

            }

            if (tax != null) {
                BasicAmountType taxAmnt = new BasicAmountType();
                taxAmnt.setCurrencyID(fromValue(store.getCurrency().getCode()));
                taxAmnt.setValue(pricingService.getStringAmount(tax, store));
                paymentDetails.setTaxTotal(taxAmnt);
            }


            BasicAmountType itemTotal = new BasicAmountType();
            itemTotal.setCurrencyID(fromValue(store.getCurrency().getCode()));
            itemTotal.setValue(pricingService.getStringAmount(summary.getSubTotal(), store));
            paymentDetails.setItemTotal(itemTotal);

            paymentDetails.setPaymentDetailsItem(lineItems);
            BasicAmountType orderTotal = new BasicAmountType();
            orderTotal.setCurrencyID(fromValue(store.getCurrency().getCode()));
            orderTotal.setValue(pricingService.getStringAmount(summary.getTotal(), store));
            paymentDetails.setOrderTotal(orderTotal);
            List<PaymentDetailsType> paymentDetailsList = new ArrayList<PaymentDetailsType>();
            paymentDetailsList.add(paymentDetails);

            StringBuilder RETURN_URL = new StringBuilder().append(
                    coreConfiguration.getProperty("SHOP_SCHEME", "http")).append("://")
                    .append(store.getDomainName()).append("/")
                    .append(coreConfiguration.getProperty("CONTEXT_PATH", "sm-shop"));


            SetExpressCheckoutRequestDetailsType setExpressCheckoutRequestDetails = new SetExpressCheckoutRequestDetailsType();
            String returnUrl = RETURN_URL.toString() + new StringBuilder().append(SHOP_URI).append("/paypal/checkout").append(coreConfiguration.getProperty("URL_EXTENSION", ".html")).append("/success").toString();
            String cancelUrl = RETURN_URL.toString() + new StringBuilder().append(SHOP_URI).append("/paypal/checkout").append(coreConfiguration.getProperty("URL_EXTENSION", ".html")).append("/cancel").toString();

            setExpressCheckoutRequestDetails.setReturnURL(returnUrl);
            setExpressCheckoutRequestDetails.setCancelURL(cancelUrl);


            setExpressCheckoutRequestDetails.setPaymentDetails(paymentDetailsList);

            SetExpressCheckoutRequestType setExpressCheckoutRequest = new SetExpressCheckoutRequestType(setExpressCheckoutRequestDetails);
            setExpressCheckoutRequest.setVersion("104.0");

            SetExpressCheckoutReq setExpressCheckoutReq = new SetExpressCheckoutReq();
            setExpressCheckoutReq.setSetExpressCheckoutRequest(setExpressCheckoutRequest);


            String mode = "sandbox";
            String env = configuration.getEnvironment();
            if (PRODUCTION_ENVIRONMENT.equals(env)) {
                mode = "production";
            }

            Map<String, String> configurationMap = new HashMap<String, String>();
            configurationMap.put("mode", mode);
            configurationMap.put("acct1.UserName", configuration.getIntegrationKeys().get("username"));
            configurationMap.put("acct1.Password", configuration.getIntegrationKeys().get("api"));
            configurationMap.put("acct1.Signature", configuration.getIntegrationKeys().get("signature"));

            PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(configurationMap);
            SetExpressCheckoutResponseType setExpressCheckoutResponse = service.setExpressCheckout(setExpressCheckoutReq);

            String token = setExpressCheckoutResponse.getToken();
            String correlationID = setExpressCheckoutResponse.getCorrelationID();
            String ack = setExpressCheckoutResponse.getAck().getValue();

            if (!"Success".equals(ack)) {
                LOGGER.error("Wrong value from init transaction " + ack);
                throw new IntegrationException("Wrong paypal ack from init transaction " + ack);
            }

            Transaction transaction = new Transaction();
            transaction.setAmount(summary.getTotal());
            transaction.setTransactionDate(new Date());
            transaction.setTransactionType(INIT);
            transaction.setPaymentType(PAYPAL);
            transaction.getTransactionDetails().put("TOKEN", token);
            transaction.getTransactionDetails().put("CORRELATION", correlationID);


            return transaction;

        } catch (Exception e) {
            e.printStackTrace();
            throw new IntegrationException(e);
        }


    }

    @Override
    public Transaction authorizeAndCapture(MerchantStore store,
                                           Customer customer, List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
                                           IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {

        PaypalPayment paypalPayment = (PaypalPayment) payment;
        notNull(paypalPayment.getPaymentToken(), "A paypal payment token is required to process this transaction");

        return processTransaction(store, customer, items, amount, paypalPayment, configuration, module);


    }

    @Override
    public Transaction refund(boolean partial, MerchantStore store,
                              Transaction transaction, Order order, BigDecimal amount,
                              IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {


        try {


            notNull(transaction, "Transaction cannot be null");
            notNull((String) transaction.getTransactionDetails().get("TRANSACTIONID"), "Transaction details must contain a TRANSACTIONID");
            notNull(order, "Order must not be null");
            notNull(order.getCurrency(), "Order nust contain Currency object");

            String mode = "sandbox";
            String env = configuration.getEnvironment();
            if (PRODUCTION_ENVIRONMENT.equals(env)) {
                mode = "production";
            }


            RefundTransactionRequestType refundTransactionRequest = new RefundTransactionRequestType();
            refundTransactionRequest.setVersion("104.0");

            RefundTransactionReq refundRequest = new RefundTransactionReq();
            refundRequest.setRefundTransactionRequest(refundTransactionRequest);


            Map<String, String> configurationMap = new HashMap<String, String>();
            configurationMap.put("mode", mode);
            configurationMap.put("acct1.UserName", configuration.getIntegrationKeys().get("username"));
            configurationMap.put("acct1.Password", configuration.getIntegrationKeys().get("api"));
            configurationMap.put("acct1.Signature", configuration.getIntegrationKeys().get("signature"));


            PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(configurationMap);


            RefundType refundType = FULL;
            if (partial) {
                refundType = PARTIAL;
            }

            refundTransactionRequest.setRefundType(refundType);

            BasicAmountType refundAmount = new BasicAmountType();
            refundAmount.setValue(pricingService.getStringAmount(amount, store));
            refundAmount.setCurrencyID(fromValue(order.getCurrency().getCode()));

            refundTransactionRequest.setAmount(refundAmount);
            refundTransactionRequest.setTransactionID(transaction.getTransactionDetails().get("TRANSACTIONID"));

            RefundTransactionResponseType refundTransactionResponse = service.refundTransaction(refundRequest);

            String refundAck = refundTransactionResponse.getAck().getValue();


            if (!"Success".equals(refundAck)) {
                LOGGER.error("Wrong value from transaction commit " + refundAck);
                throw new IntegrationException(EXCEPTION_TRANSACTION_DECLINED, "Paypal refund transaction code [" + refundTransactionResponse.getErrors().get(0).getErrorCode() + "], message-> " + refundTransactionResponse.getErrors().get(0).getShortMessage());
            }


            Transaction newTransaction = new Transaction();
            newTransaction.setAmount(amount);
            newTransaction.setTransactionDate(new Date());
            newTransaction.setTransactionType(REFUND);
            newTransaction.setPaymentType(PAYPAL);
            newTransaction.getTransactionDetails().put("TRANSACTIONID", refundTransactionResponse.getRefundTransactionID());
            transaction.getTransactionDetails().put("CORRELATION", refundTransactionResponse.getCorrelationID());


            return newTransaction;


        } catch (Exception e) {
            if (e instanceof IntegrationException) {
                throw (IntegrationException) e;
            } else {
                throw new IntegrationException(e);
            }
        }


    }

    private Transaction processTransaction(MerchantStore store,
                                           Customer customer, List<ShoppingCartItem> items, BigDecimal amount, Payment payment,
                                           IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {


        PaypalPayment paypalPayment = (PaypalPayment) payment;

        try {


            String mode = "sandbox";
            String env = configuration.getEnvironment();
            if (PRODUCTION_ENVIRONMENT.equals(env)) {
                mode = "production";
            }

            GetExpressCheckoutDetailsRequestType getExpressCheckoutDetailsRequest = new GetExpressCheckoutDetailsRequestType(paypalPayment.getPaymentToken());
            getExpressCheckoutDetailsRequest.setVersion("104.0");

            GetExpressCheckoutDetailsReq getExpressCheckoutDetailsReq = new GetExpressCheckoutDetailsReq();
            getExpressCheckoutDetailsReq.setGetExpressCheckoutDetailsRequest(getExpressCheckoutDetailsRequest);

            Map<String, String> configurationMap = new HashMap<String, String>();
            configurationMap.put("mode", mode);
            configurationMap.put("acct1.UserName", configuration.getIntegrationKeys().get("username"));
            configurationMap.put("acct1.Password", configuration.getIntegrationKeys().get("api"));
            configurationMap.put("acct1.Signature", configuration.getIntegrationKeys().get("signature"));


            PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(configurationMap);
            GetExpressCheckoutDetailsResponseType getExpressCheckoutDetailsResponse = service.getExpressCheckoutDetails(getExpressCheckoutDetailsReq);


            String token = getExpressCheckoutDetailsResponse.getGetExpressCheckoutDetailsResponseDetails().getToken();
            String correlationID = getExpressCheckoutDetailsResponse.getCorrelationID();
            String ack = getExpressCheckoutDetailsResponse.getAck().getValue();
            String payerId = getExpressCheckoutDetailsResponse.getGetExpressCheckoutDetailsResponseDetails().getPayerInfo().getPayerID();

            if (!"Success".equals(ack)) {
                LOGGER.error("Wrong value from anthorize and capture transaction " + ack);
                throw new IntegrationException("Wrong paypal ack from init transaction " + ack);
            }


            PaymentDetailsType paymentDetail = new PaymentDetailsType();
            BasicAmountType orderTotal = new BasicAmountType();
            orderTotal.setValue(pricingService.getStringAmount(amount, store));
            orderTotal.setCurrencyID(fromValue(payment.getCurrency().getCode()));
            paymentDetail.setOrderTotal(orderTotal);
            paymentDetail.setButtonSource("Shopizer_Cart_AP");
            if (payment.getTransactionType().name().equals(AUTHORIZE.name())) {
                paymentDetail.setPaymentAction(AUTHORIZATION);
            } else {
                paymentDetail.setPaymentAction(SALE);
            }

            List<PaymentDetailsType> paymentDetails = new ArrayList<PaymentDetailsType>();
            paymentDetails.add(paymentDetail);

            DoExpressCheckoutPaymentRequestDetailsType doExpressCheckoutPaymentRequestDetails = new DoExpressCheckoutPaymentRequestDetailsType();
            doExpressCheckoutPaymentRequestDetails.setToken(token);
            doExpressCheckoutPaymentRequestDetails.setPayerID(payerId);
            doExpressCheckoutPaymentRequestDetails.setPaymentDetails(paymentDetails);

            DoExpressCheckoutPaymentRequestType doExpressCheckoutPaymentRequest = new DoExpressCheckoutPaymentRequestType(doExpressCheckoutPaymentRequestDetails);
            doExpressCheckoutPaymentRequest.setVersion("104.0");

            DoExpressCheckoutPaymentReq doExpressCheckoutPaymentReq = new DoExpressCheckoutPaymentReq();
            doExpressCheckoutPaymentReq.setDoExpressCheckoutPaymentRequest(doExpressCheckoutPaymentRequest);


            DoExpressCheckoutPaymentResponseType doExpressCheckoutPaymentResponse = service.doExpressCheckoutPayment(doExpressCheckoutPaymentReq);
            String commitAck = doExpressCheckoutPaymentResponse.getAck().getValue();


            if (!"Success".equals(commitAck)) {
                LOGGER.error("Wrong value from transaction commit " + ack);
                throw new IntegrationException("Wrong paypal ack from init transaction " + ack);
            }


            List<PaymentInfoType> paymentInfoList = doExpressCheckoutPaymentResponse.getDoExpressCheckoutPaymentResponseDetails().getPaymentInfo();
            String transactionId = null;

            for (PaymentInfoType paymentInfo : paymentInfoList) {
                transactionId = paymentInfo.getTransactionID();
            }

            Transaction transaction = new Transaction();
            transaction.setAmount(amount);
            transaction.setTransactionDate(new Date());
            transaction.setTransactionType(payment.getTransactionType());
            transaction.setPaymentType(PAYPAL);
            transaction.getTransactionDetails().put("TOKEN", token);
            transaction.getTransactionDetails().put("PAYERID", payerId);
            transaction.getTransactionDetails().put("TRANSACTIONID", transactionId);
            transaction.getTransactionDetails().put("CORRELATION", correlationID);


            return transaction;


        } catch (Exception e) {
            throw new IntegrationException(e);
        }


    }

    @Override
    public Transaction capture(MerchantStore store, Customer customer,
                               Order order, Transaction capturableTransaction,
                               IntegrationConfiguration configuration, IntegrationModule module)
            throws IntegrationException {


        try {


            notNull(capturableTransaction, "Transaction cannot be null");
            notNull((String) capturableTransaction.getTransactionDetails().get("TRANSACTIONID"), "Transaction details must contain a TRANSACTIONID");
            notNull(order, "Order must not be null");
            notNull(order.getCurrency(), "Order nust contain Currency object");

            String mode = "sandbox";
            String env = configuration.getEnvironment();
            if (PRODUCTION_ENVIRONMENT.equals(env)) {
                mode = "production";
            }


            Map<String, String> configurationMap = new HashMap<String, String>();
            configurationMap.put("mode", mode);
            configurationMap.put("acct1.UserName", configuration.getIntegrationKeys().get("username"));
            configurationMap.put("acct1.Password", configuration.getIntegrationKeys().get("api"));
            configurationMap.put("acct1.Signature", configuration.getIntegrationKeys().get("signature"));


            DoCaptureReq doCaptureReq = new DoCaptureReq();


            BasicAmountType amount = new BasicAmountType();
            amount.setValue(pricingService.getStringAmount(order.getTotal(), store));
            amount.setCurrencyID(fromValue(order.getCurrency().getCode()));
            DoCaptureRequestType doCaptureRequest = new DoCaptureRequestType(
                    (String) capturableTransaction.getTransactionDetails().get("TRANSACTIONID"), amount, NOTCOMPLETE);

            doCaptureReq.setDoCaptureRequest(doCaptureRequest);
            PayPalAPIInterfaceServiceService service = new PayPalAPIInterfaceServiceService(configurationMap);

            DoCaptureResponseType doCaptureResponse = null;
            doCaptureResponse = service
                    .doCapture(doCaptureReq);
            if (!"Success".equals(doCaptureResponse.getAck().getValue())) {
                LOGGER.error("Wrong value from transaction commit " + doCaptureResponse.getAck().getValue());
                throw new IntegrationException("Wrong paypal ack from refund transaction " + doCaptureResponse.getAck().getValue());
            }


            Transaction newTransaction = new Transaction();
            newTransaction.setAmount(order.getTotal());
            newTransaction.setTransactionDate(new Date());
            newTransaction.setTransactionType(CAPTURE);
            newTransaction.setPaymentType(PAYPAL);
            newTransaction.getTransactionDetails().put("AUTHORIZATIONID", doCaptureResponse.getDoCaptureResponseDetails().getAuthorizationID());
            newTransaction.getTransactionDetails().put("TRANSACTIONID", (String) capturableTransaction.getTransactionDetails().get("TRANSACTIONID"));

            return newTransaction;


        } catch (Exception e) {
            throw new IntegrationException(e);
        }


    }

}
