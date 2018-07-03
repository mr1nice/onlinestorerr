package com.salesmanager.shop.store.controller;

import javax.servlet.http.HttpServletRequest;

import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.model.paging.PaginationData;
import com.salesmanager.shop.utils.SessionUtil;

import static com.salesmanager.shop.constants.Constants.LANGUAGE;
import static com.salesmanager.shop.utils.SessionUtil.removeSessionAttribute;
import static java.lang.Math.min;

public abstract class AbstractController {


    /**
     * Method which will help to retrieving values from Session
     * based on the key being passed to the method.
     *
     * @param key
     * @return value stored in session corresponding to the key
     */
    @SuppressWarnings("unchecked")
    protected <T> T getSessionAttribute(final String key, HttpServletRequest request) {
        return (T) SessionUtil.getSessionAttribute(key, request);

    }

    protected void setSessionAttribute(final String key, final Object value, HttpServletRequest request) {
        SessionUtil.setSessionAttribute(key, value, request);
    }


    protected void removeAttribute(final String key, HttpServletRequest request) {
        removeSessionAttribute(key, request);
    }

    protected Language getLanguage(HttpServletRequest request) {
        return (Language) request.getAttribute(LANGUAGE);
    }

    protected PaginationData createPaginaionData(final int pageNumber, final int pageSize) {
        final PaginationData paginaionData = new PaginationData(pageSize, pageNumber);

        return paginaionData;
    }

    protected PaginationData calculatePaginaionData(final PaginationData paginationData, final int pageSize, final int resultCount) {

        int currentPage = paginationData.getCurrentPage();


        int count = min((currentPage * pageSize), resultCount);
        paginationData.setCountByPage(count);

        paginationData.setTotalCount(resultCount);
        return paginationData;
    }
}
