package com.salesmanager.shop.store.controller.order.facade;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.validation.BindingResult;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.OrderTotalSummary;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.shipping.ShippingQuote;
import com.salesmanager.core.model.shipping.ShippingSummary;
import com.salesmanager.core.model.shoppingcart.ShoppingCart;
import com.salesmanager.shop.model.customer.PersistableCustomer;
import com.salesmanager.shop.model.order.PersistableOrder;
import com.salesmanager.shop.model.order.PersistableOrderApi;
import com.salesmanager.shop.model.order.ReadableOrder;
import com.salesmanager.shop.model.order.ReadableOrderList;
import com.salesmanager.shop.model.order.ShopOrder;
import com.salesmanager.shop.model.order.transaction.ReadableTransaction;


public interface OrderFacade {
	
	ShopOrder initializeOrder(MerchantStore store, Customer customer, ShoppingCart shoppingCart, Language language) throws Exception;
	void refreshOrder(ShopOrder order, MerchantStore store, Customer customer, ShoppingCart shoppingCart, Language language) throws Exception;

    OrderTotalSummary calculateOrderTotal(MerchantStore store, ShopOrder order, Language language) throws Exception;

    OrderTotalSummary calculateOrderTotal(MerchantStore store, PersistableOrder order, Language language) throws Exception;

    Order processOrder(ShopOrder order, Customer customer, MerchantStore store, Language language) throws ServiceException;

    Order processOrder(ShopOrder order, Customer customer, Transaction transaction, MerchantStore store, Language language) throws ServiceException;

    Order processOrder(PersistableOrderApi order, Customer customer, MerchantStore store, Language language, Locale locale) throws ServiceException;


    Customer initEmptyCustomer(MerchantStore store);
	List<Country> getShipToCountry(MerchantStore store, Language language)
			throws Exception;

    ShippingQuote getShippingQuote(PersistableCustomer customer, ShoppingCart cart, ShopOrder order,
                                   MerchantStore store, Language language) throws Exception;
	
	ShippingQuote getShippingQuote(Customer customer, ShoppingCart cart, PersistableOrder order,
			MerchantStore store, Language language) throws Exception;
	
	ShippingQuote getShippingQuote(Customer customer, ShoppingCart cart,
			MerchantStore store, Language language) throws Exception;

    ShippingSummary getShippingSummary(ShippingQuote quote, MerchantStore store, Language language);

    void validateOrder(ShopOrder order, BindingResult bindingResult,
                       Map<String, String> messagesResult, MerchantStore store,
                       Locale locale) throws ServiceException;

    ReadableOrder getReadableOrder(Long orderId, MerchantStore store, Language language) throws Exception;


    ReadableOrderList getReadableOrderList(MerchantStore store, Customer customer, int start,
                                           int maxCount, Language language) throws Exception;


    ReadableOrderList getCapturableOrderList(MerchantStore store, Date startDate, Date endDate,
                                             Language language) throws Exception;

    ReadableTransaction captureOrder(MerchantStore store, Order order, Customer customer, Language language) throws Exception;


    ReadableOrderList getReadableOrderList(MerchantStore store, int start,
                                           int maxCount, Language language) throws Exception;
}
