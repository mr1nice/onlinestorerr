package com.salesmanager.core.model.customer.review;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.salesmanager.core.constants.SchemaConstant;
import com.salesmanager.core.model.common.description.Description;
import com.salesmanager.core.model.reference.language.Language;

import static com.salesmanager.core.constants.SchemaConstant.SALESMANAGER_SCHEMA;

@Entity
@Table(name = "CUSTOMER_REVIEW_DESCRIPTION", schema = SALESMANAGER_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "CUSTOMER_REVIEW_ID",
                "LANGUAGE_ID"
        })
})
public class CustomerReviewDescription extends Description {
    private static final long serialVersionUID = -1L;

    @ManyToOne(targetEntity = CustomerReview.class)
    @JoinColumn(name = "CUSTOMER_REVIEW_ID")
    private CustomerReview customerReview;

    public CustomerReview getCustomerReview() {
        return customerReview;
    }

    public void setCustomerReview(CustomerReview customerReview) {
        this.customerReview = customerReview;
    }

    public CustomerReviewDescription() {
    }

    public CustomerReviewDescription(Language language, String name) {
        this.setLanguage(language);
        this.setName(name);
    }


}
