package com.topstep.wearkit.sample.ui.camera;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class YuvUtil {

    public static byte[] extractPlaneData(ImageProxy.PlaneProxy plane, int width, int height) {
        ByteBuffer buffer = plane.getBuffer();
        int stride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        byte[] data = new byte[width * height * pixelStride];

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width * pixelStride; x += pixelStride) {
                data[offset++] = buffer.get(y * stride + x);
            }
        }
        return data;
    }

    public static byte[] extractUVPlaneData(ImageProxy.PlaneProxy plane, int width, int height) {
        ByteBuffer buffer = plane.getBuffer();

        int stride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        byte[] data = new byte[width * height * pixelStride];

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width * pixelStride; x += pixelStride) {
                data[offset] = buffer.get(y * stride + x);
                int v_pos = y * stride + x + 1;
                data[offset + 1] = buffer.get((v_pos >= buffer.limit() ? buffer.limit() - 1 : v_pos));
                offset += pixelStride;
            }
        }
        return data;
    }

}
