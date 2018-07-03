package com.salesmanager.test.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import com.salesmanager.test.common.AbstractSalesManagerCoreTestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.InputContentFile;
import com.salesmanager.core.model.content.OutputContentFile;
import com.salesmanager.core.model.merchant.MerchantStore;

import static com.salesmanager.core.model.content.FileContentType.IMAGE;
import static com.salesmanager.core.model.merchant.MerchantStore.DEFAULT_STORE;
import static org.apache.commons.io.IOUtils.toByteArray;


@Ignore
public class StaticContentTest extends AbstractSalesManagerCoreTestCase {


    @Inject
    private ContentService contentService;


    //@Test
    @Ignore
    public void createImage()
            throws ServiceException, FileNotFoundException, IOException {

        MerchantStore store = merchantService.getByCode(DEFAULT_STORE);
        final File file1 = new File("c:/doc/Hadoop.jpg");

        if (!file1.exists() || !file1.canRead()) {
            throw new ServiceException("Can't read" + file1.getAbsolutePath());
        }

        final byte[] is = toByteArray(new FileInputStream(file1));
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(is);
        final InputContentFile cmsContentImage = new InputContentFile();
        cmsContentImage.setFileName(file1.getName());
        cmsContentImage.setFile(inputStream);
        cmsContentImage.setFileContentType(IMAGE);

        //Add image
        contentService.addContentFile(store.getCode(), cmsContentImage);


        //get image
        OutputContentFile image = contentService.getContentFile(store.getCode(), IMAGE, file1.getName());

        //print image
        OutputStream outputStream = new FileOutputStream("c:/doc/content-" + image.getFileName());

        ByteArrayOutputStream baos = image.getFile();
        baos.writeTo(outputStream);


        //remove image
        contentService.removeFile(store.getCode(), IMAGE, file1.getName());


    }


}