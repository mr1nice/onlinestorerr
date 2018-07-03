package com.salesmanager.core.model.shipping;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.salesmanager.core.model.common.Delivery;
import com.salesmanager.core.model.system.IntegrationModule;

public class ShippingQuote implements Serializable {

    private static final long serialVersionUID = 1L;
	public final static String NO_SHIPPING_TO_SELECTED_COUNTRY = "NO_SHIPPING_TO_SELECTED_COUNTRY";
	public final static String NO_SHIPPING_MODULE_CONFIGURED= "NO_SHIPPING_MODULE_CONFIGURED";
	public final static String NO_POSTAL_CODE= "NO_POSTAL_CODE";
	public final static String ERROR= "ERROR";

	private String shippingModuleCode;
	private List<ShippingOption> shippingOptions = null;
	private String shippingReturnCode = null;//NO_SHIPPING... or NO_SHIPPING_MODULE... or NO_POSTAL_...
    private boolean freeShipping;
	private BigDecimal freeShippingAmount;
    private BigDecimal handlingFees;
	private boolean applyTaxOnShipping;

	private Delivery deliveryAddress;
	
	private List<String> warnings = new ArrayList<String>();
	
	private ShippingOption selectedShippingOption = null;
	
	private IntegrationModule currentShippingModule;
	
	private String quoteError = null;

	private Map<String, Object> quoteInformations = new HashMap<String, Object>();
	
	
	
	public void setShippingOptions(List<ShippingOption> shippingOptions) {
		this.shippingOptions = shippingOptions;
	}
	public List<ShippingOption> getShippingOptions() {
		return shippingOptions;
	}
	public void setShippingModuleCode(String shippingModuleCode) {
		this.shippingModuleCode = shippingModuleCode;
	}
	public String getShippingModuleCode() {
		return shippingModuleCode;
	}
	public void setShippingReturnCode(String shippingReturnCode) {
		this.shippingReturnCode = shippingReturnCode;
	}
	public String getShippingReturnCode() {
		return shippingReturnCode;
	}
	public void setFreeShipping(boolean freeShipping) {
		this.freeShipping = freeShipping;
	}
	public boolean isFreeShipping() {
		return freeShipping;
	}
	public void setFreeShippingAmount(BigDecimal freeShippingAmount) {
		this.freeShippingAmount = freeShippingAmount;
	}
	public BigDecimal getFreeShippingAmount() {
		return freeShippingAmount;
	}
	public void setHandlingFees(BigDecimal handlingFees) {
		this.handlingFees = handlingFees;
	}
	public BigDecimal getHandlingFees() {
		return handlingFees;
	}
	public void setApplyTaxOnShipping(boolean applyTaxOnShipping) {
		this.applyTaxOnShipping = applyTaxOnShipping;
	}
	public boolean isApplyTaxOnShipping() {
		return applyTaxOnShipping;
	}
	public void setSelectedShippingOption(ShippingOption selectedShippingOption) {
		this.selectedShippingOption = selectedShippingOption;
	}
	public ShippingOption getSelectedShippingOption() {
		return selectedShippingOption;
	}
	public String getQuoteError() {
		return quoteError;
	}
	public void setQuoteError(String quoteError) {
		this.quoteError = quoteError;
	}
	public Map<String,Object> getQuoteInformations() {
		return quoteInformations;
	}
	public void setQuoteInformations(Map<String,Object> quoteInformations) {
		this.quoteInformations = quoteInformations;
	}
	public IntegrationModule getCurrentShippingModule() {
		return currentShippingModule;
	}
	public void setCurrentShippingModule(IntegrationModule currentShippingModule) {
		this.currentShippingModule = currentShippingModule;
	}
	public List<String> getWarnings() {
		return warnings;
	}
	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}
	public Delivery getDeliveryAddress() {
		return deliveryAddress;
	}
	public void setDeliveryAddress(Delivery deliveryAddress) {
		this.deliveryAddress = deliveryAddress;
	}

	
	

}
