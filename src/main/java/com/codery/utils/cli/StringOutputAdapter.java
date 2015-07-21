package com.codery.utils.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StringOutputAdapter extends OutputStream {

    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    @Override
    public void write(int arg0) throws IOException {
        outStream.write(arg0);
    }

    public String getWrittenContent() {
        String ret = new String(outStream.toByteArray());
        outStream.reset();
        return ret;
    }

    @Override
    public void close() throws IOException {
        outStream.close();
    }

    @Override
    public void flush() throws IOException {
        outStream.flush();
    }

}
