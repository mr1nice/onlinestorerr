package com.salesmanager.test.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.inject.Inject;

import com.salesmanager.test.common.AbstractSalesManagerCoreTestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.InputContentFile;
import com.salesmanager.core.model.content.OutputContentFile;
import com.salesmanager.core.model.merchant.MerchantStore;

import static com.salesmanager.core.model.content.FileContentType.LOGO;
import static com.salesmanager.core.model.merchant.MerchantStore.DEFAULT_STORE;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.io.IOUtils.toByteArray;


@Ignore
public class ContentImagesTest extends AbstractSalesManagerCoreTestCase {

    private static final Date date = new Date(currentTimeMillis());

    @Inject
    private ContentService contentService;


    //@Test
    @Ignore
    public void createStoreLogo()
            throws ServiceException, FileNotFoundException, IOException {

        MerchantStore store = merchantService.getByCode(DEFAULT_STORE);

        final File file1 = new File("C:/doc/Hadoop.jpg");

        if (!file1.exists() || !file1.canRead()) {
            throw new ServiceException("Can't read" + file1.getAbsolutePath());
        }

        byte[] is = toByteArray(new FileInputStream(file1));
        ByteArrayInputStream inputStream = new ByteArrayInputStream(is);
        InputContentFile cmsContentImage = new InputContentFile();

        cmsContentImage.setFileName(file1.getName());
        cmsContentImage.setFile(inputStream);


        //logo as a content
        contentService.addLogo(store.getCode(), cmsContentImage);

        store.setStoreLogo(file1.getName());
        merchantService.update(store);

        //query the store
        store = merchantService.getByCode(DEFAULT_STORE);


        //get the logo
        String logo = store.getStoreLogo();

        OutputContentFile image = contentService.getContentFile(store.getCode(), LOGO, logo);

        //print image
        OutputStream outputStream = new FileOutputStream("C:/doc/logo-" + image.getFileName());

        ByteArrayOutputStream baos = image.getFile();
        baos.writeTo(outputStream);


        //remove image
        contentService.removeFile(store.getCode(), LOGO, store.getStoreLogo());


    }


}