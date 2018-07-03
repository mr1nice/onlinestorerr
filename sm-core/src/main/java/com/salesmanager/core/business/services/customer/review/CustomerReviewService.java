package com.salesmanager.core.business.services.customer.review;

import java.util.List;

import com.salesmanager.core.business.services.common.generic.SalesManagerEntityService;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.customer.review.CustomerReview;

public interface CustomerReviewService extends
	SalesManagerEntityService<Long, CustomerReview> {

    List<CustomerReview> getByCustomer(Customer customer);

    List<CustomerReview> getByReviewedCustomer(Customer customer);

    CustomerReview getByReviewerAndReviewed(Long reviewer, Long reviewed);

}
