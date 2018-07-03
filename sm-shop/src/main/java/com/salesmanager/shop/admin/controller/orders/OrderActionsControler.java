package com.salesmanager.shop.admin.controller.orders;

import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.order.OrderService;
import com.salesmanager.core.business.services.payments.PaymentService;
import com.salesmanager.core.business.services.payments.TransactionService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.system.EmailService;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.orderstatus.OrderStatusHistory;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.shop.admin.model.orders.Refund;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.utils.DateUtil;
import com.salesmanager.shop.utils.EmailTemplatesUtils;
import com.salesmanager.shop.utils.LabelUtils;
import com.salesmanager.shop.utils.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;

import static com.salesmanager.core.business.utils.ajax.AjaxResponse.RESPONSE_OPERATION_COMPLETED;
import static com.salesmanager.core.business.utils.ajax.AjaxResponse.RESPONSE_STATUS_FAIURE;
import static com.salesmanager.shop.constants.Constants.ADMIN_STORE;
import static com.salesmanager.shop.utils.DateUtil.formatLongDate;
import static com.salesmanager.shop.utils.LocaleUtils.getLocale;
import static java.lang.Long.parseLong;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class OrderActionsControler {

	private static final Logger LOGGER = getLogger(OrderActionsControler.class);

	@Inject
	private LabelUtils messages;

	@Inject
	private OrderService orderService;

	@Inject
	CountryService countryService;

	@Inject
	ZoneService zoneService;

	@Inject
	PaymentService paymentService;

	@Inject
	CustomerService customerService;

	@Inject
	PricingService pricingService;

	@Inject
	TransactionService transactionService;

	@Inject
	EmailService emailService;

	@Inject
	EmailTemplatesUtils emailTemplatesUtils;


	@PreAuthorize("hasRole('ORDER')")
	@RequestMapping(value = "/admin/orders/captureOrder.html", method = POST)
	public @ResponseBody
	ResponseEntity<String> captureOrder(HttpServletRequest request, HttpServletResponse response, Locale locale) {


		MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);

		String sId = request.getParameter("id");

		AjaxResponse resp = new AjaxResponse();
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(APPLICATION_JSON_UTF8);

		try {
			Long id = parseLong(sId);

			Order order = orderService.getById(id);

			if (order == null) {

				LOGGER.error("Order {0} does not exists", id);
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			if (order.getMerchant().getId().intValue() != store.getId().intValue()) {

				LOGGER.error("Merchant store does not have order {0}", id);
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			Customer customer = customerService.getById(order.getCustomerId());

			if (customer == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setStatusMessage(messages.getMessage("message.notexist.customer", locale));
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			paymentService.processCapturePayment(order, customer, store);

			resp.setStatus(RESPONSE_OPERATION_COMPLETED);

		} catch (IntegrationException e) {
			LOGGER.error("Error while processing capture", e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorString(messages.getMessage(e.getMessageCode(), locale));
		} catch (Exception e) {
			LOGGER.error("Error while getting category", e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorMessage(e);
		}

		String returnString = resp.toJSONString();
		return new ResponseEntity<String>(returnString, httpHeaders, OK);
	}

	@PreAuthorize("hasRole('ORDER')")
	@RequestMapping(value = "/admin/orders/refundOrder.html", method = POST)
	public @ResponseBody
	ResponseEntity<String> refundOrder(@RequestBody Refund refund, HttpServletRequest request, HttpServletResponse response, Locale locale) {


		MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);


		AjaxResponse resp = new AjaxResponse();
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(APPLICATION_JSON_UTF8);

		BigDecimal submitedAmount = null;

		try {

			Order order = orderService.getById(refund.getOrderId());

			if (order == null) {

				LOGGER.error("Order {0} does not exists", refund.getOrderId());
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			if (order.getMerchant().getId().intValue() != store.getId().intValue()) {

				LOGGER.error("Merchant store does not have order {0}", refund.getOrderId());
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			//parse amount
			try {
				submitedAmount = new BigDecimal(refund.getAmount());
				if (submitedAmount.doubleValue() == 0) {
					resp.setStatus(RESPONSE_STATUS_FAIURE);
					resp.setStatusMessage(messages.getMessage("message.invalid.amount", locale));
					String returnString = resp.toJSONString();
					return new ResponseEntity<String>(returnString, httpHeaders, OK);
				}

			} catch (Exception e) {
				LOGGER.equals("invalid refundAmount " + refund.getAmount());
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}


			BigDecimal orderTotal = order.getTotal();
			if (submitedAmount.doubleValue() > orderTotal.doubleValue()) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setStatusMessage(messages.getMessage("message.invalid.amount", locale));
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			if (submitedAmount.doubleValue() <= 0) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setStatusMessage(messages.getMessage("message.invalid.amount", locale));
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			Customer customer = customerService.getById(order.getCustomerId());

			if (customer == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setStatusMessage(messages.getMessage("message.notexist.customer", locale));
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}


			paymentService.processRefund(order, customer, store, submitedAmount);

			resp.setStatus(RESPONSE_OPERATION_COMPLETED);
		} catch (IntegrationException e) {
			LOGGER.error("Error while processing refund", e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorString(messages.getMessage(e.getMessageCode(), locale));
		} catch (Exception e) {
			LOGGER.error("Error while processing refund", e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorMessage(e);
		}

		String returnString = resp.toJSONString();
		return new ResponseEntity<String>(returnString, httpHeaders, OK);
	}

	@PreAuthorize("hasRole('ORDER')")
	@RequestMapping(value = "/admin/orders/printInvoice.html", method = GET)
	public void printInvoice(HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {

		String sId = request.getParameter("id");

		try {

			Long id = parseLong(sId);

			MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);
			Order order = orderService.getById(id);

			if (order.getMerchant().getId().intValue() != store.getId().intValue()) {
				throw new Exception("Invalid order");
			}


			Language lang = store.getDefaultLanguage();


			ByteArrayOutputStream stream = orderService.generateInvoice(store, order, lang);
			StringBuilder attachment = new StringBuilder();
			//attachment.append("attachment; filename=");
			attachment.append(order.getId());
			attachment.append(".pdf");

			response.setHeader("Content-disposition", "attachment;filename=" + attachment.toString());

			//Set the mime type for the response
			response.setContentType("application/pdf");


			response.getOutputStream().write(stream.toByteArray());

			response.flushBuffer();


		} catch (Exception e) {
			LOGGER.error("Error while printing a report", e);
		}


	}


	@SuppressWarnings("unchecked")
	@PreAuthorize("hasRole('ORDER')")
	@RequestMapping(value = "/admin/orders/listTransactions.html", method = GET)
	public @ResponseBody
	ResponseEntity<String> listTransactions(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String sId = request.getParameter("id");

		MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);


		AjaxResponse resp = new AjaxResponse();
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(APPLICATION_JSON_UTF8);

		if (sId == null) {
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			String returnString = resp.toJSONString();
			return new ResponseEntity<String>(returnString, httpHeaders, OK);
		}


		try {

			Long id = parseLong(sId);


			Order dbOrder = orderService.getById(id);

			if (dbOrder == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}


			if (dbOrder.getMerchant().getId().intValue() != store.getId().intValue()) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}


			List<Transaction> transactions = transactionService.listTransactions(dbOrder);

			if (transactions != null) {

				for (Transaction transaction : transactions) {
					@SuppressWarnings("rawtypes")
					Map entry = new HashMap();
					entry.put("transactionId", transaction.getId());
					entry.put("transactionDate", formatLongDate(transaction.getTransactionDate()));
					entry.put("transactionType", transaction.getTransactionType().name());
					entry.put("paymentType", transaction.getPaymentType().name());
					entry.put("transactionAmount", pricingService.getStringAmount(transaction.getAmount(), store));
					entry.put("transactionDetails", transaction.getTransactionDetails());
					resp.addDataEntry(entry);
				}


			}


			resp.setStatus(RESPONSE_OPERATION_COMPLETED);

		} catch (Exception e) {
			LOGGER.error("Cannot get transactions for order id " + sId, e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorMessage(e);
		}

		String returnString = resp.toJSONString();
		return new ResponseEntity<String>(returnString, httpHeaders, OK);


	}


	@PreAuthorize("hasRole('ORDER')")
	@RequestMapping(value = "/admin/orders/sendInvoice.html", method = GET)
	public @ResponseBody
	ResponseEntity<String> sendInvoice(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String sId = request.getParameter("id");
		MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);


		AjaxResponse resp = new AjaxResponse();
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(APPLICATION_JSON_UTF8);

		if (sId == null) {
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			String returnString = resp.toJSONString();
			return new ResponseEntity<String>(returnString, httpHeaders, OK);
		}


		try {

			Long id = parseLong(sId);


			Order dbOrder = orderService.getById(id);

			if (dbOrder == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}


			if (dbOrder.getMerchant().getId().intValue() != store.getId().intValue()) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			//get customer
			Customer customer = customerService.getById(dbOrder.getCustomerId());

			if (customer == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setErrorString("Customer does not exist");
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			Locale customerLocale = getLocale(customer.getDefaultLanguage());

			emailTemplatesUtils.sendOrderEmail(customer.getEmailAddress(), customer, dbOrder, customerLocale, customer.getDefaultLanguage(), store, request.getContextPath());


			resp.setStatus(RESPONSE_OPERATION_COMPLETED);

		} catch (Exception e) {
			LOGGER.error("Cannot get transactions for order id " + sId, e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorMessage(e);
		}

		String returnString = resp.toJSONString();
		return new ResponseEntity<String>(returnString, httpHeaders, OK);


	}


	@PreAuthorize("hasRole('ORDER')")
	@RequestMapping(value = "/admin/orders/updateStatus.html", method = GET)
	public @ResponseBody
	ResponseEntity<String> updateStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String sId = request.getParameter("id");
		MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);


		AjaxResponse resp = new AjaxResponse();
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(APPLICATION_JSON_UTF8);

		if (sId == null) {
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			String returnString = resp.toJSONString();
			return new ResponseEntity<String>(returnString, httpHeaders, OK);
		}


		try {

			Long id = parseLong(sId);


			Order dbOrder = orderService.getById(id);

			if (dbOrder == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}


			if (dbOrder.getMerchant().getId().intValue() != store.getId().intValue()) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			//get customer
			Customer customer = customerService.getById(dbOrder.getCustomerId());

			if (customer == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setErrorString("Customer does not exist");
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			Locale customerLocale = getLocale(customer.getDefaultLanguage());


			Set<OrderStatusHistory> orderStatus = dbOrder.getOrderHistory();
			OrderStatusHistory lastHistory = null;
			if (orderStatus != null) {
				int count = 1;
				for (OrderStatusHistory history : orderStatus) {
					if (count == orderStatus.size()) {
						lastHistory = history;
						break;
					}
					count++;
				}
			}

			if (lastHistory == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setErrorString("No history");
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}
			emailTemplatesUtils.sendUpdateOrderStatusEmail(customer, dbOrder, lastHistory, store, customerLocale, request.getContextPath());


			resp.setStatus(RESPONSE_OPERATION_COMPLETED);

		} catch (Exception e) {
			LOGGER.error("Cannot get transactions for order id " + sId, e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorString(e.getMessage());
			resp.setErrorMessage(e);
		}

		String returnString = resp.toJSONString();
		return new ResponseEntity<String>(returnString, httpHeaders, OK);


	}

	@PreAuthorize("hasRole('ORDER')")
	@RequestMapping(value = "/admin/orders/sendDownloadEmail.html", method = GET)
	public @ResponseBody
	ResponseEntity<String> sendDownloadEmail(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String sId = request.getParameter("id");
		MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);


		AjaxResponse resp = new AjaxResponse();
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(APPLICATION_JSON_UTF8);

		if (sId == null) {
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			String returnString = resp.toJSONString();
			return new ResponseEntity<String>(returnString, httpHeaders, OK);
		}


		try {

			Long id = parseLong(sId);


			Order dbOrder = orderService.getById(id);

			if (dbOrder == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}


			if (dbOrder.getMerchant().getId().intValue() != store.getId().intValue()) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			//get customer
			Customer customer = customerService.getById(dbOrder.getCustomerId());

			if (customer == null) {
				resp.setStatus(RESPONSE_STATUS_FAIURE);
				resp.setErrorString("Customer does not exist");
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString, httpHeaders, OK);
			}

			Locale customerLocale = getLocale(customer.getDefaultLanguage());


			emailTemplatesUtils.sendOrderDownloadEmail(customer, dbOrder, store, customerLocale, request.getContextPath());


			resp.setStatus(RESPONSE_OPERATION_COMPLETED);

		} catch (Exception e) {
			LOGGER.error("Cannot get transactions for order id " + sId, e);
			resp.setStatus(RESPONSE_STATUS_FAIURE);
			resp.setErrorString(e.getMessage());
			resp.setErrorMessage(e);
		}

		String returnString = resp.toJSONString();
		return new ResponseEntity<String>(returnString, httpHeaders, OK);


	}


}
