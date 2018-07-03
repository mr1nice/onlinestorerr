package com.salesmanager.core.business.modules.cms.impl;

import com.google.api.client.util.Value;

public class StaticContentCacheManagerImpl extends CacheManagerImpl {

    private final static String NAMED_CACHE = "FilesRepository";

    @Value(("${config.cms.files.location}"))
    private String location = null;


    public StaticContentCacheManagerImpl(String location) {

        super.init(NAMED_CACHE, location);


    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
