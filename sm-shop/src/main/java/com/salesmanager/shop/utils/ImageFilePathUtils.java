package com.salesmanager.shop.utils;

import org.springframework.stereotype.Component;

import com.salesmanager.shop.constants.Constants;

import static com.salesmanager.shop.constants.Constants.STATIC_URI;

@Component
public class ImageFilePathUtils extends AbstractimageFilePath {

    private String basePath = STATIC_URI;

    @Override
    public String getBasePath() {
        return basePath;
    }

    @Override
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String getContextPath() {
        return super.getProperties().getProperty(CONTEXT_PATH);
    }


}
