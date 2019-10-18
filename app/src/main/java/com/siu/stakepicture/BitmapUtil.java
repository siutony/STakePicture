package com.siu.stakepicture;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapUtil {
    /**
     * 旋转一定角度
     *
     * @param bm
     * @param degree
     * @param needMirror
     * @return
     */
    public static Bitmap rotateAndMirrorBitmap(Bitmap bm, int degree, boolean needMirror) {
        Bitmap newBm = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        if (needMirror) {
            matrix.postScale(-1, 1); // 镜像水平翻转
        }
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            newBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                    bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (newBm == null) {
            newBm = bm;
        }
        if (bm != newBm) {
            bm.recycle();
        }
        return newBm;
    }
}
