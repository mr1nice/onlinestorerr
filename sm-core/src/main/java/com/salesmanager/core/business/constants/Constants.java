package com.salesmanager.core.business.constants;

import java.util.Currency;
import java.util.Locale;

import static java.util.Currency.getInstance;
import static java.util.Locale.US;

public class Constants {

    public final static String TEST_ENVIRONMENT = "TEST";
    public final static String PRODUCTION_ENVIRONMENT = "PROD";
    public final static String SHOP_URI = "/shop";

    public static final String ALL_REGIONS = "*";


    public final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public final static String DEFAULT_DATE_FORMAT_YEAR = "yyyy";
    public final static String DEFAULT_LANGUAGE = "en";
    public final static String DEFAULT_COUNTRY = "CA";

    public final static String EMAIL_CONFIG = "EMAIL_CONFIG";

    public final static String UNDERSCORE = "_";
    public final static String SLASH = "/";
    public final static String TRUE = "true";
    public final static String FALSE = "false";
    public final static String OT_ITEM_PRICE_MODULE_CODE = "itemprice";
    public final static String OT_SUBTOTAL_MODULE_CODE = "subtotal";
    public final static String OT_TOTAL_MODULE_CODE = "total";
    public final static String OT_SHIPPING_MODULE_CODE = "shipping";
    public final static String OT_HANDLING_MODULE_CODE = "handling";
    public final static String OT_TAX_MODULE_CODE = "tax";
    public final static String OT_REFUND_MODULE_CODE = "refund";
    public final static String OT_DISCOUNT_TITLE = "order.total.discount";

    public final static String DEFAULT_STORE = "DEFAULT";

    public final static Locale DEFAULT_LOCALE = US;
    public final static Currency DEFAULT_CURRENCY = getInstance(US);

}
