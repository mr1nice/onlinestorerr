package com.salesmanager.test.order;

import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.salesmanager.test.common.AbstractSalesManagerCoreTestCase;
import org.apache.commons.lang3.StringUtils;
import org.jopendocument.dom.OOUtils;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.model.OpenDocument;
import org.jopendocument.renderer.ODTRenderer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.utils.ProductPriceUtils;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.attribute.ProductAttribute;
import com.salesmanager.core.model.catalog.product.attribute.ProductOption;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionDescription;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValue;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValueDescription;
import com.salesmanager.core.model.catalog.product.availability.ProductAvailability;
import com.salesmanager.core.model.catalog.product.description.ProductDescription;
import com.salesmanager.core.model.catalog.product.price.ProductPrice;
import com.salesmanager.core.model.catalog.product.price.ProductPriceDescription;
import com.salesmanager.core.model.catalog.product.type.ProductType;
import com.salesmanager.core.model.common.Billing;
import com.salesmanager.core.model.common.Delivery;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.customer.CustomerGender;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.OrderTotal;
import com.salesmanager.core.model.order.orderproduct.OrderProduct;
import com.salesmanager.core.model.order.orderproduct.OrderProductAttribute;
import com.salesmanager.core.model.order.orderproduct.OrderProductDownload;
import com.salesmanager.core.model.order.orderproduct.OrderProductPrice;
import com.salesmanager.core.model.order.orderstatus.OrderStatus;
import com.salesmanager.core.model.order.orderstatus.OrderStatusHistory;
import com.salesmanager.core.model.payments.PaymentType;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.currency.Currency;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;

import static com.itextpdf.text.PageSize.A4;
import static com.itextpdf.text.pdf.PdfWriter.getInstance;
import static com.salesmanager.core.business.constants.Constants.DEFAULT_DATE_FORMAT;
import static com.salesmanager.core.model.catalog.product.type.ProductType.GENERAL_TYPE;
import static com.salesmanager.core.model.customer.CustomerGender.M;
import static com.salesmanager.core.model.merchant.MerchantStore.DEFAULT_STORE;
import static com.salesmanager.core.model.order.orderstatus.OrderStatus.DELIVERED;
import static com.salesmanager.core.model.order.orderstatus.OrderStatus.ORDERED;
import static com.salesmanager.core.model.payments.PaymentType.PAYPAL;
import static java.util.Locale.CANADA;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.jopendocument.dom.OOUtils.open;
import static org.jopendocument.dom.spreadsheet.SpreadSheet.createFromFile;
import static org.junit.Assert.assertTrue;


@Ignore
public class InvoiceTest extends AbstractSalesManagerCoreTestCase {

    @Inject
    ProductPriceUtils priceUtil;

    //@Test
    @Ignore
    public void createInvoice() throws ServiceException {


        MerchantStore store = merchantService.getByCode(DEFAULT_STORE);

        //create a product
        ProductType generalType = productTypeService.getProductType(GENERAL_TYPE);

        Language en = languageService.getByCode("en");


        /**
         * 1) Create an order
         *
         */

        //1.1 create a product

        //create an option
        ProductOption color = new ProductOption();
        color.setMerchantStore(store);
        color.setCode("color");
        color.setProductOptionType("SELECT");

        ProductOptionDescription colorDescription = new ProductOptionDescription();
        colorDescription.setDescription("Color");
        colorDescription.setName("Color");
        colorDescription.setLanguage(en);
        colorDescription.setProductOption(color);

        Set<ProductOptionDescription> colorDescriptions = new HashSet<ProductOptionDescription>();
        colorDescriptions.add(colorDescription);

        color.setDescriptions(colorDescriptions);

        productOptionService.create(color);

        //create an option value
        ProductOptionValue red = new ProductOptionValue();
        red.setMerchantStore(store);
        red.setCode("red");

        ProductOptionValueDescription redDescription = new ProductOptionValueDescription();
        redDescription.setDescription("Red");
        redDescription.setLanguage(en);
        redDescription.setName("Red");
        redDescription.setProductOptionValue(red);

        Set<ProductOptionValueDescription> redDescriptions = new HashSet<ProductOptionValueDescription>();
        redDescriptions.add(redDescription);

        red.setDescriptions(redDescriptions);

        productOptionValueService.create(red);

        //create a product
        Product product = new Product();
        product.setProductHeight(new BigDecimal(4));
        product.setProductLength(new BigDecimal(3));
        product.setProductWidth(new BigDecimal(5));
        product.setProductWeight(new BigDecimal(8));
        product.setSku("TESTSKU");
        product.setType(generalType);
        product.setMerchantStore(store);

        // Product description
        ProductDescription description = new ProductDescription();
        description.setName("Product 1");
        description.setLanguage(en);
        description.setProduct(product);

        product.getDescriptions().add(description);


        // Availability
        ProductAvailability availability = new ProductAvailability();
        availability.setProductDateAvailable(new Date());
        availability.setProductQuantity(100);
        availability.setRegion("*");
        availability.setProduct(product);// associate with product

        product.getAvailabilities().add(availability);

        //price
        ProductPrice dprice = new ProductPrice();
        dprice.setDefaultPrice(true);
        dprice.setProductPriceAmount(new BigDecimal(29.99));
        dprice.setProductAvailability(availability);

        ProductPriceDescription dpd = new ProductPriceDescription();
        dpd.setName("Base price");
        dpd.setProductPrice(dprice);
        dpd.setLanguage(en);

        dprice.getDescriptions().add(dpd);


        //create an attribute
        ProductAttribute colorAttribute = new ProductAttribute();
        colorAttribute.setProduct(product);
        colorAttribute.setProductAttributePrice(new BigDecimal(5));
        colorAttribute.setProductOption(color);
        colorAttribute.setProductOptionValue(red);


        product.getAttributes().add(colorAttribute);

        productService.create(product);


        //1.2 create a Customer
        Country country = countryService.getByCode("CA");
        Zone zone = zoneService.getByCode("QC");

        Customer customer = new Customer();
        customer.setMerchantStore(store);
        customer.setEmailAddress("test@test.com");
        customer.setGender(M);
        customer.setAnonymous(true);
        customer.setCompany("ifactory");
        customer.setDateOfBirth(new Date());
        customer.setNick("My nick");
        customer.setDefaultLanguage(en);


        Delivery delivery = new Delivery();
        delivery.setAddress("358 Du Languadoc");
        delivery.setCity("Boucherville");
        delivery.setCountry(country);
//	    delivery.setCountryCode(CA_COUNTRY_CODE);
        delivery.setFirstName("First");
        delivery.setLastName("Last");
        delivery.setPostalCode("J4B-8J9");
        delivery.setZone(zone);

        Billing billing = new Billing();
        billing.setAddress("358 Du Languadoc");
        billing.setCity("Boucherville");
        billing.setCompany("CSTI Consulting");
        billing.setCountry(country);
//	    billing.setCountryCode(CA_COUNTRY_CODE);
        billing.setFirstName("Carl");
        billing.setLastName("Samson");
        billing.setPostalCode("J4B-8J9");
        billing.setZone(zone);

        customer.setBilling(billing);
        customer.setDelivery(delivery);
        customerService.create(customer);

        Currency currency = currencyService.getByCode(CAD_CURRENCY_CODE);

        //1.3 create an order
        OrderStatusHistory orderStatusHistory = new OrderStatusHistory();


        Order order = new Order();
        order.setDatePurchased(new Date());
        order.setCurrency(currency);
        order.setLastModified(new Date());
        order.setBilling(billing);

        Locale l = CANADA;
        order.setLocale(l);


        order.setCurrencyValue(new BigDecimal(0.98));//compared to based currency (not necessary)
        order.setCustomerId(customer.getId());
        order.setDelivery(delivery);
        order.setIpAddress("ipAddress");
        order.setMerchant(store);
        order.setCustomerEmailAddress(customer.getEmailAddress());
        order.setOrderDateFinished(new Date());//committed date

        orderStatusHistory.setComments("We received your order");
        orderStatusHistory.setCustomerNotified(1);
        orderStatusHistory.setStatus(ORDERED);
        orderStatusHistory.setDateAdded(new Date());
        orderStatusHistory.setOrder(order);
        order.getOrderHistory().add(orderStatusHistory);


        order.setPaymentType(PAYPAL);
        order.setPaymentModuleCode("paypal");
        order.setStatus(DELIVERED);
        order.setTotal(new BigDecimal(23.99));


        //OrderProductDownload - Digital download
        OrderProductDownload orderProductDownload = new OrderProductDownload();
        orderProductDownload.setDownloadCount(1);
        orderProductDownload.setMaxdays(31);
        orderProductDownload.setOrderProductFilename("Your digital file name");

        //OrderProductPrice
        OrderProductPrice oproductprice = new OrderProductPrice();
        oproductprice.setDefaultPrice(true);
        oproductprice.setProductPrice(new BigDecimal(19.99));
        oproductprice.setProductPriceCode("baseprice");
        oproductprice.setProductPriceName("Base Price");

        //OrderProduct
        OrderProduct oproduct = new OrderProduct();
        oproduct.getDownloads().add(orderProductDownload);
        oproduct.setOneTimeCharge(new BigDecimal(19.99));
        oproduct.setOrder(order);
        oproduct.setProductName("Product name");
        oproduct.setProductQuantity(2);
        oproduct.setSku("TB12345");
        oproduct.getPrices().add(oproductprice);


        //an attribute to the OrderProduct
        OrderProductAttribute orderAttribute = new OrderProductAttribute();
        orderAttribute.setOrderProduct(oproduct);
        orderAttribute.setProductAttributeName(colorDescription.getName());
        orderAttribute.setProductAttributeValueName(redDescription.getName());
        orderAttribute.setProductOptionId(color.getId());
        orderAttribute.setProductOptionValueId(red.getId());
        orderAttribute.setProductAttributePrice(colorAttribute.getProductAttributePrice());

        Set<OrderProductAttribute> orderAttributes = new HashSet<OrderProductAttribute>();
        orderAttributes.add(orderAttribute);

        oproduct.setOrderAttributes(orderAttributes);

        oproductprice.setOrderProduct(oproduct);
        orderProductDownload.setOrderProduct(oproduct);
        order.getOrderProducts().add(oproduct);


        //product #2
        OrderProductPrice oproductprice2 = new OrderProductPrice();
        oproductprice2.setDefaultPrice(true);
        oproductprice2.setProductPrice(new BigDecimal(9.99));
        oproductprice2.setProductPriceCode("baseprice");
        oproductprice2.setProductPriceName("Base Price");

        //OrderProduct
        OrderProduct oproduct2 = new OrderProduct();
        oproduct2.setOneTimeCharge(new BigDecimal(9.99));
        oproduct2.setOrder(order);
        oproduct2.setProductName("Additional item name");
        oproduct2.setProductQuantity(1);
        oproduct2.setSku("TB12346");
        oproduct2.getPrices().add(oproductprice2);

        oproductprice2.setOrderProduct(oproduct2);
        order.getOrderProducts().add(oproduct2);


        //requires
        //OrderProduct
        //OrderProductPrice
        //OrderTotal


        //OrderTotal
        OrderTotal subtotal = new OrderTotal();
        subtotal.setModule("summary");
        subtotal.setSortOrder(0);
        subtotal.setText("Summary");
        subtotal.setTitle("Summary");
        subtotal.setValue(new BigDecimal(19.99));
        subtotal.setOrder(order);

        order.getOrderTotal().add(subtotal);

        OrderTotal tax = new OrderTotal();
        tax.setModule("tax");
        tax.setSortOrder(1);
        tax.setText("Tax");
        tax.setTitle("Tax");
        tax.setValue(new BigDecimal(4));
        tax.setOrder(order);

        order.getOrderTotal().add(tax);

        OrderTotal total = new OrderTotal();
        total.setModule("total");
        total.setSortOrder(2);
        total.setText("Total");
        total.setTitle("Total");
        total.setValue(new BigDecimal(23.99));
        total.setOrder(order);

        order.getOrderTotal().add(total);

        orderService.create(order);
        assertTrue(orderService.count() == 1);

        Locale locale = ENGLISH;


        order = orderService.getById(order.getId());

        /**
         * 2 Create an invoice
         */
        try {
            URL resource = getClass().getResource("/templates/invoice/invoice.ods");
            File file = new File(resource.toURI());
            //File file = new File("templates/invoice/invoice.ods");

            Sheet sheet = createFromFile(file).getSheet(0);


            //Store name
            sheet.setValueAt(store.getStorename(), 0, 0);

            store.setStoreaddress("2001 zoo avenue");
            store.setCurrencyFormatNational(true);//use $ instead of USD


            //Address
            //count store address cell
            int storeAddressCell = 2;
            //if(!StringUtils.isBlank(store.getStoreaddress())) {
            //	sheet.setValueAt(store.getStoreaddress(), 0, storeAddressCell);
            //	storeAddressCell ++;
            //}

            //3
            StringBuilder storeAddress = null;
            if (!isBlank(store.getStoreaddress())) {
                storeAddress = new StringBuilder();
                storeAddress.append(store.getStoreaddress());
            }
            if (!isBlank(store.getStorecity())) {
                if (storeAddress == null) {
                    storeAddress = new StringBuilder();
                } else {
                    storeAddress.append(", ");
                }
                storeAddress.append(store.getStorecity());
            }
            if (storeAddress != null) {
                sheet.setValueAt(storeAddress.toString(), 0, storeAddressCell);
                storeAddressCell++;
            }

            //4
            StringBuilder storeProvince = null;
            if (store.getZone() != null) {
                storeProvince = new StringBuilder();
                List<Zone> zones = zoneService.getZones(store.getCountry(), en);
                for (Zone z : zones) {
                    if (z.getCode().equals(store.getZone().getCode())) {
                        storeProvince.append(store.getZone().getName());
                        break;
                    }
                }

            } else {
                if (!isBlank(store.getStorestateprovince())) {
                    storeProvince = new StringBuilder();
                    storeProvince.append(store.getStorestateprovince());
                }
            }
            if (store.getCountry() != null) {
                if (storeProvince == null) {
                    storeProvince = new StringBuilder();
                } else {
                    storeProvince.append(", ");
                }
                Map<String, Country> countries = countryService.getCountriesMap(en);
                Country c = countries.get(store.getCountry().getIsoCode());
                if (c != null) {
                    storeProvince.append(c.getName());
                } else {
                    storeProvince.append(store.getCountry().getIsoCode());
                }

            }
            if (storeProvince != null) {
                sheet.setValueAt(storeProvince.toString(), 0, storeAddressCell);
                storeAddressCell++;
            }

            //5
            if (!isBlank(store.getStorepostalcode())) {
                sheet.setValueAt(store.getStorepostalcode(), 0, storeAddressCell);
                storeAddressCell++;
            }

            //6
            if (!isBlank(store.getStorephone())) {
                sheet.setValueAt(store.getStorephone(), 0, storeAddressCell);
            }

            //delete address blank lines
            for (int i = storeAddressCell; i < 5; i++) {
                sheet.setValueAt("", 0, i);
            }

            //invoice date
            SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
            sheet.setValueAt(format.format(order.getDatePurchased()), 3, 2);

            //invoice number
            sheet.setValueAt(order.getId(), 3, 3);

            //bill to
            //count bill to address cell
            int billToCell = 8;
            if (!isBlank(customer.getBilling().getFirstName())) {
                sheet.setValueAt(customer.getBilling().getFirstName() + " " + customer.getBilling().getLastName(), 0, billToCell);
                billToCell++;
            }

            //9
            if (!isBlank(customer.getBilling().getCompany())) {
                sheet.setValueAt(customer.getBilling().getCompany(), 0, billToCell);
                billToCell++;
            }

            //10
            StringBuilder billToAddress = null;
            if (!isBlank(customer.getBilling().getAddress())) {
                billToAddress = new StringBuilder();
                billToAddress.append(customer.getBilling().getAddress());
            }
            if (!isBlank(customer.getBilling().getCity())) {
                if (billToAddress == null) {
                    billToAddress = new StringBuilder();
                } else {
                    billToAddress.append(", ");
                }
                billToAddress.append(customer.getBilling().getCity());
            }
            if (billToAddress != null) {
                sheet.setValueAt(billToAddress.toString(), 0, billToCell);
                billToCell++;
            }

            //11
            StringBuilder billToProvince = null;
            if (customer.getBilling().getZone() != null) {
                billToProvince = new StringBuilder();
                List<Zone> zones = zoneService.getZones(customer.getBilling().getCountry(), en);
                for (Zone z : zones) {
                    if (z.getCode().equals(customer.getBilling().getZone().getCode())) {
                        billToProvince.append(customer.getBilling().getZone().getName());
                        break;
                    }
                }

            } else {
                if (!isBlank(customer.getBilling().getState())) {
                    billToProvince = new StringBuilder();
                    billToProvince.append(customer.getBilling().getState());
                }
            }
            if (customer.getBilling().getCountry() != null) {
                if (billToProvince == null) {
                    billToProvince = new StringBuilder();
                } else {
                    billToProvince.append(", ");
                }
                Map<String, Country> countries = countryService.getCountriesMap(en);
                Country c = countries.get(customer.getBilling().getCountry().getIsoCode());
                if (c != null) {
                    billToProvince.append(c.getName());
                } else {
                    billToProvince.append(customer.getBilling().getCountry().getIsoCode());
                }

            }
            if (billToProvince != null) {
                sheet.setValueAt(billToProvince.toString(), 0, billToCell);
                billToCell++;
            }

            //12
            if (!isBlank(customer.getBilling().getPostalCode())) {
                sheet.setValueAt(customer.getBilling().getPostalCode(), 0, billToCell);
                billToCell++;
            }

            //13
            if (!isBlank(customer.getBilling().getTelephone())) {
                sheet.setValueAt(customer.getBilling().getTelephone(), 0, billToCell);
            }

            //delete address blank lines
            for (int i = billToCell; i < 13; i++) {
                sheet.setValueAt("", 0, i);
            }

            //products
            Set<OrderProduct> orderProducts = order.getOrderProducts();
            int productCell = 16;
            for (OrderProduct orderProduct : orderProducts) {

                //product name
                String pName = orderProduct.getProductName();
                Set<OrderProductAttribute> oAttributes = orderProduct.getOrderAttributes();
                StringBuilder attributeName = null;
                for (OrderProductAttribute oProductAttribute : oAttributes) {
                    if (attributeName == null) {
                        attributeName = new StringBuilder();
                        attributeName.append("[");
                    } else {
                        attributeName.append(", ");
                    }
                    attributeName.append(oProductAttribute.getProductAttributeName())
                            .append(": ")
                            .append(oProductAttribute.getProductAttributeValueName());

                }


                StringBuilder productName = new StringBuilder();
                productName.append(pName);

                if (attributeName != null) {
                    attributeName.append("]");
                    productName.append(" ").append(attributeName.toString());
                }


                sheet.setValueAt(productName.toString(), 0, productCell);

                int quantity = orderProduct.getProductQuantity();
                sheet.setValueAt(quantity, 1, productCell);
                String amount = priceUtil.getStoreFormatedAmountWithCurrency(store, orderProduct.getOneTimeCharge());
                sheet.setValueAt(amount, 2, productCell);
                String t = priceUtil.getStoreFormatedAmountWithCurrency(store, priceUtil.getOrderProductTotalPrice(store, orderProduct));
                sheet.setValueAt(t, 3, productCell);

                productCell++;

            }

            //print totals
            productCell++;
            Set<OrderTotal> totals = order.getOrderTotal();
            for (OrderTotal orderTotal : totals) {

                String totalName = orderTotal.getText();
                String totalValue = priceUtil.getStoreFormatedAmountWithCurrency(store, orderTotal.getValue());
                sheet.setValueAt(totalName, 2, productCell);
                sheet.setValueAt(totalValue, 3, productCell);
                productCell++;
            }

            //sheet.getCellAt(0, 0).setImage(arg0)
            //sheet.getCellAt(0, 0).setStyleName(arg0)
            //sheet.getCellAt(0, 0).getStyle().


            File outputFile = new File(order.getId() + "_invoice.ods");
            open(sheet.getSpreadSheet().saveAs(outputFile));


            final OpenDocument doc = new OpenDocument();
            doc.loadFrom(order.getId() + "_invoice.ods");

            // Open the PDF document
            Document document = new Document(A4);
            File outFile = new File("invoice.pdf");

            PdfDocument pdf = new PdfDocument();

            document.addDocListener(pdf);

            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
            PdfWriter writer = getInstance(pdf, fileOutputStream);
            pdf.addWriter(writer);

            document.open();

            // Create a template and a Graphics2D object
            Rectangle pageSize = document.getPageSize();
            int w = (int) (pageSize.getWidth() * 0.9);
            int h = (int) (pageSize.getHeight() * 0.95);
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(w, h);

            Graphics2D g2 = tp.createPrinterGraphics(w, h, null);
            // If you want to prevent copy/paste, you can use
            // g2 = tp.createGraphicsShapes(w, h, true, 0.9f);

            tp.setWidth(w);
            tp.setHeight(h);

            // Configure the renderer
            ODTRenderer renderer = new ODTRenderer(doc);
            renderer.setIgnoreMargins(true);
            renderer.setPaintMaxResolution(true);

            // Scale the renderer to fit width
            renderer.setResizeFactor(renderer.getPrintWidth() / w);
            // Render
            renderer.paintComponent(g2);
            g2.dispose();

            // Add our spreadsheet in the middle of the page
            float offsetX = (pageSize.getWidth() - w) / 2;
            float offsetY = (pageSize.getHeight() - h) / 2;
            cb.addTemplate(tp, offsetX, offsetY);
            // Close the PDF document
            document.close();

            outputFile.delete();//remove temp file

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }


}