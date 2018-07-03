package com.salesmanager.core.business.services.order;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.modules.order.InvoiceModule;
import com.salesmanager.core.business.repositories.order.OrderRepository;
import com.salesmanager.core.business.services.common.generic.SalesManagerEntityServiceImpl;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.order.ordertotal.OrderTotalService;
import com.salesmanager.core.business.services.payments.PaymentService;
import com.salesmanager.core.business.services.payments.TransactionService;
import com.salesmanager.core.business.services.shipping.ShippingService;
import com.salesmanager.core.business.services.tax.TaxService;
import com.salesmanager.core.model.catalog.product.price.FinalPrice;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.*;
import com.salesmanager.core.model.order.orderproduct.OrderProduct;
import com.salesmanager.core.model.order.orderstatus.OrderStatus;
import com.salesmanager.core.model.order.orderstatus.OrderStatusHistory;
import com.salesmanager.core.model.payments.Payment;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.payments.TransactionType;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.shipping.ShippingConfiguration;
import com.salesmanager.core.model.shoppingcart.ShoppingCart;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.core.model.tax.TaxItem;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.salesmanager.core.business.constants.Constants.*;
import static com.salesmanager.core.model.order.OrderSummaryType.ORDERTOTAL;
import static com.salesmanager.core.model.order.OrderSummaryType.SHOPPINGCART;
import static com.salesmanager.core.model.order.OrderTotalType.*;
import static com.salesmanager.core.model.order.OrderValueType.ONE_TIME;
import static com.salesmanager.core.model.order.orderstatus.OrderStatus.ORDERED;
import static com.salesmanager.core.model.payments.TransactionType.*;
import static com.salesmanager.core.model.payments.TransactionType.REFUND;
import static java.math.RoundingMode.HALF_UP;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.slf4j.LoggerFactory.getLogger;


@Service("orderService")
public class OrderServiceImpl extends SalesManagerEntityServiceImpl<Long, Order> implements OrderService {

    private static final Logger LOGGER = getLogger(OrderServiceImpl.class);

    @Inject
    private InvoiceModule invoiceModule;

    @Inject
    private ShippingService shippingService;

    @Inject
    private PaymentService paymentService;

    @Inject
    private TaxService taxService;

    @Inject
    private CustomerService customerService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private OrderTotalService orderTotalService;

    private final OrderRepository orderRepository;

    @Inject
    public OrderServiceImpl(OrderRepository orderRepository) {
        super(orderRepository);
        this.orderRepository = orderRepository;
    }

    @Override
    public void addOrderStatusHistory(Order order, OrderStatusHistory history) throws ServiceException {
        order.getOrderHistory().add(history);
        history.setOrder(order);
        update(order);
    }

    @Override
    public Order processOrder(Order order, Customer customer, List<ShoppingCartItem> items, OrderTotalSummary summary, Payment payment, MerchantStore store) throws ServiceException {

        return this.process(order, customer, items, summary, payment, null, store);
    }

    @Override
    public Order processOrder(Order order, Customer customer, List<ShoppingCartItem> items, OrderTotalSummary summary, Payment payment, Transaction transaction, MerchantStore store) throws ServiceException {

        return this.process(order, customer, items, summary, payment, transaction, store);
    }

    private Order process(Order order, Customer customer, List<ShoppingCartItem> items, OrderTotalSummary summary, Payment payment, Transaction transaction, MerchantStore store) throws ServiceException {


        notNull(order, "Order cannot be null");
        notNull(customer, "Customer cannot be null (even if anonymous order)");
        notEmpty(items, "ShoppingCart items cannot be null");
        notNull(payment, "Payment cannot be null");
        notNull(store, "MerchantStore cannot be null");
        notNull(summary, "Order total Summary cannot be null");
        Transaction processTransaction = paymentService.processPayment(customer, store, payment, items, order);

        if (order.getOrderHistory() == null || order.getOrderHistory().size() == 0 || order.getStatus() == null) {
            OrderStatus status = order.getStatus();
            if (status == null) {
                status = ORDERED;
                order.setStatus(status);
            }
            Set<OrderStatusHistory> statusHistorySet = new HashSet<OrderStatusHistory>();
            OrderStatusHistory statusHistory = new OrderStatusHistory();
            statusHistory.setStatus(status);
            statusHistory.setDateAdded(new Date());
            statusHistory.setOrder(order);
            statusHistorySet.add(statusHistory);
            order.setOrderHistory(statusHistorySet);

        }

        if (customer.getId() == null || customer.getId() == 0) {
            customerService.create(customer);
        }

        order.setCustomerId(customer.getId());

        this.create(order);

        if (transaction != null) {
            transaction.setOrder(order);
            if (transaction.getId() == null || transaction.getId() == 0) {
                transactionService.create(transaction);
            } else {
                transactionService.update(transaction);
            }
        }

        if (processTransaction != null) {
            processTransaction.setOrder(order);
            if (processTransaction.getId() == null || processTransaction.getId() == 0) {
                transactionService.create(processTransaction);
            } else {
                transactionService.update(processTransaction);
            }
        }

        return order;


    }

    private OrderTotalSummary caculateOrder(OrderSummary summary, Customer customer, final MerchantStore store, final Language language) throws Exception {

        OrderTotalSummary totalSummary = new OrderTotalSummary();
        List<OrderTotal> orderTotals = new ArrayList<OrderTotal>();
        Map<String, OrderTotal> otherPricesTotals = new HashMap<String, OrderTotal>();

        ShippingConfiguration shippingConfiguration = null;

        BigDecimal grandTotal = new BigDecimal(0);
        grandTotal.setScale(2, HALF_UP);
        BigDecimal subTotal = new BigDecimal(0);
        subTotal.setScale(2, HALF_UP);
        for (ShoppingCartItem item : summary.getProducts()) {

            BigDecimal st = item.getItemPrice().multiply(new BigDecimal(item.getQuantity()));
            item.setSubTotal(st);
            subTotal = subTotal.add(st);
            FinalPrice finalPrice = item.getFinalPrice();
            if (finalPrice != null) {
                List<FinalPrice> otherPrices = finalPrice.getAdditionalPrices();
                if (otherPrices != null) {
                    for (FinalPrice price : otherPrices) {
                        if (!price.isDefaultPrice()) {
                            OrderTotal itemSubTotal = otherPricesTotals.get(price.getProductPrice().getCode());

                            if (itemSubTotal == null) {
                                itemSubTotal = new OrderTotal();
                                itemSubTotal.setModule(OT_ITEM_PRICE_MODULE_CODE);
                                itemSubTotal.setTitle(OT_ITEM_PRICE_MODULE_CODE);
                                itemSubTotal.setOrderTotalCode(price.getProductPrice().getCode());
                                itemSubTotal.setOrderTotalType(PRODUCT);
                                itemSubTotal.setSortOrder(0);
                                otherPricesTotals.put(price.getProductPrice().getCode(), itemSubTotal);
                            }

                            BigDecimal orderTotalValue = itemSubTotal.getValue();
                            if (orderTotalValue == null) {
                                orderTotalValue = new BigDecimal(0);
                                orderTotalValue.setScale(2, HALF_UP);
                            }

                            orderTotalValue = orderTotalValue.add(price.getFinalPrice());
                            itemSubTotal.setValue(orderTotalValue);
                            if (price.getProductPrice().getProductPriceType().name().equals(ONE_TIME)) {
                                subTotal = subTotal.add(price.getFinalPrice());
                            }
                        }
                    }
                }
            }

        }
        if (ORDERTOTAL.name().equals(summary.getOrderSummaryType().name())) {
            OrderTotalVariation orderTotalVariation = orderTotalService.findOrderTotalVariation(summary, customer, store, language);

            int currentCount = 10;

            if (isNotEmpty(orderTotalVariation.getVariations())) {
                for (OrderTotal variation : orderTotalVariation.getVariations()) {
                    variation.setSortOrder(currentCount++);
                    orderTotals.add(variation);
                    subTotal = subTotal.subtract(variation.getValue());
                }
            }

        }


        totalSummary.setSubTotal(subTotal);
        grandTotal = grandTotal.add(subTotal);

        OrderTotal orderTotalSubTotal = new OrderTotal();
        orderTotalSubTotal.setModule(OT_SUBTOTAL_MODULE_CODE);
        orderTotalSubTotal.setOrderTotalType(SUBTOTAL);
        orderTotalSubTotal.setOrderTotalCode("order.total.subtotal");
        orderTotalSubTotal.setTitle(OT_SUBTOTAL_MODULE_CODE);
        orderTotalSubTotal.setSortOrder(5);
        orderTotalSubTotal.setValue(subTotal);

        orderTotals.add(orderTotalSubTotal);
        if (summary.getShippingSummary() != null) {


            OrderTotal shippingSubTotal = new OrderTotal();
            shippingSubTotal.setModule(OT_SHIPPING_MODULE_CODE);
            shippingSubTotal.setOrderTotalType(SHIPPING);
            shippingSubTotal.setOrderTotalCode("order.total.shipping");
            shippingSubTotal.setTitle(OT_SHIPPING_MODULE_CODE);
            shippingSubTotal.setSortOrder(100);

            orderTotals.add(shippingSubTotal);

            if (!summary.getShippingSummary().isFreeShipping()) {
                shippingSubTotal.setValue(summary.getShippingSummary().getShipping());
                grandTotal = grandTotal.add(summary.getShippingSummary().getShipping());
            } else {
                shippingSubTotal.setValue(new BigDecimal(0));
                grandTotal = grandTotal.add(new BigDecimal(0));
            }
            shippingConfiguration = shippingService.getShippingConfiguration(store);
            if (summary.getShippingSummary().getHandling() != null && summary.getShippingSummary().getHandling().doubleValue() > 0) {
                if (shippingConfiguration.getHandlingFees() != null && shippingConfiguration.getHandlingFees().doubleValue() > 0) {
                    OrderTotal handlingubTotal = new OrderTotal();
                    handlingubTotal.setModule(OT_HANDLING_MODULE_CODE);
                    handlingubTotal.setOrderTotalType(HANDLING);
                    handlingubTotal.setOrderTotalCode("order.total.handling");
                    handlingubTotal.setTitle(OT_HANDLING_MODULE_CODE);
                    handlingubTotal.setSortOrder(120);
                    handlingubTotal.setValue(summary.getShippingSummary().getHandling());
                    orderTotals.add(handlingubTotal);
                    grandTotal = grandTotal.add(summary.getShippingSummary().getHandling());
                }
            }
        }
        List<TaxItem> taxes = taxService.calculateTax(summary, customer, store, language);
        if (taxes != null && taxes.size() > 0) {
            BigDecimal totalTaxes = new BigDecimal(0);
            totalTaxes.setScale(2, HALF_UP);
            int taxCount = 200;
            for (TaxItem tax : taxes) {

                OrderTotal taxLine = new OrderTotal();
                taxLine.setModule(OT_TAX_MODULE_CODE);
                taxLine.setOrderTotalType(TAX);
                taxLine.setOrderTotalCode(tax.getLabel());
                taxLine.setSortOrder(taxCount);
                taxLine.setTitle(OT_TAX_MODULE_CODE);
                taxLine.setText(tax.getLabel());
                taxLine.setValue(tax.getItemPrice());

                totalTaxes = totalTaxes.add(tax.getItemPrice());
                orderTotals.add(taxLine);

                taxCount++;

            }
            grandTotal = grandTotal.add(totalTaxes);
            totalSummary.setTaxTotal(totalTaxes);
        }
        OrderTotal orderTotal = new OrderTotal();
        orderTotal.setModule(OT_TOTAL_MODULE_CODE);
        orderTotal.setOrderTotalType(TOTAL);
        orderTotal.setOrderTotalCode("order.total.total");
        orderTotal.setTitle(OT_TOTAL_MODULE_CODE);
        orderTotal.setSortOrder(500);
        orderTotal.setValue(grandTotal);
        orderTotals.add(orderTotal);

        totalSummary.setTotal(grandTotal);
        totalSummary.setTotals(orderTotals);
        return totalSummary;

    }


    @Override
    public OrderTotalSummary caculateOrderTotal(final OrderSummary orderSummary, final Customer customer, final MerchantStore store, final Language language) throws ServiceException {
        notNull(orderSummary, "Order summary cannot be null");
        notNull(orderSummary.getProducts(), "Order summary.products cannot be null");
        notNull(store, "MerchantStore cannot be null");
        notNull(customer, "Customer cannot be null");

        try {
            return caculateOrder(orderSummary, customer, store, language);
        } catch (Exception e) {
            throw new ServiceException(e);
        }

    }


    @Override
    public OrderTotalSummary caculateOrderTotal(final OrderSummary orderSummary, final MerchantStore store, final Language language) throws ServiceException {
        notNull(orderSummary, "Order summary cannot be null");
        notNull(orderSummary.getProducts(), "Order summary.products cannot be null");
        notNull(store, "MerchantStore cannot be null");

        try {
            return caculateOrder(orderSummary, null, store, language);
        } catch (Exception e) {
            throw new ServiceException(e);
        }

    }

    private OrderTotalSummary caculateShoppingCart(final ShoppingCart shoppingCart, final Customer customer, final MerchantStore store, final Language language) throws Exception {


        OrderSummary orderSummary = new OrderSummary();
        orderSummary.setOrderSummaryType(SHOPPINGCART);

        List<ShoppingCartItem> itemsSet = new ArrayList<ShoppingCartItem>(shoppingCart.getLineItems());
        orderSummary.setProducts(itemsSet);


        return this.caculateOrder(orderSummary, customer, store, language);

    }


    @Override
    public OrderTotalSummary calculateShoppingCartTotal(
            final ShoppingCart shoppingCart, final Customer customer, final MerchantStore store,
            final Language language) throws ServiceException {
        notNull(shoppingCart, "Order summary cannot be null");
        notNull(customer, "Customery cannot be null");
        notNull(store, "MerchantStore cannot be null.");
        try {
            return caculateShoppingCart(shoppingCart, customer, store, language);
        } catch (Exception e) {
            LOGGER.error("Error while calculating shopping cart total" + e);
            throw new ServiceException(e);
        }

    }


    @Override
    public OrderTotalSummary calculateShoppingCartTotal(
            final ShoppingCart shoppingCart, final MerchantStore store, final Language language)
            throws ServiceException {
        notNull(shoppingCart, "Order summary cannot be null");
        notNull(store, "MerchantStore cannot be null");

        try {
            return caculateShoppingCart(shoppingCart, null, store, language);
        } catch (Exception e) {
            LOGGER.error("Error while calculating shopping cart total" + e);
            throw new ServiceException(e);
        }
    }

    @Override
    public void delete(final Order order) throws ServiceException {


        super.delete(order);
    }


    @Override
    public ByteArrayOutputStream generateInvoice(final MerchantStore store, final Order order, final Language language) throws ServiceException {

        notNull(order.getOrderProducts(), "Order products cannot be null");
        notNull(order.getOrderTotal(), "Order totals cannot be null");

        try {
            ByteArrayOutputStream stream = invoiceModule.createInvoice(store, order, language);
            return stream;
        } catch (Exception e) {
            throw new ServiceException(e);
        }


    }

    @Override
    public Order getOrder(final Long orderId) {
        return getById(orderId);
    }

    @Override
    public OrderList listByStore(final MerchantStore store, final OrderCriteria criteria) {

        return orderRepository.listByStore(store, criteria);
    }


    @Override
    public void saveOrUpdate(final Order order) throws ServiceException {

        if (order.getId() != null && order.getId() > 0) {
            LOGGER.debug("Updating Order");
            super.update(order);

        } else {
            LOGGER.debug("Creating Order");
            super.create(order);

        }
    }

    @Override
    public boolean hasDownloadFiles(Order order) throws ServiceException {

        notNull(order, "Order cannot be null");
        notNull(order.getOrderProducts(), "Order products cannot be null");
        notEmpty(order.getOrderProducts(), "Order products cannot be empty");

        boolean hasDownloads = false;
        for (OrderProduct orderProduct : order.getOrderProducts()) {

            if (isNotEmpty(orderProduct.getDownloads())) {
                hasDownloads = true;
                break;
            }
        }

        return hasDownloads;
    }

    @Override
    public List<Order> getCapturableOrders(MerchantStore store, Date startDate, Date endDate) throws ServiceException {

        List<Transaction> transactions = transactionService.listTransactions(startDate, endDate);

        List<Order> returnOrders = null;

        if (!isEmpty(transactions)) {

            returnOrders = new ArrayList<Order>();
            Map<Long, Order> preAuthOrders = new HashMap<Long, Order>();
            Map<Long, List<Transaction>> processingTransactions = new HashMap<Long, List<Transaction>>();

            for (Transaction trx : transactions) {
                Order order = trx.getOrder();
                if (AUTHORIZE.name().equals(trx.getTransactionType().name())) {
                    preAuthOrders.put(order.getId(), order);
                }

                //put transaction
                List<Transaction> listTransactions = null;
                if (processingTransactions.containsKey(order.getId())) {
                    listTransactions = processingTransactions.get(order.getId());
                } else {
                    listTransactions = new ArrayList<Transaction>();
                    processingTransactions.put(order.getId(), listTransactions);
                }
                listTransactions.add(trx);
            }

            for (Long orderId : processingTransactions.keySet()) {

                List<Transaction> trx = processingTransactions.get(orderId);
                if (isNotEmpty(trx)) {

                    boolean capturable = true;
                    for (Transaction t : trx) {

                        if (CAPTURE.name().equals(t.getTransactionType().name())) {
                            capturable = false;
                        } else if (AUTHORIZECAPTURE.name().equals(t.getTransactionType().name())) {
                            capturable = false;
                        } else if (REFUND.name().equals(t.getTransactionType().name())) {
                            capturable = false;
                        }

                    }

                    if (capturable) {
                        Order o = preAuthOrders.get(orderId);
                        returnOrders.add(o);
                    }

                }


            }
        }

        return returnOrders;
    }


}
