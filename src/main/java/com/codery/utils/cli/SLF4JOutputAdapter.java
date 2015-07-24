package com.codery.utils.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLF4JOutputAdapter extends OutputStream {

    private final Logger logger;
    private ByteArrayOutputStream byteOut;
    private static final int BYTES_THRESHOLD = 1024;
    private int bytesWritten;

    public SLF4JOutputAdapter() {
        this(null);
    }

    public SLF4JOutputAdapter(Class<?> clazz) {
        if (clazz != null) {
            logger = LoggerFactory.getLogger(clazz);
        } else {
            logger = LoggerFactory.getLogger(SLF4JOutputAdapter.class);
        }
        byteOut = new ByteArrayOutputStream();
    }

    @Override
    public void write(int arg0) throws IOException {
        byteOut.write(arg0);
        bytesWritten++;
        if (bytesWritten >= BYTES_THRESHOLD) {
            logger.info(new String(byteOut.toByteArray()));
            byteOut.reset();
            bytesWritten = 0;
        }
    }

    @Override
    public void close() throws IOException {
        String remainingContent = byteOut.toString();
        if (!remainingContent.isEmpty()) {
            logger.info(remainingContent);
        }
        super.close();
        byteOut.close();
    }

}
