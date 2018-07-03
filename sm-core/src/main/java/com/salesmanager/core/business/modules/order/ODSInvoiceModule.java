package com.salesmanager.core.business.modules.order;

import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jopendocument.dom.OOUtils;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.jopendocument.model.OpenDocument;
import org.jopendocument.renderer.ODTRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.utils.ProductPriceUtils;
import com.salesmanager.core.business.utils.ProductUtils;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.OrderTotal;
import com.salesmanager.core.model.order.orderproduct.OrderProduct;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;

import static com.itextpdf.text.PageSize.A4;
import static com.itextpdf.text.pdf.PdfWriter.getInstance;
import static com.salesmanager.core.business.constants.Constants.DEFAULT_DATE_FORMAT;
import static com.salesmanager.core.business.utils.ProductUtils.buildOrderProductDisplayName;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.jopendocument.dom.OOUtils.open;
import static org.jopendocument.dom.spreadsheet.SpreadSheet.createFromFile;


public class ODSInvoiceModule implements InvoiceModule {
	
	private final static String INVOICE_TEMPLATE = "templates/invoice/Invoice";
	private final static String INVOICE_TEMPLATE_EXTENSION = ".ods";
	private final static String TEMP_INVOICE_SUFFIX_NAME = "_invoice.ods";
	private final static int ADDRESS_ROW_START = 2;
	private final static int ADDRESS_ROW_END = 5;
	
	private final static int BILLTO_ROW_START = 8;
	private final static int BILLTO_ROW_END = 13;
	
	private final static int PRODUCT_ROW_START = 16;
	
	private static final Logger LOGGER = LoggerFactory.getLogger( ODSInvoiceModule.class );
	
	@Inject
	private ZoneService zoneService;
	
	@Inject
	private CountryService countryService;
	
	@Inject
	private ProductPriceUtils priceUtil;
	

	@Override
	public ByteArrayOutputStream createInvoice(MerchantStore store, Order order, Language language) throws Exception {


        List<Zone> zones = zoneService.getZones(store.getCountry(), language);
        Map<String, Country> countries = countryService.getCountriesMap(language);
        String template = new StringBuilder().append(INVOICE_TEMPLATE).append("_").append(language.getCode().toLowerCase()).append(INVOICE_TEMPLATE_EXTENSION).toString();
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(template);
        } catch (Exception e) {
            LOGGER.warn("Cannot open template " + template);
            throw new Exception("Cannot open " + template);
        }

        if (is == null) {
            try {
                is = getClass().getClassLoader().getResourceAsStream(new StringBuilder().append(INVOICE_TEMPLATE).append(INVOICE_TEMPLATE_EXTENSION).toString());
            } catch (Exception e) {
                LOGGER.warn("Cannot open template " + template);
                throw new Exception("Cannot open " + new StringBuilder().append(INVOICE_TEMPLATE).append(INVOICE_TEMPLATE_EXTENSION).toString());
            }
        }

        if (is == null) {
            LOGGER.warn("Cannot open template " + template);
            throw new Exception("Cannot open " + new StringBuilder().append(INVOICE_TEMPLATE).append(INVOICE_TEMPLATE_EXTENSION).toString());
        }

        File file = new File(order.getId() + "_working");
        OutputStream os = new FileOutputStream(file);
        copy(is, os);
        os.close();
        //File file = new File(resource.toURI().toURL());

        Sheet sheet = createFromFile(file).getSheet(0);
        sheet.setValueAt(store.getStorename(), 0, 0);
        int storeAddressCell = ADDRESS_ROW_START;

        Map<String, Zone> zns = zoneService.getZones(language);
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
        StringBuilder storeProvince = null;
        if (store.getZone() != null) {
            storeProvince = new StringBuilder();

            for (Zone z : zones) {
                if (z.getCode().equals(store.getZone().getCode())) {
                    storeProvince.append(z.getName());
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
        if (!isBlank(store.getStorepostalcode())) {
            sheet.setValueAt(store.getStorepostalcode(), 0, storeAddressCell);
            storeAddressCell++;
        }
        if (!isBlank(store.getStorephone())) {
            sheet.setValueAt(store.getStorephone(), 0, storeAddressCell);
        }
        for (int i = storeAddressCell; i < ADDRESS_ROW_END; i++) {
            sheet.setValueAt("", 0, i);
        }
        SimpleDateFormat format = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        sheet.setValueAt(format.format(order.getDatePurchased()), 3, 2);
        sheet.setValueAt(order.getId(), 3, 3);
        int billToCell = BILLTO_ROW_START;
        if (!isBlank(order.getBilling().getFirstName())) {
            StringBuilder nm = new StringBuilder();
            nm.append(order.getBilling().getFirstName()).append(" ").append(order.getBilling().getLastName());
            sheet.setValueAt(nm.toString(), 0, billToCell);
            billToCell++;
        }
        if (!isBlank(order.getBilling().getCompany())) {
            sheet.setValueAt(order.getBilling().getCompany(), 0, billToCell);
            billToCell++;
        }
        StringBuilder billToAddress = null;
        if (!isBlank(order.getBilling().getAddress())) {
            billToAddress = new StringBuilder();
            billToAddress.append(order.getBilling().getAddress());
        }
        if (!isBlank(order.getBilling().getCity())) {
            if (billToAddress == null) {
                billToAddress = new StringBuilder();
            } else {
                billToAddress.append(", ");
            }
            billToAddress.append(order.getBilling().getCity());
        }
        if (billToAddress != null) {
            sheet.setValueAt(billToAddress.toString(), 0, billToCell);
            billToCell++;
        }
        StringBuilder billToProvince = null;
        if (order.getBilling().getZone() != null) {
            billToProvince = new StringBuilder();

            Zone billingZone = zns.get(order.getBilling().getZone().getCode());
            if (billingZone != null) {
                billToProvince.append(billingZone.getName());
            }

        } else {
            if (!isBlank(order.getBilling().getState())) {
                billToProvince = new StringBuilder();
                billToProvince.append(order.getBilling().getState());
            }
        }
        if (order.getBilling().getCountry() != null) {
            if (billToProvince == null) {
                billToProvince = new StringBuilder();
            } else {
                billToProvince.append(", ");
            }
            Country c = countries.get(order.getBilling().getCountry().getIsoCode());
            if (c != null) {
                billToProvince.append(c.getName());
            } else {
                billToProvince.append(order.getBilling().getCountry().getIsoCode());
            }

        }
        if (billToProvince != null) {
            sheet.setValueAt(billToProvince.toString(), 0, billToCell);
            billToCell++;
        }
        if (!isBlank(order.getBilling().getPostalCode())) {
            billToCell++;
            sheet.setValueAt(order.getBilling().getPostalCode(), 0, billToCell);
            billToCell++;
        }
        if (!isBlank(order.getBilling().getTelephone())) {
            sheet.setValueAt(order.getBilling().getTelephone(), 0, billToCell);
        }
        for (int i = billToCell; i < BILLTO_ROW_END; i++) {
            sheet.setValueAt("", 0, i);
        }
        Set<OrderProduct> orderProducts = order.getOrderProducts();
        int productCell = PRODUCT_ROW_START;
        for (OrderProduct orderProduct : orderProducts) {


            String orderProductName = buildOrderProductDisplayName(orderProduct);

            sheet.setValueAt(orderProductName.toString(), 0, productCell);

            int quantity = orderProduct.getProductQuantity();
            sheet.setValueAt(quantity, 1, productCell);
            String amount = priceUtil.getStoreFormatedAmountWithCurrency(store, orderProduct.getOneTimeCharge());
            sheet.setValueAt(amount, 2, productCell);
            String t = priceUtil.getStoreFormatedAmountWithCurrency(store, priceUtil.getOrderProductTotalPrice(store, orderProduct));
            sheet.setValueAt(t, 3, productCell);

            productCell++;

        }
        productCell++;
        Set<OrderTotal> totals = order.getOrderTotal();
        for (OrderTotal orderTotal : totals) {

            String totalName = orderTotal.getText();
            if (totalName.contains(".")) {
                totalName = orderTotal.getTitle();
            }
            String totalValue = priceUtil.getStoreFormatedAmountWithCurrency(store, orderTotal.getValue());
            sheet.setValueAt(totalName, 2, productCell);
            sheet.setValueAt(totalValue, 3, productCell);
            productCell++;
        }
        StringBuilder tempInvoiceName = new StringBuilder();
        tempInvoiceName.append(order.getId()).append(TEMP_INVOICE_SUFFIX_NAME);
        File outputFile = new File(tempInvoiceName.toString());
        open(sheet.getSpreadSheet().saveAs(outputFile));


        final OpenDocument doc = new OpenDocument();
        doc.loadFrom(tempInvoiceName.toString());
        Document document = new Document(A4);

        PdfDocument pdf = new PdfDocument();

        document.addDocListener(pdf);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();


        PdfWriter writer = getInstance(pdf, outputStream);
        pdf.addWriter(writer);

        document.open();
        Rectangle pageSize = document.getPageSize();
        int w = (int) (pageSize.getWidth() * 0.9);
        int h = (int) (pageSize.getHeight() * 0.95);
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate(w, h);

        Graphics2D g2 = tp.createPrinterGraphics(w, h, null);

        tp.setWidth(w);
        tp.setHeight(h);
        ODTRenderer renderer = new ODTRenderer(doc);
        renderer.setIgnoreMargins(true);
        renderer.setPaintMaxResolution(true);
        renderer.setResizeFactor(renderer.getPrintWidth() / w);
        renderer.paintComponent(g2);
        g2.dispose();
        float offsetX = (float) ((pageSize.getWidth() - w) / 2);
        float offsetY = (float) ((pageSize.getHeight() - h) / 2);
        cb.addTemplate(tp, offsetX, offsetY);
        document.close();
        outputFile.delete();
        file.delete();
        is.close();
        return outputStream;


    }

}
