package com.salesmanager.core.business.modules.cms.content;

import java.util.List;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.OutputContentFile;


public interface FileGet {

    public OutputContentFile getFile(final String merchantStoreCode, FileContentType fileContentType, String contentName) throws ServiceException;

    public List<String> getFileNames(final String merchantStoreCode, FileContentType fileContentType) throws ServiceException;

    public List<OutputContentFile> getFiles(final String merchantStoreCode, FileContentType fileContentType) throws ServiceException;
}
