package com.salesmanager.shop.admin.model.content;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;

public class ContentFiles implements Serializable {

    private static final long serialVersionUID = 1L;


    private List<MultipartFile> file;

    public void setFile(List<MultipartFile> file) {
        this.file = file;
    }

    private String fileName;

    //@NotEmpty(message="{merchant.files.invalid}")
    //@Valid
    public List<MultipartFile> getFile() {
        return file;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }


}
