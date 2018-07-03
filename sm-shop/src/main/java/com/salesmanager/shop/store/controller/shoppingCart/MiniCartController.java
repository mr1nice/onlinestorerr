package com.salesmanager.shop.store.controller.shoppingCart;

import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.shoppingcart.ShoppingCartData;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.store.controller.shoppingCart.facade.ShoppingCartFacade;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import static com.salesmanager.shop.constants.Constants.*;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author Umesh A
 */
@Controller
@RequestMapping("/shop/cart")
public class MiniCartController extends AbstractController {

    private static final Logger LOG = getLogger(MiniCartController.class);

    @Inject
    private ShoppingCartFacade shoppingCartFacade;


    @RequestMapping(value = {"/displayMiniCartByCode"}, method = {GET, POST})
    public @ResponseBody
    ShoppingCartData displayMiniCart(final String shoppingCartCode, HttpServletRequest request, Model model) {

        Language language = (Language) request.getAttribute(LANGUAGE);

        try {
            MerchantStore merchantStore = (MerchantStore) request.getAttribute(MERCHANT_STORE);
            Customer customer = getSessionAttribute(CUSTOMER, request);
            ShoppingCartData cart = shoppingCartFacade.getShoppingCartData(customer, merchantStore, shoppingCartCode, language);
            if (cart != null) {
                request.getSession().setAttribute(SHOPPING_CART, cart.getCode());
            }
            if (cart == null) {
                request.getSession().removeAttribute(SHOPPING_CART);//make sure there is no cart here
                cart = new ShoppingCartData();//create an empty cart
            }
            return cart;


        } catch (Exception e) {
            LOG.error("Error while getting the shopping cart", e);
        }

        return null;

    }


    @RequestMapping(value = {"/removeMiniShoppingCartItem"}, method = {GET, POST})
    public @ResponseBody
    ShoppingCartData removeShoppingCartItem(Long lineItemId, final String shoppingCartCode, HttpServletRequest request, Model model) throws Exception {
        Language language = (Language) request.getAttribute(LANGUAGE);
        MerchantStore merchantStore = (MerchantStore) request.getAttribute(MERCHANT_STORE);
        ShoppingCartData cart = shoppingCartFacade.getShoppingCartData(null, merchantStore, shoppingCartCode, language);
        if (cart == null) {
            return null;
        }
        ShoppingCartData shoppingCartData = shoppingCartFacade.removeCartItem(lineItemId, cart.getCode(), merchantStore, language);


        if (isEmpty(shoppingCartData.getShoppingCartItems())) {
            shoppingCartFacade.deleteShoppingCart(shoppingCartData.getId(), merchantStore);
            request.getSession().removeAttribute(SHOPPING_CART);
            return null;
        }


        request.getSession().setAttribute(SHOPPING_CART, cart.getCode());

        LOG.debug("removed item" + lineItemId + "from cart");
        return shoppingCartData;
    }


}
