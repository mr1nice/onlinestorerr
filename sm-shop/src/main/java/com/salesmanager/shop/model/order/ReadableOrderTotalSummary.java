package com.salesmanager.shop.model.order;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.salesmanager.shop.model.order.total.ReadableOrderTotal;

public class ReadableOrderTotalSummary implements Serializable {

    private static final long serialVersionUID = 1L;
    private String subTotal;
    private String total;
    private String taxTotal;

    private List<ReadableOrderTotal> totals = new ArrayList<ReadableOrderTotal>();

	public String getSubTotal() {
		return subTotal;
	}

	public void setSubTotal(String subTotal) {
		this.subTotal = subTotal;
	}

	public String getTotal() {
		return total;
	}

	public void setTotal(String total) {
		this.total = total;
	}

	public String getTaxTotal() {
		return taxTotal;
	}

	public void setTaxTotal(String taxTotal) {
		this.taxTotal = taxTotal;
	}

	public List<ReadableOrderTotal> getTotals() {
		return totals;
	}

	public void setTotals(List<ReadableOrderTotal> totals) {
		this.totals = totals;
	}

}