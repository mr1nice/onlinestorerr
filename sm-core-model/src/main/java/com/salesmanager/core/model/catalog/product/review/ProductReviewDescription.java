package com.salesmanager.core.model.catalog.product.review;

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
@Table(name = "PRODUCT_REVIEW_DESCRIPTION", schema = SALESMANAGER_SCHEMA, uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "PRODUCT_REVIEW_ID",
                "LANGUAGE_ID"
        })
})
public class ProductReviewDescription extends Description {
    private static final long serialVersionUID = -1957502640742695406L;

    @ManyToOne(targetEntity = ProductReview.class)
    @JoinColumn(name = "PRODUCT_REVIEW_ID")
    private ProductReview productReview;

    public ProductReviewDescription() {
    }

    public ProductReviewDescription(Language language, String name) {
        this.setLanguage(language);
        this.setName(name);
    }

    public ProductReview getProductReview() {
        return productReview;
    }

    public void setProductReview(ProductReview productReview) {
        this.productReview = productReview;
    }
}
