package com.salesmanager.core.business.modules.cms.product.local;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.modules.cms.impl.LocalCacheManagerImpl;
import com.salesmanager.core.business.modules.cms.product.ProductImageGet;
import com.salesmanager.core.business.modules.cms.product.ProductImagePut;
import com.salesmanager.core.business.modules.cms.product.ProductImageRemove;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.file.ProductImageSize;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.ImageContentFile;
import com.salesmanager.core.model.content.OutputContentFile;
import com.salesmanager.core.model.merchant.MerchantStore;

import static com.salesmanager.core.business.constants.Constants.SLASH;
import static com.salesmanager.core.model.content.FileContentType.PRODUCT;
import static com.salesmanager.core.model.content.FileContentType.PRODUCTLG;
import static java.nio.file.Files.*;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.slf4j.LoggerFactory.getLogger;

public class CmsImageFileManagerImpl
        implements ProductImagePut, ProductImageGet, ProductImageRemove {

    private static final Logger LOGGER = getLogger(CmsImageFileManagerImpl.class);

    private static CmsImageFileManagerImpl fileManager = null;

    private final static String ROOT_NAME = "";

    private final static String SMALL = "SMALL";
    private final static String LARGE = "LARGE";

    private static final String ROOT_CONTAINER = "products";

    private String rootName = ROOT_NAME;

    private LocalCacheManagerImpl cacheManager;


    public static CmsImageFileManagerImpl getInstance() {

        if (fileManager == null) {
            fileManager = new CmsImageFileManagerImpl();
        }

        return fileManager;

    }

    private CmsImageFileManagerImpl() {

    }

    /**
     * root/products/<merchant id>/<product id>/1.jpg
     */

    @Override
    public void addProductImage(ProductImage productImage,
                                ImageContentFile contentImage)
            throws ServiceException {


        try {

            //base path
            String rootPath = this.buildRootPath();
            Path confDir = get(rootPath);
            this.createDirectoryIfNorExist(confDir);

            //node path
            StringBuilder nodePath = new StringBuilder();
            nodePath
                    .append(rootPath)
                    .append(productImage.getProduct().getMerchantStore().getCode());
            Path merchantPath = get(nodePath.toString());
            this.createDirectoryIfNorExist(merchantPath);

            //product path
            nodePath.append(SLASH).append(productImage.getProduct().getSku()).append(SLASH);
            Path dirPath = get(nodePath.toString());
            this.createDirectoryIfNorExist(dirPath);

            //small large
            if (contentImage.getFileContentType().name().equals(PRODUCT.name())) {
                nodePath.append(SMALL);
            } else if (contentImage.getFileContentType().name().equals(PRODUCTLG.name())) {
                nodePath.append(LARGE);
            }
            Path sizePath = get(nodePath.toString());
            this.createDirectoryIfNorExist(sizePath);


            //file creation
            nodePath.append(SLASH).append(contentImage.getFileName());


            Path path = get(nodePath.toString());
            InputStream isFile = contentImage.getFile();

            copy(isFile, path, REPLACE_EXISTING);


        } catch (Exception e) {

            throw new ServiceException(e);

        }

    }

    @Override
    public OutputContentFile getProductImage(ProductImage productImage)
            throws ServiceException {

        //the web server takes care of the images
        return null;

    }


    public List<OutputContentFile> getImages(MerchantStore store, FileContentType imageContentType)
            throws ServiceException {

        //the web server takes care of the images

        return null;

    }

    @Override
    public List<OutputContentFile> getImages(Product product)
            throws ServiceException {

        //the web server takes care of the images

        return null;
    }


    @Override
    public void removeImages(final String merchantStoreCode)
            throws ServiceException {

        try {


            StringBuilder merchantPath = new StringBuilder();
            merchantPath.append(buildRootPath())
                    .append(SLASH)
                    .append(merchantStoreCode);

            Path path = get(merchantPath.toString());

            deleteIfExists(path);


        } catch (Exception e) {
            throw new ServiceException(e);
        }


    }


    @Override
    public void removeProductImage(ProductImage productImage)
            throws ServiceException {


        try {


            StringBuilder nodePath = new StringBuilder();
            nodePath.append(buildRootPath())
                    .append(SLASH)
                    .append(productImage.getProduct().getMerchantStore().getCode()).append(SLASH).append(productImage.getProduct().getSku());

            //delete small
            StringBuilder smallPath = new StringBuilder(nodePath);
            smallPath.append(SLASH).append(SMALL).append(SLASH).append(productImage.getProductImage());


            Path path = get(smallPath.toString());

            deleteIfExists(path);

            //delete large
            StringBuilder largePath = new StringBuilder(nodePath);
            largePath.append(SLASH).append(LARGE).append(SLASH).append(productImage.getProductImage());


            path = get(largePath.toString());

            deleteIfExists(path);

        } catch (Exception e) {
            throw new ServiceException(e);
        }


    }

    @Override
    public void removeProductImages(Product product)
            throws ServiceException {

        try {


            StringBuilder nodePath = new StringBuilder();
            nodePath.append(buildRootPath())
                    .append(SLASH)
                    .append(product.getMerchantStore().getCode()).append(SLASH).append(product.getSku());


            Path path = get(nodePath.toString());

            deleteIfExists(path);

        } catch (Exception e) {
            throw new ServiceException(e);
        }

    }


    @Override
    public List<OutputContentFile> getImages(final String merchantStoreCode,
                                             FileContentType imageContentType) throws ServiceException {

        //the web server taks care of the images

        return null;
    }

    @Override
    public OutputContentFile getProductImage(String merchantStoreCode,
                                             String productCode, String imageName) throws ServiceException {
        return getProductImage(merchantStoreCode, productCode, imageName, ProductImageSize.SMALL.name());
    }

    @Override
    public OutputContentFile getProductImage(String merchantStoreCode,
                                             String productCode, String imageName, ProductImageSize size)
            throws ServiceException {
        return getProductImage(merchantStoreCode, productCode, imageName, size.name());
    }

    private OutputContentFile getProductImage(String merchantStoreCode,
                                              String productCode, String imageName, String size) throws ServiceException {

        return null;

    }


    private String buildRootPath() {
        return new StringBuilder().append(getRootName()).append(SLASH).append(ROOT_CONTAINER).append(SLASH).toString();

    }


    private void createDirectoryIfNorExist(Path path) throws IOException {

        if (notExists(path)) {
            createDirectory(path);
        }
    }

    public void setRootName(String rootName) {
        this.rootName = rootName;
    }

    public String getRootName() {
        return rootName;
    }

    public LocalCacheManagerImpl getCacheManager() {
        return cacheManager;
    }

    public void setCacheManager(LocalCacheManagerImpl cacheManager) {
        this.cacheManager = cacheManager;
    }


}
