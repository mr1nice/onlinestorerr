package com.salesmanager.shop.store.controller.shoppingCart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.salesmanager.core.model.shoppingcart.ShoppingCart;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductAttributeService;
import com.salesmanager.core.business.services.order.OrderService;
import com.salesmanager.core.business.services.shoppingcart.ShoppingCartService;
import com.salesmanager.core.business.utils.ProductPriceUtils;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.shop.PageInformation;
import com.salesmanager.shop.model.shoppingcart.ShoppingCartData;
import com.salesmanager.shop.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.store.controller.ControllerConstants;
import com.salesmanager.shop.store.controller.shoppingCart.facade.ShoppingCartFacade;
import com.salesmanager.shop.utils.LabelUtils;
import com.salesmanager.shop.utils.LanguageUtils;

import static com.salesmanager.core.business.utils.ajax.AjaxResponse.RESPONSE_STATUS_FAIURE;
import static com.salesmanager.core.business.utils.ajax.AjaxResponse.RESPONSE_STATUS_SUCCESS;
import static com.salesmanager.shop.constants.Constants.*;
import static com.salesmanager.shop.store.controller.ControllerConstants.Tiles;
import static com.salesmanager.shop.store.controller.ControllerConstants.Tiles.ShoppingCart;
import static com.salesmanager.shop.store.controller.ControllerConstants.Tiles.ShoppingCart.shoppingCart;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@RequestMapping("/shop/cart/")
public class ShoppingCartController extends AbstractController {

    private static final Logger LOG = getLogger(ShoppingCartController.class);
    @Inject
    private ProductService productService;

    @Inject
    private ProductAttributeService productAttributeService;

    @Inject
    private PricingService pricingService;

    @Inject
    private OrderService orderService;

    @Inject
    private ShoppingCartService shoppingCartService;

    @Inject
    private ProductPriceUtils productPriceUtils;

    @Inject
    private ShoppingCartFacade shoppingCartFacade;

    @Inject
    private LabelUtils messages;

    @Inject
    private LanguageUtils languageUtils;


    /**
     * Add an item to the ShoppingCart (AJAX exposed method)
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/addShoppingCartItem"}, method = POST)
    public @ResponseBody
    ShoppingCartData addShoppingCartItem(@RequestBody final ShoppingCartItem item, final HttpServletRequest request, final HttpServletResponse response, final Locale locale) throws Exception {


        ShoppingCartData shoppingCart = null;


        //Look in the HttpSession to see if a customer is logged in
        MerchantStore store = getSessionAttribute(MERCHANT_STORE, request);
        Language language = (Language) request.getAttribute(LANGUAGE);
        Customer customer = getSessionAttribute(CUSTOMER, request);


        if (customer != null) {
            com.salesmanager.core.model.shoppingcart.ShoppingCart customerCart = shoppingCartService.getByCustomer(customer);
            if (customerCart != null) {
                shoppingCart = shoppingCartFacade.getShoppingCartData(customerCart, language);


                //TODO if shoppingCart != null ?? merge
                //TODO maybe they have the same code
                //TODO what if codes are different (-- merge carts, keep the latest one, delete the oldest, switch codes --)
            }
        }


        if (shoppingCart == null && !isBlank(item.getCode())) {
            shoppingCart = shoppingCartFacade.getShoppingCartData(item.getCode(), store, language);
        }


        //if shoppingCart is null create a new one
        if (shoppingCart == null) {
            shoppingCart = new ShoppingCartData();
            String code = randomUUID().toString().replaceAll("-", "");
            shoppingCart.setCode(code);
        }

        shoppingCart = shoppingCartFacade.addItemsToShoppingCart(shoppingCart, item, store, language, customer);
        request.getSession().setAttribute(SHOPPING_CART, shoppingCart.getCode());


        /******************************************************/
        //TODO validate all of this

        //if a customer exists in http session
        //if a cart does not exist in httpsession
        //get cart from database
        //if a cart exist in the database add the item to the cart and put cart in httpsession and save to the database
        //else a cart does not exist in the database, create a new one, set the customer id, set the cart in the httpsession
        //else a cart exist in the httpsession, add item to httpsession cart and save to the database
        //else no customer in httpsession
        //if a cart does not exist in httpsession
        //create a new one, set the cart in the httpsession
        //else a cart exist in the httpsession, add item to httpsession cart and save to the database


        /**
         *  Tested with the following :
         * 	what if you add item in the shopping cart as an anonymous user
         *  later on you log in to process with checkout but the system retrieves a previous shopping cart saved in the database for that customer
         *  in that case we need to synchronize both carts and the original one (the one with the customer id) supercedes the current cart in session
         *  the system will have to deal with the original one and remove the latest
         */


        //**more implementation details
        //calculate the price of each item by using ProductPriceUtils in sm-core
        //for each product in the shopping cart get the product
        //invoke productPriceUtils.getFinalProductPrice
        //from FinalPrice get final price which is the calculated price given attributes and discounts
        //set each item price in ShoppingCartItem.price


        return shoppingCart;

    }


    /**
     * Retrieves a Shopping cart from the database (regular shopping cart)
     *
     * @param model
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/shoppingCart.html"}, method = GET)
    public String displayShoppingCart(final Model model, final HttpServletRequest request, final HttpServletResponse response, final Locale locale)
            throws Exception {

        LOG.debug("Starting to calculate shopping cart...");

        Language language = (Language) request.getAttribute(LANGUAGE);


        //meta information
        PageInformation pageInformation = new PageInformation();
        pageInformation.setPageTitle(messages.getMessage("label.cart.placeorder", locale));
        request.setAttribute(REQUEST_PAGE_INFORMATION, pageInformation);


        MerchantStore store = (MerchantStore) request.getAttribute(MERCHANT_STORE);
        Customer customer = getSessionAttribute(CUSTOMER, request);

        /** there must be a cart in the session **/
        String cartCode = (String) request.getSession().getAttribute(SHOPPING_CART);

        if (isBlank(cartCode)) {
            //display empty cart
            StringBuilder template =
                    new StringBuilder().append(shoppingCart).append(".").append(store.getStoreTemplate());
            return template.toString();
        }

        ShoppingCartData shoppingCart = shoppingCartFacade.getShoppingCartData(customer, store, cartCode, language);

        if (shoppingCart == null) {
            //display empty cart
            StringBuilder template =
                    new StringBuilder().append(shoppingCart).append(".").append(store.getStoreTemplate());
            return template.toString();
        }

        Language lang = languageUtils.getRequestLanguage(request, response);
        //Filter unavailables
        List<ShoppingCartItem> unavailables = new ArrayList<ShoppingCartItem>();
        List<ShoppingCartItem> availables = new ArrayList<ShoppingCartItem>();
        //Take out items no more available
        List<ShoppingCartItem> items = shoppingCart.getShoppingCartItems();
        for (ShoppingCartItem item : items) {
            String code = item.getProductCode();
            Product p = productService.getByCode(code, lang);
            if (!p.isAvailable()) {
                unavailables.add(item);
            } else {
                availables.add(item);
            }

        }
        shoppingCart.setShoppingCartItems(availables);
        shoppingCart.setUnavailables(unavailables);


        model.addAttribute("cart", shoppingCart);


        /** template **/
        StringBuilder template =
                new StringBuilder().append(shoppingCart).append(".").append(store.getStoreTemplate());
        return template.toString();

    }


    @RequestMapping(value = {"/shoppingCartByCode"}, method = {GET})
    public String displayShoppingCart(@ModelAttribute String shoppingCartCode, final Model model, HttpServletRequest request, HttpServletResponse response, final Locale locale) throws Exception {

        MerchantStore merchantStore = (MerchantStore) request.getAttribute(MERCHANT_STORE);
        Customer customer = getSessionAttribute(CUSTOMER, request);

        Language language = (Language) request.getAttribute(LANGUAGE);

        if (isBlank(shoppingCartCode)) {
            return "redirect:/shop";
        }

        ShoppingCartData cart = shoppingCartFacade.getShoppingCartData(customer, merchantStore, shoppingCartCode, language);
        if (cart == null) {
            return "redirect:/shop";
        }


        Language lang = languageUtils.getRequestLanguage(request, response);
        //Filter unavailables
        List<ShoppingCartItem> unavailables = new ArrayList<ShoppingCartItem>();
        List<ShoppingCartItem> availables = new ArrayList<ShoppingCartItem>();
        //Take out items no more available
        List<ShoppingCartItem> items = cart.getShoppingCartItems();
        for (ShoppingCartItem item : items) {
            String code = item.getProductCode();
            Product p = productService.getByCode(code, lang);
            if (!p.isAvailable()) {
                unavailables.add(item);
            } else {
                availables.add(item);
            }

        }
        cart.setShoppingCartItems(availables);
        cart.setUnavailables(unavailables);


        //meta information
        PageInformation pageInformation = new PageInformation();
        pageInformation.setPageTitle(messages.getMessage("label.cart.placeorder", locale));
        request.setAttribute(REQUEST_PAGE_INFORMATION, pageInformation);
        request.getSession().setAttribute(SHOPPING_CART, cart.getCode());
        model.addAttribute("cart", cart);

        /** template **/
        StringBuilder template =
                new StringBuilder().append(shoppingCart).append(".").append(merchantStore.getStoreTemplate());
        return template.toString();


    }


    /**
     * Removes an item from the Shopping Cart (AJAX exposed method)
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/removeShoppingCartItem.html"}, method = {GET, POST})
    String removeShoppingCartItem(final Long lineItemId, final HttpServletRequest request, final HttpServletResponse response) throws Exception {


        //Looks in the HttpSession to see if a customer is logged in

        //get any shopping cart for this user

        //** need to check if the item has property, similar items may exist but with different properties
        //String attributes = request.getParameter("attribute");//attributes id are sent as 1|2|5|
        //this will help with hte removal of the appropriate item

        //remove the item shoppingCartService.create

        //create JSON representation of the shopping cart

        //return the JSON structure in AjaxResponse

        //store the shopping cart in the http session

        MerchantStore store = getSessionAttribute(MERCHANT_STORE, request);
        Language language = (Language) request.getAttribute(LANGUAGE);
        Customer customer = getSessionAttribute(CUSTOMER, request);

        /** there must be a cart in the session **/
        String cartCode = (String) request.getSession().getAttribute(SHOPPING_CART);

        if (isBlank(cartCode)) {
            return "redirect:/shop";
        }

        ShoppingCartData shoppingCart = shoppingCartFacade.getShoppingCartData(customer, store, cartCode, language);

        ShoppingCartData shoppingCartData = shoppingCartFacade.removeCartItem(lineItemId, shoppingCart.getCode(), store, language);


        if (isEmpty(shoppingCartData.getShoppingCartItems())) {
            shoppingCartFacade.deleteShoppingCart(shoppingCartData.getId(), store);
            return "redirect:/shop";
        }


        return REDIRECT_PREFIX + "/shop/cart/shoppingCart.html";


    }

    /**
     * Update the quantity of an item in the Shopping Cart (AJAX exposed method)
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping(value = {"/updateShoppingCartItem.html"}, method = {POST})
    public @ResponseBody
    String updateShoppingCartItem(@RequestBody final ShoppingCartItem[] shoppingCartItems, final HttpServletRequest request, final HttpServletResponse response) {

        AjaxResponse ajaxResponse = new AjaxResponse();


        MerchantStore store = getSessionAttribute(MERCHANT_STORE, request);
        Language language = (Language) request.getAttribute(LANGUAGE);


        String cartCode = (String) request.getSession().getAttribute(SHOPPING_CART);

        if (isBlank(cartCode)) {
            return "redirect:/shop";
        }

        try {
            List<ShoppingCartItem> items = asList(shoppingCartItems);
            ShoppingCartData shoppingCart = shoppingCartFacade.updateCartItems(items, store, language);
            ajaxResponse.setStatus(RESPONSE_STATUS_SUCCESS);

        } catch (Exception e) {
            LOG.error("Excption while updating cart", e);
            ajaxResponse.setStatus(RESPONSE_STATUS_FAIURE);
        }

        return ajaxResponse.toJSONString();


    }


}
