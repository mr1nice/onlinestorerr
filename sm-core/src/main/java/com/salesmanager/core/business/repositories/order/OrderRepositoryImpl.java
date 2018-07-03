package com.salesmanager.core.business.repositories.order;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;

import com.salesmanager.core.model.common.CriteriaOrderBy;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.OrderCriteria;
import com.salesmanager.core.model.order.OrderList;

import static com.salesmanager.core.model.common.CriteriaOrderBy.ASC;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class OrderRepositoryImpl implements OrderRepositoryCustom {

	
    @PersistenceContext
    private EntityManager em;
    
	@SuppressWarnings("unchecked")
	@Override
	public OrderList listByStore(MerchantStore store, OrderCriteria criteria) {


        OrderList orderList = new OrderList();
        StringBuilder countBuilderSelect = new StringBuilder();
        StringBuilder objectBuilderSelect = new StringBuilder();

        String orderByCriteria = " order by o.id desc";

        if (criteria.getOrderBy() != null) {
            if (ASC.name().equals(criteria.getOrderBy().name())) {
                orderByCriteria = " order by o.id asc";
            }
        }

        String countBaseQuery = "select count(o) from Order as o";
        String baseQuery = "select o from Order as o left join fetch o.orderTotal ot left join fetch o.orderProducts op left join fetch o.orderAttributes oa left join fetch op.orderAttributes opo left join fetch op.prices opp";
        countBuilderSelect.append(countBaseQuery);
        objectBuilderSelect.append(baseQuery);


        StringBuilder countBuilderWhere = new StringBuilder();
        StringBuilder objectBuilderWhere = new StringBuilder();
        String whereQuery = " where o.merchant.id=:mId";
        countBuilderWhere.append(whereQuery);
        objectBuilderWhere.append(whereQuery);


        if (!isBlank(criteria.getCustomerName())) {
            String nameQuery = " and o.billing.firstName like:nm or o.billing.lastName like:nm";
            countBuilderWhere.append(nameQuery);
            objectBuilderWhere.append(nameQuery);
        }

        if (!isBlank(criteria.getPaymentMethod())) {
            String paymentQuery = " and o.paymentModuleCode like:pm";
            countBuilderWhere.append(paymentQuery);
            objectBuilderWhere.append(paymentQuery);
        }

        if (criteria.getCustomerId() != null) {
            String customerQuery = " and o.customerId =:cid";
            countBuilderWhere.append(customerQuery);
            objectBuilderWhere.append(customerQuery);
        }

        objectBuilderWhere.append(orderByCriteria);
        Query countQ = em.createQuery(
                countBuilderSelect.toString() + countBuilderWhere.toString());
        Query objectQ = em.createQuery(
                objectBuilderSelect.toString() + objectBuilderWhere.toString());

        countQ.setParameter("mId", store.getId());
        objectQ.setParameter("mId", store.getId());


        if (!isBlank(criteria.getCustomerName())) {
            String nameParam = new StringBuilder().append("%").append(criteria.getCustomerName()).append("%").toString();
            countQ.setParameter("nm", nameParam);
            objectQ.setParameter("nm", nameParam);
        }

        if (!isBlank(criteria.getPaymentMethod())) {
            String payementParam = new StringBuilder().append("%").append(criteria.getPaymentMethod()).append("%").toString();
            countQ.setParameter("pm", payementParam);
            objectQ.setParameter("pm", payementParam);
        }

        if (criteria.getCustomerId() != null) {
            countQ.setParameter("cid", criteria.getCustomerId());
            objectQ.setParameter("cid", criteria.getCustomerId());
        }


        Number count = (Number) countQ.getSingleResult();

        orderList.setTotalCount(count.intValue());

        if (count.intValue() == 0)
            return orderList;
        int max = criteria.getMaxCount();
        int first = criteria.getStartIndex();

        objectQ.setFirstResult(first);


        if (max > 0) {
            int maxCount = first + max;

            if (maxCount < count.intValue()) {
                objectQ.setMaxResults(maxCount);
            } else {
                objectQ.setMaxResults(count.intValue());
            }
        }

        orderList.setOrders(objectQ.getResultList());

        return orderList;


    }


}
