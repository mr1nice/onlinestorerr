package com.salesmanager.shop.utils;

import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

import static com.salesmanager.shop.constants.Constants.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static org.springframework.web.servlet.support.RequestContextUtils.getLocaleResolver;

@Component
public class LanguageUtils {
	
	@Inject
	LanguageService languageService;

    public Language getRequestLanguage(HttpServletRequest request, HttpServletResponse response) {

        Locale locale = null;

        Language language = (Language) request.getSession().getAttribute(LANGUAGE);
        MerchantStore store = (MerchantStore) request.getSession().getAttribute(MERCHANT_STORE);


        if (language == null) {
            try {

                locale = getLocale();//should be browser locale


                if (store != null) {
                    language = store.getDefaultLanguage();
                    if (language != null) {
                        locale = languageService.toLocale(language, store);
                        if (locale != null) {
                            setLocale(locale);
                        }
                        request.getSession().setAttribute(LANGUAGE, language);
                    }

                    if (language == null) {
                        language = languageService.toLanguage(locale);
                        request.getSession().setAttribute(LANGUAGE, language);
                    }

                }

            } catch (Exception e) {
                if (language == null) {
                    try {
                        language = languageService.getByCode(DEFAULT_LANGUAGE);
                    } catch (Exception ignore) {
                    }
                }
            }
        } else {


            Locale localeFromContext = getLocale();//should be browser locale
            if (!language.getCode().equals(localeFromContext.getLanguage())) {
                //get locale context
                language = languageService.toLanguage(localeFromContext);
            }

        }

        if (language != null) {
            locale = languageService.toLocale(language, store);
        } else {
            language = languageService.toLanguage(locale);
        }

        LocaleResolver localeResolver = getLocaleResolver(request);
        if (localeResolver != null) {
            localeResolver.setLocale(request, response, locale);
        }
        response.setLocale(locale);
        request.getSession().setAttribute(LANGUAGE, language);

        return language;
    }

    public Language getRESTLanguage(HttpServletRequest request, MerchantStore store) throws Exception {

        notNull(request, "HttpServletRequest must not be null");
        notNull(store, "MerchantStore must not be null");

        Language language = null;


        String lang = request.getParameter(LANG);

        if (isBlank(lang)) {
            //try with HttpSession
            language = (Language) request.getSession().getAttribute(LANGUAGE);
            if (language == null) {
                language = store.getDefaultLanguage();
            }

            if (language == null) {
                language = languageService.defaultLanguage();
            }
        } else {
            language = languageService.getByCode(lang);
            if (language == null) {
                language = (Language) request.getSession().getAttribute(LANGUAGE);
                if (language == null) {
                    language = store.getDefaultLanguage();
                }

                if (language == null) {
                    language = languageService.defaultLanguage();
                }
            }
        }

        return language;
    }

}
