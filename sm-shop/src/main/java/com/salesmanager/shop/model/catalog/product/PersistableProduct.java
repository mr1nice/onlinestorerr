package com.salesmanager.shop.model.catalog.product;

import java.io.Serializable;
import java.util.List;

import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.manufacturer.Manufacturer;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductAttribute;



public class PersistableProduct extends ProductEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<ProductDescription> descriptions;
    private List<PersistableProductAttribute> attributes;
    private List<PersistableImage> images;
    private List<PersistableProductPrice> productPrices;
    private List<Category> categories;
    private List<RelatedProduct> relatedProducts;
    private Manufacturer manufacturer;

    private RentalOwner owner;
	
	public List<ProductDescription> getDescriptions() {
		return descriptions;
	}
	public void setDescriptions(List<ProductDescription> descriptions) {
		this.descriptions = descriptions;
	}

	public List<PersistableImage> getImages() {
		return images;
	}
	public void setImages(List<PersistableImage> images) {
		this.images = images;
	}
	public List<Category> getCategories() {
		return categories;
	}
	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}
	public List<RelatedProduct> getRelatedProducts() {
		return relatedProducts;
	}
	public void setRelatedProducts(List<RelatedProduct> relatedProducts) {
		this.relatedProducts = relatedProducts;
	}
	public void setManufacturer(Manufacturer manufacturer) {
		this.manufacturer = manufacturer;
	}
	public Manufacturer getManufacturer() {
		return manufacturer;
	}
	public void setAttributes(List<PersistableProductAttribute> attributes) {
		this.attributes = attributes;
	}
	public List<PersistableProductAttribute> getAttributes() {
		return attributes;
	}
	public List<PersistableProductPrice> getProductPrices() {
		return productPrices;
	}
	public void setProductPrices(List<PersistableProductPrice> productPrices) {
		this.productPrices = productPrices;
	}
	public RentalOwner getOwner() {
		return owner;
	}
	public void setOwner(RentalOwner owner) {
		this.owner = owner;
	}

}
