package com.salesmanager.shop.store.controller.order;

import com.salesmanager.core.business.modules.integration.payment.impl.PayPalExpressCheckoutPayment;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionService;
import com.salesmanager.core.business.services.customer.attribute.CustomerOptionValueService;
import com.salesmanager.core.business.services.order.OrderService;
import com.salesmanager.core.business.services.payments.PaymentService;
import com.salesmanager.core.business.services.payments.TransactionService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.shoppingcart.ShoppingCartService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.OrderTotalSummary;
import com.salesmanager.core.model.payments.PaypalPayment;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.shipping.ShippingSummary;
import com.salesmanager.core.model.shoppingcart.ShoppingCart;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.integration.payment.model.PaymentModule;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.order.ShopOrder;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.store.controller.order.facade.OrderFacade;
import com.salesmanager.shop.store.controller.shoppingCart.facade.ShoppingCartFacade;
import com.salesmanager.shop.utils.LabelUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.mobile.device.Device;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.*;

import static com.salesmanager.core.business.constants.Constants.PRODUCTION_ENVIRONMENT;
import static com.salesmanager.core.business.utils.ajax.AjaxResponse.*;
import static com.salesmanager.shop.constants.Constants.*;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping(Constants.SHOP_URI)
public class ShoppingOrderPaymentController extends AbstractController {
	
	private static final Logger LOGGER = LoggerFactory
	.getLogger(ShoppingOrderPaymentController.class);
	
	private final static String INIT_ACTION = "init";
	
	
	@Inject
	private ShoppingCartFacade shoppingCartFacade;
	
    @Inject
    private ShoppingCartService shoppingCartService;
	
    @Inject
	private LanguageService languageService;
	
	@Inject
	private PaymentService paymentService;	

	@Inject
	private OrderService orderService;
	
	@Inject
	private CountryService countryService;
	
	@Inject
	private ZoneService zoneService;
	
	@Inject
	private OrderFacade orderFacade;
	
	@Inject
	private LabelUtils messages;
	
	@Inject
	private PricingService pricingService;
	
	@Inject
	private CustomerService customerService;
	
	@Inject
	private CustomerOptionService customerOptionService;
	
	@Inject
	private CustomerOptionValueService customerOptionValueService;
	
	@Inject
	private TransactionService transactionService;
	
	@Inject
	private CoreConfiguration coreConfiguration;

    @RequestMapping(value = {"/order/payment/{action}/{paymentmethod}.html"}, method = RequestMethod.POST)
    public @ResponseBody
    String paymentAction(@Valid @ModelAttribute(value = "order") ShopOrder order, @PathVariable String action, @PathVariable String paymentmethod, Device device, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {


        Language language = (Language) request.getAttribute("LANGUAGE");
        MerchantStore store = (MerchantStore) request.getAttribute(MERCHANT_STORE);
        String shoppingCartCode = getSessionAttribute(SHOPPING_CART, request);

        notNull(shoppingCartCode, "shoppingCartCode does not exist in the session");
        AjaxResponse ajaxResponse = new AjaxResponse();

        try {


            ShoppingCart cart = shoppingCartFacade.getShoppingCartModel(shoppingCartCode, store);

            Set<ShoppingCartItem> items = cart.getLineItems();
            List<ShoppingCartItem> cartItems = new ArrayList<ShoppingCartItem>(items);
            order.setShoppingCartItems(cartItems);

            //validate order first
            Map<String, String> messages = new TreeMap<String, String>();
            orderFacade.validateOrder(order, new BeanPropertyBindingResult(order, "order"), messages, store, locale);

            if (isNotEmpty(messages.values())) {
                for (String key : messages.keySet()) {
                    String value = messages.get(key);
                    ajaxResponse.addValidationMessage(key, value);
                }
                ajaxResponse.setStatus(RESPONSE_STATUS_VALIDATION_FAILED);
                return ajaxResponse.toJSONString();
            }


            IntegrationConfiguration config = paymentService.getPaymentConfiguration(order.getPaymentModule(), store);
            IntegrationModule integrationModule = paymentService.getPaymentMethodByCode(store, order.getPaymentModule());


            //OrderTotalSummary orderTotalSummary = orderFacade.calculateOrderTotal(store, order, language);
            OrderTotalSummary orderTotalSummary = super.getSessionAttribute(ORDER_SUMMARY, request);
            if (orderTotalSummary == null) {
                orderTotalSummary = orderFacade.calculateOrderTotal(store, order, language);
                super.setSessionAttribute(ORDER_SUMMARY, orderTotalSummary, request);
            }

            ShippingSummary summary = (ShippingSummary) request.getSession().getAttribute("SHIPPING_SUMMARY");

            if (summary != null) {
                order.setShippingSummary(summary);
            }


            if (action.equals(INIT_ACTION)) {
                if (paymentmethod.equals("PAYPAL")) {
                    try {


                        PaymentModule module = paymentService.getPaymentModule("paypal-express-checkout");
                        PayPalExpressCheckoutPayment p = (PayPalExpressCheckoutPayment) module;
                        PaypalPayment payment = new PaypalPayment();
                        payment.setCurrency(store.getCurrency());
                        Transaction transaction = p.initPaypalTransaction(store, cartItems, orderTotalSummary, payment, config, integrationModule);
                        transactionService.create(transaction);

                        super.setSessionAttribute(INIT_TRANSACTION_KEY, transaction, request);

                        //https://www.paypal.com/cgi-bin/webscr?cmd=_express-checkout-mobile&token=tokenValueReturnedFromSetExpressCheckoutCall
                        //For Desktop use
                        //https://www.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=tokenValueReturnedFromSetExpressCheckoutCall

                        StringBuilder urlAppender = new StringBuilder();

                        if (device != null) {
                            if (device.isNormal()) {
                                urlAppender.append(coreConfiguration.getProperty("PAYPAL_EXPRESSCHECKOUT_REGULAR"));
                            }
                            if (device.isTablet()) {
                                urlAppender.append(coreConfiguration.getProperty("PAYPAL_EXPRESSCHECKOUT_REGULAR"));
                            }
                            if (device.isMobile()) {
                                urlAppender.append(coreConfiguration.getProperty("PAYPAL_EXPRESSCHECKOUT_MOBILE"));
                            }
                        } else {
                            urlAppender.append(coreConfiguration.getProperty("PAYPAL_EXPRESSCHECKOUT_REGULAR"));
                        }

                        urlAppender.append(transaction.getTransactionDetails().get("TOKEN"));


                        if (config.getEnvironment().equals(PRODUCTION_ENVIRONMENT)) {
                            StringBuilder url = new StringBuilder().append(coreConfiguration.getProperty("PAYPAL_EXPRESSCHECKOUT_PRODUCTION")).append(urlAppender.toString());
                            ajaxResponse.addEntry("url", url.toString());
                        } else {
                            StringBuilder url = new StringBuilder().append(coreConfiguration.getProperty("PAYPAL_EXPRESSCHECKOUT_SANDBOX")).append(urlAppender.toString());
                            ajaxResponse.addEntry("url", url.toString());
                        }

                        //keep order in session when user comes back from pp
                        super.setSessionAttribute(ORDER, order, request);
                        ajaxResponse.setStatus(RESPONSE_OPERATION_COMPLETED);

                    } catch (Exception e) {
                        ajaxResponse.setStatus(RESPONSE_STATUS_FAIURE);
                    }


                }
            }

        } catch (Exception e) {
            LOGGER.error("Error while performing payment action " + action + " for payment method " + paymentmethod, e);
            ajaxResponse.setErrorMessage(e);
            ajaxResponse.setStatus(RESPONSE_STATUS_FAIURE);

        }

        return ajaxResponse.toJSONString();
    }

    @RequestMapping(value = {"/paypal/checkout.html/{code}"}, method = RequestMethod.GET)
    public String returnPayPalPayment(@PathVariable String code, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {
        if (SUCCESS.equals(code)) {
            return "redirect:" + SHOP_URI + "/order/commitPreAuthorized.html";
        } else {//process as cancel
            return "redirect:" + SHOP_URI + "/order/checkout.html";
        }
    }

}
