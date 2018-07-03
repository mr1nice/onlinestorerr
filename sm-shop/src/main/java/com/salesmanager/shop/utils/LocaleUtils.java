package com.salesmanager.shop.utils;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;

import static com.salesmanager.core.business.constants.Constants.DEFAULT_LOCALE;
import static java.util.Locale.getAvailableLocales;


public class LocaleUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LocaleUtils.class);
	
	public static Locale getLocale(Language language) {
		
		return new Locale(language.getCode());
		
	}

    public static Locale getLocale(MerchantStore store) {

        Locale defaultLocale = DEFAULT_LOCALE;
        Locale[] locales = getAvailableLocales();
        for (int i = 0; i < locales.length; i++) {
            Locale l = locales[i];
            try {
                if (l.getISO3Country().equals(store.getCurrency().getCode())) {
                    defaultLocale = l;
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("An error occured while getting ISO code for locale " + l.toString());
            }
        }

        return defaultLocale;

    }
	

}
