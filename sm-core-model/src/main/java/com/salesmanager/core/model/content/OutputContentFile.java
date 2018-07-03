package com.salesmanager.core.model.content;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

public class OutputContentFile extends StaticContentFile implements Serializable {
    private static final long serialVersionUID = 1L;
    private ByteArrayOutputStream file;

    public ByteArrayOutputStream getFile() {
        return file;
    }

    public void setFile(ByteArrayOutputStream file) {
        this.file = file;
    }

}