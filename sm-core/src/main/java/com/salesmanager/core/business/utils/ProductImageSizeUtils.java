package com.salesmanager.core.business.utils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import static java.awt.AlphaComposite.Src;
import static java.awt.RenderingHints.*;
import static java.awt.Transparency.OPAQUE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.round;

public class ProductImageSizeUtils {


    private ProductImageSizeUtils() {

    }


    /**
     * Simple resize, does not maintain aspect ratio
     *
     * @param image
     * @param width
     * @param height
     * @return
     */

    public static BufferedImage resize(BufferedImage image, int width, int height) {
        int type = image.getType() == 0 ? TYPE_INT_ARGB : image
                .getType();
        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.setComposite(Src);
        g.setRenderingHint(KEY_INTERPOLATION,
                VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(KEY_RENDERING,
                VALUE_RENDER_QUALITY);
        g.setRenderingHint(KEY_ANTIALIASING,
                VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    /**
     * @param img
     * @param targetWidth
     * @param targetHeight
     * @param hint          {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *                      {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *                      {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality
     * @return
     */
    public static BufferedImage resizeWithHint(BufferedImage img,
                                               int targetWidth, int targetHeight, Object hint,
                                               boolean higherQuality) {
        int type = (img.getTransparency() == OPAQUE) ? TYPE_INT_RGB
                : TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }


    public static BufferedImage resizeWithRatio(BufferedImage image, int destinationWidth, int destinationHeight) {

        int type = image.getType() == 0 ? TYPE_INT_ARGB : image.getType();

        //*Special* if the width or height is 0 use image src dimensions
        if (destinationWidth == 0) {
            destinationWidth = image.getWidth();
        }
        if (destinationHeight == 0) {
            destinationHeight = image.getHeight();
        }

        int fHeight = destinationHeight;
        int fWidth = destinationWidth;

        //Work out the resized width/height
        if (image.getHeight() > destinationHeight || image.getWidth() > destinationWidth) {
            fHeight = destinationHeight;
            int wid = destinationWidth;
            float sum = (float) image.getWidth() / (float) image.getHeight();
            fWidth = round(fHeight * sum);

            if (fWidth > wid) {
                //rezise again for the width this time
                fHeight = round(wid / sum);
                fWidth = wid;
            }
        }

        BufferedImage resizedImage = new BufferedImage(fWidth, fHeight, type);
        Graphics2D g = resizedImage.createGraphics();
        g.setComposite(Src);

        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        g.drawImage(image, 0, 0, fWidth, fHeight, null);
        g.dispose();

        return resizedImage;
    }


}
