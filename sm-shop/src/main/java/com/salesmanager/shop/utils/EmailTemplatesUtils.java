package com.salesmanager.shop.utils;

import com.salesmanager.core.business.modules.email.Email;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.system.EmailService;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.OrderTotal;
import com.salesmanager.core.model.order.orderproduct.OrderProduct;
import com.salesmanager.core.model.order.orderstatus.OrderStatusHistory;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;
import com.salesmanager.shop.constants.ApplicationConstants;
import com.salesmanager.shop.constants.EmailConstants;
import com.salesmanager.shop.model.customer.PersistableCustomer;
import com.salesmanager.shop.model.shop.ContactForm;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static com.salesmanager.shop.constants.ApplicationConstants.MAX_DOWNLOAD_DAYS;
import static com.salesmanager.shop.constants.EmailConstants.*;
import static com.salesmanager.shop.utils.DateUtil.formatDate;
import static com.salesmanager.shop.utils.DateUtil.formatLongDate;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


@Component
public class EmailTemplatesUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplatesUtils.class);
	
	@Inject
	private EmailService emailService;

	@Inject
	private LabelUtils messages;
	
	@Inject
	private CountryService countryService;
	
	@Inject
	private ProductService productService;
	
	@Inject
	private ZoneService zoneService;
	
	@Inject
	private PricingService pricingService;
	
	@Inject
	@Qualifier("img")
	private ImageFilePath imageUtils;
	
	@Inject
	private EmailUtils emailUtils;
	
	@Inject
	private FilePathUtils filePathUtils;
	
	private final static String LINE_BREAK = "<br/>";
	private final static String TABLE = "<table width=\"100%\">";
	private final static String CLOSING_TABLE = "</table>";
	private final static String TR = "<tr>";
	private final static String TR_BORDER = "<tr class=\"border\">";
	private final static String CLOSING_TR = "</tr>";
	private final static String TD = "<td valign=\"top\">";
	private final static String CLOSING_TD = "</td>";


    @Async
    public void sendOrderEmail(String toEmail, Customer customer, Order order, Locale customerLocale, Language language, MerchantStore merchantStore, String contextPath) {
        /** issue with putting that elsewhere **/
        LOGGER.info("Sending welcome email to customer");
        try {

            Map<String, Zone> zones = zoneService.getZones(language);

            Map<String, Country> countries = countryService.getCountriesMap(language);

            //format Billing address
            StringBuilder billing = new StringBuilder();
            if (isBlank(order.getBilling().getCompany())) {
                billing.append(order.getBilling().getFirstName()).append(" ")
                        .append(order.getBilling().getLastName()).append(LINE_BREAK);
            } else {
                billing.append(order.getBilling().getCompany()).append(LINE_BREAK);
            }
            billing.append(order.getBilling().getAddress()).append(LINE_BREAK);
            billing.append(order.getBilling().getCity()).append(", ");

            if (order.getBilling().getZone() != null) {
                Zone zone = zones.get(order.getBilling().getZone().getCode());
                if (zone != null) {
                    billing.append(zone.getName());
                }
                billing.append(LINE_BREAK);
            } else if (!isBlank(order.getBilling().getState())) {
                billing.append(order.getBilling().getState()).append(LINE_BREAK);
            }
            Country country = countries.get(order.getBilling().getCountry().getIsoCode());
            if (country != null) {
                billing.append(country.getName()).append(" ");
            }
            billing.append(order.getBilling().getPostalCode());


            //format shipping address
            StringBuilder shipping = null;
            if (order.getDelivery() != null && !isBlank(order.getDelivery().getFirstName())) {
                shipping = new StringBuilder();
                if (isBlank(order.getDelivery().getCompany())) {
                    shipping.append(order.getDelivery().getFirstName()).append(" ")
                            .append(order.getDelivery().getLastName()).append(LINE_BREAK);
                } else {
                    shipping.append(order.getDelivery().getCompany()).append(LINE_BREAK);
                }
                shipping.append(order.getDelivery().getAddress()).append(LINE_BREAK);
                shipping.append(order.getDelivery().getCity()).append(", ");

                if (order.getDelivery().getZone() != null) {
                    Zone zone = zones.get(order.getDelivery().getZone().getCode());
                    if (zone != null) {
                        shipping.append(zone.getName());
                    }
                    shipping.append(LINE_BREAK);
                } else if (!isBlank(order.getDelivery().getState())) {
                    shipping.append(order.getDelivery().getState()).append(LINE_BREAK);
                }
                Country deliveryCountry = countries.get(order.getDelivery().getCountry().getIsoCode());
                if (country != null) {
                    shipping.append(deliveryCountry.getName()).append(" ");
                }
                shipping.append(order.getDelivery().getPostalCode());
            }

            if (shipping == null && isNotBlank(order.getShippingModuleCode())) {
                //TODO IF HAS NO SHIPPING
                shipping = billing;
            }

            //format order
            //String storeUri = FilePathUtils.buildStoreUri(merchantStore, contextPath);
            StringBuilder orderTable = new StringBuilder();
            orderTable.append(TABLE);
            for (OrderProduct product : order.getOrderProducts()) {
                //Product productModel = productService.getByCode(product.getSku(), language);
                orderTable.append(TR);
                orderTable.append(TD).append(product.getProductName()).append(" - ").append(product.getSku()).append(CLOSING_TD);
                orderTable.append(TD).append(messages.getMessage("label.quantity", customerLocale)).append(": ").append(product.getProductQuantity()).append(CLOSING_TD);
                orderTable.append(TD).append(pricingService.getDisplayAmount(product.getOneTimeCharge(), merchantStore)).append(CLOSING_TD);
                orderTable.append(CLOSING_TR);
            }

            //order totals
            for (OrderTotal total : order.getOrderTotal()) {
                orderTable.append(TR_BORDER);
                //orderTable.append(TD);
                //orderTable.append(CLOSING_TD);
                orderTable.append(TD);
                orderTable.append(CLOSING_TD);
                orderTable.append(TD);
                orderTable.append("<strong>");
                if (total.getModule().equals("tax")) {
                    orderTable.append(total.getText()).append(": ");

                } else {
                    //if(total.getModule().equals("total") || total.getModule().equals("subtotal")) {
                    //}
                    orderTable.append(messages.getMessage(total.getOrderTotalCode(), customerLocale)).append(": ");
                    //if(total.getModule().equals("total") || total.getModule().equals("subtotal")) {

                    //}
                }
                orderTable.append("</strong>");
                orderTable.append(CLOSING_TD);
                orderTable.append(TD);
                orderTable.append("<strong>");

                orderTable.append(pricingService.getDisplayAmount(total.getValue(), merchantStore));

                orderTable.append("</strong>");
                orderTable.append(CLOSING_TD);
                orderTable.append(CLOSING_TR);
            }
            orderTable.append(CLOSING_TABLE);

            Map<String, String> templateTokens = emailUtils.createEmailObjectsMap(contextPath, merchantStore, messages, customerLocale);
            templateTokens.put(LABEL_HI, messages.getMessage("label.generic.hi", customerLocale));
            templateTokens.put(EMAIL_CUSTOMER_FIRSTNAME, order.getBilling().getFirstName());
            templateTokens.put(EMAIL_CUSTOMER_LASTNAME, order.getBilling().getLastName());

            String[] params = {valueOf(order.getId())};
            String[] dt = {formatDate(order.getDatePurchased())};
            templateTokens.put(EMAIL_ORDER_NUMBER, messages.getMessage("email.order.confirmation", params, customerLocale));
            templateTokens.put(EMAIL_ORDER_DATE, messages.getMessage("email.order.ordered", dt, customerLocale));
            templateTokens.put(EMAIL_ORDER_THANKS, messages.getMessage("email.order.thanks", customerLocale));
            templateTokens.put(ADDRESS_BILLING, billing.toString());

            templateTokens.put(ORDER_PRODUCTS_DETAILS, orderTable.toString());
            templateTokens.put(EMAIL_ORDER_DETAILS_TITLE, messages.getMessage("label.order.details", customerLocale));
            templateTokens.put(ADDRESS_BILLING_TITLE, messages.getMessage("label.customer.billinginformation", customerLocale));
            templateTokens.put(PAYMENT_METHOD_TITLE, messages.getMessage("label.order.paymentmode", customerLocale));
            templateTokens.put(PAYMENT_METHOD_DETAILS, messages.getMessage(new StringBuilder().append("payment.type.").append(order.getPaymentType().name()).toString(), customerLocale, order.getPaymentType().name()));

            if (isNotBlank(order.getShippingModuleCode())) {
                templateTokens.put(SHIPPING_METHOD_DETAILS, messages.getMessage(new StringBuilder().append("module.shipping.").append(order.getShippingModuleCode()).toString(), customerLocale, order.getShippingModuleCode()));
                templateTokens.put(ADDRESS_SHIPPING_TITLE, messages.getMessage("label.order.shippingmethod", customerLocale));
                templateTokens.put(ADDRESS_DELIVERY_TITLE, messages.getMessage("label.customer.shippinginformation", customerLocale));
                templateTokens.put(SHIPPING_METHOD_TITLE, messages.getMessage("label.customer.shippinginformation", customerLocale));
                templateTokens.put(ADDRESS_DELIVERY, shipping.toString());
            } else {
                templateTokens.put(SHIPPING_METHOD_DETAILS, "");
                templateTokens.put(ADDRESS_SHIPPING_TITLE, "");
                templateTokens.put(ADDRESS_DELIVERY_TITLE, "");
                templateTokens.put(SHIPPING_METHOD_TITLE, "");
                templateTokens.put(ADDRESS_DELIVERY, "");
            }

            String status = messages.getMessage("label.order." + order.getStatus().name(), customerLocale, order.getStatus().name());
            String[] statusMessage = {formatDate(order.getDatePurchased()), status};
            templateTokens.put(ORDER_STATUS, messages.getMessage("email.order.status", statusMessage, customerLocale));


            String[] title = {merchantStore.getStorename(), valueOf(order.getId())};
            Email email = new Email();
            email.setFrom(merchantStore.getStorename());
            email.setFromEmail(merchantStore.getStoreEmailAddress());
            email.setSubject(messages.getMessage("email.order.title", title, customerLocale));
            email.setTo(toEmail);
            email.setTemplateName(EMAIL_ORDER_TPL);
            email.setTemplateTokens(templateTokens);

            LOGGER.debug("Sending email to {} for order id {} ", customer.getEmailAddress(), order.getId());
            emailService.sendHtmlEmail(merchantStore, email);

        } catch (Exception e) {
            LOGGER.error("Error occured while sending order confirmation email ", e);
        }

    }

    @Async
    public void sendRegistrationEmail(
            PersistableCustomer customer, MerchantStore merchantStore,
            Locale customerLocale, String contextPath) {
        /** issue with putting that elsewhere **/
        LOGGER.info("Sending welcome email to customer");
        try {

            Map<String, String> templateTokens = emailUtils.createEmailObjectsMap(contextPath, merchantStore, messages, customerLocale);
            templateTokens.put(LABEL_HI, messages.getMessage("label.generic.hi", customerLocale));
            templateTokens.put(EMAIL_CUSTOMER_FIRSTNAME, customer.getBilling().getFirstName());
            templateTokens.put(EMAIL_CUSTOMER_LASTNAME, customer.getBilling().getLastName());
            String[] greetingMessage = {merchantStore.getStorename(), filePathUtils.buildCustomerUri(merchantStore, contextPath), merchantStore.getStoreEmailAddress()};
            templateTokens.put(EMAIL_CUSTOMER_GREETING, messages.getMessage("email.customer.greeting", greetingMessage, customerLocale));
            templateTokens.put(EMAIL_USERNAME_LABEL, messages.getMessage("label.generic.username", customerLocale));
            templateTokens.put(EMAIL_PASSWORD_LABEL, messages.getMessage("label.generic.password", customerLocale));
            templateTokens.put(CUSTOMER_ACCESS_LABEL, messages.getMessage("label.customer.accessportal", customerLocale));
            templateTokens.put(ACCESS_NOW_LABEL, messages.getMessage("label.customer.accessnow", customerLocale));
            templateTokens.put(EMAIL_USER_NAME, customer.getUserName());
            templateTokens.put(EMAIL_CUSTOMER_PASSWORD, customer.getClearPassword());

            //shop url
            String customerUrl = filePathUtils.buildStoreUri(merchantStore, contextPath);
            templateTokens.put(CUSTOMER_ACCESS_URL, customerUrl);

            Email email = new Email();
            email.setFrom(merchantStore.getStorename());
            email.setFromEmail(merchantStore.getStoreEmailAddress());
            email.setSubject(messages.getMessage("email.newuser.title", customerLocale));
            email.setTo(customer.getEmailAddress());
            email.setTemplateName(EMAIL_CUSTOMER_TPL);
            email.setTemplateTokens(templateTokens);

            LOGGER.debug("Sending email to {} on their  registered email id {} ", customer.getBilling().getFirstName(), customer.getEmailAddress());
            emailService.sendHtmlEmail(merchantStore, email);

        } catch (Exception e) {
            LOGGER.error("Error occured while sending welcome email ", e);
        }

    }
	
	public void sendContactEmail(
			ContactForm contact, MerchantStore merchantStore,
				Locale storeLocale, String contextPath) {
        LOGGER.info("Sending welcome email to customer");
        try {

            Map<String, String> templateTokens = emailUtils.createEmailObjectsMap(contextPath, merchantStore, messages, storeLocale);

            templateTokens.put(EMAIL_CONTACT_NAME, contact.getName());
            templateTokens.put(EMAIL_CONTACT_EMAIL, contact.getEmail());
            templateTokens.put(EMAIL_CONTACT_CONTENT, contact.getComment());

            String[] contactSubject = {contact.getSubject()};

            templateTokens.put(EMAIL_CUSTOMER_CONTACT, messages.getMessage("email.contact", contactSubject, storeLocale));
            templateTokens.put(EMAIL_CONTACT_NAME_LABEL, messages.getMessage("label.entity.name", storeLocale));
            templateTokens.put(EMAIL_CONTACT_EMAIL_LABEL, messages.getMessage("label.generic.email", storeLocale));


            Email email = new Email();
            email.setFrom(merchantStore.getStorename());
            email.setFromEmail(contact.getEmail());
            email.setSubject(messages.getMessage("email.contact.title", storeLocale));
            email.setTo(merchantStore.getStoreEmailAddress());
            email.setTemplateName(EMAIL_CONTACT_TMPL);
            email.setTemplateTokens(templateTokens);

            LOGGER.debug("Sending contact email");
            emailService.sendHtmlEmail(merchantStore, email);

        } catch (Exception e) {
            LOGGER.error("Error occured while sending contact email ", e);
        }

    }

    @Async
    public void sendUpdateOrderStatusEmail(
            Customer customer, Order order, OrderStatusHistory lastHistory, MerchantStore merchantStore,
            Locale customerLocale, String contextPath) {
        /** issue with putting that elsewhere **/
        LOGGER.info("Sending order status email to customer");
        try {


            Map<String, String> templateTokens = emailUtils.createEmailObjectsMap(contextPath, merchantStore, messages, customerLocale);

            templateTokens.put(LABEL_HI, messages.getMessage("label.generic.hi", customerLocale));
            templateTokens.put(EMAIL_CUSTOMER_FIRSTNAME, customer.getBilling().getFirstName());
            templateTokens.put(EMAIL_CUSTOMER_LASTNAME, customer.getBilling().getLastName());

            String[] statusMessageText = {valueOf(order.getId()), formatDate(order.getDatePurchased())};
            String status = messages.getMessage("label.order." + order.getStatus().name(), customerLocale, order.getStatus().name());
            String[] statusMessage = {formatDate(lastHistory.getDateAdded()), status};

            String comments = lastHistory.getComments();
            if (isBlank(comments)) {
                comments = messages.getMessage("label.order." + order.getStatus().name(), customerLocale, order.getStatus().name());
            }

            templateTokens.put(EMAIL_ORDER_STATUS_TEXT, messages.getMessage("email.order.statustext", statusMessageText, customerLocale));
            templateTokens.put(EMAIL_ORDER_STATUS, messages.getMessage("email.order.status", statusMessage, customerLocale));
            templateTokens.put(EMAIL_TEXT_STATUS_COMMENTS, comments);


            Email email = new Email();
            email.setFrom(merchantStore.getStorename());
            email.setFromEmail(merchantStore.getStoreEmailAddress());
            email.setSubject(messages.getMessage("email.order.status.title", new String[]{valueOf(order.getId())}, customerLocale));
            email.setTo(customer.getEmailAddress());
            email.setTemplateName(ORDER_STATUS_TMPL);
            email.setTemplateTokens(templateTokens);


            emailService.sendHtmlEmail(merchantStore, email);

        } catch (Exception e) {
            LOGGER.error("Error occured while sending order download email ", e);
        }

    }

    @Async
    public void sendOrderDownloadEmail(
            Customer customer, Order order, MerchantStore merchantStore,
            Locale customerLocale, String contextPath) {
        /** issue with putting that elsewhere **/
        LOGGER.info("Sending download email to customer");
        try {

            Map<String, String> templateTokens = emailUtils.createEmailObjectsMap(contextPath, merchantStore, messages, customerLocale);
            templateTokens.put(LABEL_HI, messages.getMessage("label.generic.hi", customerLocale));
            templateTokens.put(EMAIL_CUSTOMER_FIRSTNAME, customer.getBilling().getFirstName());
            templateTokens.put(EMAIL_CUSTOMER_LASTNAME, customer.getBilling().getLastName());
            String[] downloadMessage = {valueOf(MAX_DOWNLOAD_DAYS), valueOf(order.getId()), filePathUtils.buildCustomerUri(merchantStore, contextPath), merchantStore.getStoreEmailAddress()};
            templateTokens.put(EMAIL_ORDER_DOWNLOAD, messages.getMessage("email.order.download.text", downloadMessage, customerLocale));
            templateTokens.put(CUSTOMER_ACCESS_LABEL, messages.getMessage("label.customer.accessportal", customerLocale));
            templateTokens.put(ACCESS_NOW_LABEL, messages.getMessage("label.customer.accessnow", customerLocale));

            //shop url
            String customerUrl = filePathUtils.buildStoreUri(merchantStore, contextPath);
            templateTokens.put(CUSTOMER_ACCESS_URL, customerUrl);

            String[] orderInfo = {valueOf(order.getId())};

            Email email = new Email();
            email.setFrom(merchantStore.getStorename());
            email.setFromEmail(merchantStore.getStoreEmailAddress());
            email.setSubject(messages.getMessage("email.order.download.title", orderInfo, customerLocale));
            email.setTo(customer.getEmailAddress());
            email.setTemplateName(EMAIL_ORDER_DOWNLOAD_TPL);
            email.setTemplateTokens(templateTokens);

            LOGGER.debug("Sending email to {} with download info", customer.getEmailAddress());
            emailService.sendHtmlEmail(merchantStore, email);

        } catch (Exception e) {
            LOGGER.error("Error occured while sending order download email ", e);
        }

    }

    @Async
    public void changePasswordNotificationEmail(
            Customer customer, MerchantStore merchantStore,
            Locale customerLocale, String contextPath) {
        LOGGER.debug("Sending change password email");
        try {


            Map<String, String> templateTokens = emailUtils.createEmailObjectsMap(contextPath, merchantStore, messages, customerLocale);

            templateTokens.put(LABEL_HI, messages.getMessage("label.generic.hi", customerLocale));
            templateTokens.put(EMAIL_CUSTOMER_FIRSTNAME, customer.getBilling().getFirstName());
            templateTokens.put(EMAIL_CUSTOMER_LASTNAME, customer.getBilling().getLastName());

            String[] date = {formatLongDate(new Date())};

            templateTokens.put(EMAIL_NOTIFICATION_MESSAGE, messages.getMessage("label.notification.message.passwordchanged", date, customerLocale));


            Email email = new Email();
            email.setFrom(merchantStore.getStorename());
            email.setFromEmail(merchantStore.getStoreEmailAddress());
            email.setSubject(messages.getMessage("label.notification.title.passwordchanged", customerLocale));
            email.setTo(customer.getEmailAddress());
            email.setTemplateName(EMAIL_NOTIFICATION_TMPL);
            email.setTemplateTokens(templateTokens);


            emailService.sendHtmlEmail(merchantStore, email);

        } catch (Exception e) {
            LOGGER.error("Error occured while sending change password email ", e);
        }

    }

}