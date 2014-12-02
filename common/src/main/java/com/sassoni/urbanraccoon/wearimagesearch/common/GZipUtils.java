package com.sassoni.urbanraccoon.wearimagesearch.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Compressing and uncompressing utilities <br><br>
 * Credit to <a href="http://stackoverflow.com/users/843985/scessor">scessor</a>
 * @<a href="http://stackoverflow.com/">stackoverflow</a> for his really nice answer:
 * <a href="http://goo.gl/uFi13e">http://goo.gl/uFi13e</a>
 */
public class GZipUtils {

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(data);
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

    public static byte[] decompress(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            out.write(data, 0, bytesRead);
        }
        gis.close();
        is.close();
        return out.toByteArray();
    }

}
