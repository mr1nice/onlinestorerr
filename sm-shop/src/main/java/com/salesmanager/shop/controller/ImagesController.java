package com.salesmanager.shop.controller;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.product.image.ProductImageService;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.model.catalog.product.file.ProductImageSize;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.OutputContentFile;

import static com.salesmanager.core.model.catalog.product.file.ProductImageSize.LARGE;
import static com.salesmanager.core.model.catalog.product.file.ProductImageSize.SMALL;
import static com.salesmanager.core.model.content.FileContentType.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

@Controller
public class ImagesController {

    private static final Logger LOGGER = getLogger(ImagesController.class);


    @Inject
    private ContentService contentService;

    @Inject
    private ProductImageService productImageService;

    /**
     * Logo, content image
     *
     * @param storeId
     * @param imageType (LOGO, CONTENT, IMAGE)
     * @param imageName
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    @RequestMapping("/static/files/{storeCode}/{imageType}/{imageName}.{extension}")
    public @ResponseBody
    byte[] printImage(@PathVariable final String storeCode, @PathVariable final String imageType, @PathVariable final String imageName, @PathVariable final String extension) throws IOException, ServiceException {

        // example -> /static/files/DEFAULT/CONTENT/myImage.png

        FileContentType imgType = null;

        if (LOGO.name().equals(imageType)) {
            imgType = LOGO;
        }

        if (IMAGE.name().equals(imageType)) {
            imgType = IMAGE;
        }

        if (PROPERTY.name().equals(imageType)) {
            imgType = PROPERTY;
        }

        OutputContentFile image = contentService.getContentFile(storeCode, imgType, new StringBuilder().append(imageName).append(".").append(extension).toString());


        if (image != null) {
            return image.getFile().toByteArray();
        } else {
            //empty image placeholder
            return null;
        }

    }


    /**
     * For product images
     *
     * @param storeCode
     * @param productCode
     * @param imageType
     * @param imageName
     * @param extension
     * @return
     * @throws IOException
     * @Deprecated
     */
    @RequestMapping("/static/{storeCode}/{imageType}/{productCode}/{imageName}.{extension}")
    public @ResponseBody
    byte[] printImage(@PathVariable final String storeCode, @PathVariable final String productCode, @PathVariable final String imageType, @PathVariable final String imageName, @PathVariable final String extension) throws IOException {

        // product image
        // example small product image -> /static/DEFAULT/products/TB12345/product1.jpg

        // example large product image -> /static/DEFAULT/products/TB12345/product1.jpg


        /**
         * List of possible imageType
         *
         */


        ProductImageSize size = SMALL;

        if (imageType.equals(PRODUCTLG.name())) {
            size = LARGE;
        }


        OutputContentFile image = null;
        try {
            image = productImageService.getProductImage(storeCode, productCode, new StringBuilder().append(imageName).append(".").append(extension).toString(), size);
        } catch (ServiceException e) {
            LOGGER.error("Cannot retrieve image " + imageName, e);
        }
        if (image != null) {
            return image.getFile().toByteArray();
        } else {
            //empty image placeholder
            return null;
        }

    }

    /**
     * Exclusive method for dealing with product images
     *
     * @param storeCode
     * @param productCode
     * @param imageName
     * @param extension
     * @param request
     * @return
     * @throws IOException
     */
    @RequestMapping("/static/products/{storeCode}/{productCode}/{imageSize}/{imageName}.{extension}")
    public @ResponseBody
    byte[] printImage(@PathVariable final String storeCode, @PathVariable final String productCode, @PathVariable final String imageSize, @PathVariable final String imageName, @PathVariable final String extension, HttpServletRequest request) throws IOException {

        // product image small
        // example small product image -> /static/products/DEFAULT/TB12345/SMALL/product1.jpg

        // example large product image -> /static/products/DEFAULT/TB12345/LARGE/product1.jpg


        /**
         * List of possible imageType
         *
         */


        ProductImageSize size = SMALL;

        if (PRODUCTLG.name().equals(imageSize)) {
            size = LARGE;
        }


        OutputContentFile image = null;
        try {
            image = productImageService.getProductImage(storeCode, productCode, new StringBuilder().append(imageName).append(".").append(extension).toString(), size);
        } catch (ServiceException e) {
            LOGGER.error("Cannot retrieve image " + imageName, e);
        }
        if (image != null) {
            return image.getFile().toByteArray();
        } else {
            //empty image placeholder
            return null;
        }

    }

    /**
     * Exclusive method for dealing with product images
     *
     * @param storeCode
     * @param productCode
     * @param imageName
     * @param extension
     * @param request
     * @return
     * @throws IOException
     */
    @RequestMapping("/static/products/{storeCode}/{productCode}/{imageName}.{extension}")
    public @ResponseBody
    byte[] printImage(@PathVariable final String storeCode, @PathVariable final String productCode, @PathVariable final String imageName, @PathVariable final String extension, HttpServletRequest request) throws IOException {

        // product image
        // example small product image -> /static/products/DEFAULT/TB12345/product1.jpg?size=small

        // example large product image -> /static/products/DEFAULT/TB12345/product1.jpg
        // or
        //example large product image -> /static/products/DEFAULT/TB12345/product1.jpg?size=large


        /**
         * List of possible imageType
         *
         */


        ProductImageSize size = LARGE;


        if (isNotBlank(request.getParameter("size"))) {
            String requestSize = request.getParameter("size");
            if (requestSize.equals(SMALL.name())) {
                size = SMALL;
            }
        }


        OutputContentFile image = null;
        try {
            image = productImageService.getProductImage(storeCode, productCode, new StringBuilder().append(imageName).append(".").append(extension).toString(), size);
        } catch (ServiceException e) {
            LOGGER.error("Cannot retrieve image " + imageName, e);
        }
        if (image != null) {
            return image.getFile().toByteArray();
        } else {
            //empty image placeholder
            return null;
        }

    }

}
