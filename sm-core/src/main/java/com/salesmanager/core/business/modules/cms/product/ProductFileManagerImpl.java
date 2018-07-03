package com.salesmanager.core.business.modules.cms.product;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.business.utils.ProductImageCropUtils;
import com.salesmanager.core.business.utils.ProductImageSizeUtils;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.file.ProductImageSize;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.ImageContentFile;
import com.salesmanager.core.model.content.OutputContentFile;

import static com.salesmanager.core.business.constants.Constants.TRUE;
import static com.salesmanager.core.business.utils.ProductImageSizeUtils.resizeWithRatio;
import static com.salesmanager.core.model.content.FileContentType.PRODUCT;
import static com.salesmanager.core.model.content.FileContentType.PRODUCTLG;
import static java.io.File.createTempFile;
import static java.lang.Integer.parseInt;
import static java.net.URLConnection.getFileNameMap;
import static javax.imageio.ImageIO.read;
import static javax.imageio.ImageIO.write;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class ProductFileManagerImpl extends ProductFileManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ProductFileManagerImpl.class);
	

	private ProductImagePut uploadImage;
	private ProductImageGet getImage;
	private ProductImageRemove removeImage;
	
	private CoreConfiguration configuration;
	
	private final static String PRODUCT_IMAGE_HEIGHT_SIZE = "PRODUCT_IMAGE_HEIGHT_SIZE";
	private final static String PRODUCT_IMAGE_WIDTH_SIZE = "PRODUCT_IMAGE_WIDTH_SIZE";
	private final static String CROP_UPLOADED_IMAGES ="CROP_UPLOADED_IMAGES";


	public CoreConfiguration getConfiguration() {
		return configuration;
	}


	public void setConfiguration(CoreConfiguration configuration) {
		this.configuration = configuration;
	}


	public ProductImageRemove getRemoveImage() {
		return removeImage;
	}


	public void setRemoveImage(ProductImageRemove removeImage) {
		this.removeImage = removeImage;
	}


	public void addProductImage(ProductImage productImage, ImageContentFile contentImage)
	throws ServiceException {
	
	
	try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = contentImage.getFile().read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
        InputStream is2 = new ByteArrayInputStream(baos.toByteArray());

        BufferedImage bufferedImage = read(is2);


        if (bufferedImage == null) {
            LOGGER.error("Cannot read image format for " + productImage.getProductImage());
            throw new Exception("Cannot read image format " + productImage.getProductImage());
        }
        contentImage.setFile(is1);
        contentImage.setFileContentType(PRODUCTLG);
        uploadImage.addProductImage(productImage, contentImage);

        String slargeImageHeight = configuration.getProperty(PRODUCT_IMAGE_HEIGHT_SIZE);
        String slargeImageWidth = configuration.getProperty(PRODUCT_IMAGE_WIDTH_SIZE);


        if (!isBlank(slargeImageHeight) && !isBlank(slargeImageWidth)) {


            FileNameMap fileNameMap = getFileNameMap();

            String contentType = fileNameMap.getContentTypeFor(contentImage.getFileName());
            String extension = null;
            if (contentType != null) {
                extension = contentType.substring(contentType.indexOf("/") + 1, contentType.length());
            }

            if (extension == null) {
                extension = "jpeg";
            }


            int largeImageHeight = parseInt(slargeImageHeight);
            int largeImageWidth = parseInt(slargeImageWidth);

            if (largeImageHeight <= 0 || largeImageWidth <= 0) {
                String sizeMsg = "Image configuration set to an invalid value [PRODUCT_IMAGE_HEIGHT_SIZE] " + largeImageHeight + " , [PRODUCT_IMAGE_WIDTH_SIZE] " + largeImageWidth;
                LOGGER.error(sizeMsg);
                throw new ServiceException(sizeMsg);
            }

            if (!isBlank(configuration.getProperty(CROP_UPLOADED_IMAGES)) && configuration.getProperty(CROP_UPLOADED_IMAGES).equals(TRUE)) {
                ProductImageCropUtils utils = new ProductImageCropUtils(bufferedImage, largeImageWidth, largeImageHeight);
                if (utils.isCropeable()) {
                    bufferedImage = utils.getCroppedImage();
                }
            }

            //resize large
            BufferedImage largeResizedImage = resizeWithRatio(bufferedImage, largeImageWidth, largeImageHeight);


            File tempLarge = createTempFile(new StringBuilder().append(productImage.getProduct().getId()).append("tmpLarge").toString(), "." + extension);
            write(largeResizedImage, extension, tempLarge);

            FileInputStream isLarge = new FileInputStream(tempLarge);


            ImageContentFile largeContentImage = new ImageContentFile();
            largeContentImage.setFileContentType(PRODUCT);
            largeContentImage.setFileName(productImage.getProductImage());
            largeContentImage.setFile(isLarge);


            uploadImage.addProductImage(productImage, largeContentImage);

            tempLarge.delete();


        } else {
            contentImage.setFileContentType(PRODUCT);
            uploadImage.addProductImage(productImage, contentImage);
        }


    } catch (Exception e) {
		throw new ServiceException(e);
	} finally {
		try {
			productImage.getImage().close();
		} catch(Exception ignore) {}
	}

}

	
	public OutputContentFile getProductImage(ProductImage productImage) throws ServiceException {
        return getImage.getProductImage(productImage);
    }

	
	@Override
	public List<OutputContentFile> getImages(final String merchantStoreCode, FileContentType imageContentType)
			throws ServiceException {
        return getImage.getImages(merchantStoreCode, PRODUCT);
    }
	
	@Override
	public List<OutputContentFile> getImages(Product product)
			throws ServiceException {
		return getImage.getImages(product);
	}






	@Override
	public void removeProductImage(ProductImage productImage)
			throws ServiceException {

        this.removeImage.removeProductImage(productImage);

    }


	@Override
	public void removeProductImages(Product product) throws ServiceException {

		this.removeImage.removeProductImages(product);
		
	}


	@Override
	public void removeImages(final String merchantStoreCode) throws ServiceException {
		
		this.removeImage.removeImages(merchantStoreCode);
		
	}


	public ProductImagePut getUploadImage() {
		return uploadImage;
	}


	public void setUploadImage(ProductImagePut uploadImage) {
		this.uploadImage = uploadImage;
	}


	

	public ProductImageGet getGetImage() {
		return getImage;
	}


	public void setGetImage(ProductImageGet getImage) {
		this.getImage = getImage;
	}


	@Override
	public OutputContentFile getProductImage(String merchantStoreCode,
			String productCode, String imageName) throws ServiceException {
		return getImage.getProductImage(merchantStoreCode, productCode, imageName);
	}



	@Override
	public OutputContentFile getProductImage(String merchantStoreCode,
			String productCode, String imageName, ProductImageSize size)
			throws ServiceException {
		return getImage.getProductImage(merchantStoreCode, productCode, imageName, size);
	}





}
