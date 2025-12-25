package com.group_finity.mascot.image;

import javax.imageio.ImageIO;

import com.group_finity.mascot.Main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import hqx.Hqx_2x;
import hqx.Hqx_3x;
import hqx.Hqx_4x;

/**
 * 加载图片对imagePairs
 */
public class ImagePairLoader {
    /**
     * 缩放图片的方式, 可选:
     * <ul><li>NEAREST_NEIGHBOUR -最邻近插值法</li>
     * <li>HQX -(“hq”代表“高清”、“x”代表放大倍率）是一种由Maxim Stepin开发的像素放大算法</li>
     * <li>BICUBIC -双三次插值</li></ul>
     */
    public enum Filter {NEAREST_NEIGHBOUR, HQX, BICUBIC}

    /**
     * 加载一个图片对{@code imagePair}
     * @param path 需要加载的面向左边的图片的文件路径
     * @param rightPath 需要加载的面向右边的图片的文件路径
     * @param center 图片中心的坐标
     * @param scaling 比例因子, 图片大小缩放的比例
     * @param filter 生成(缩放)图片的{@code filter}类型
     * @param opacity 不透明度
     * @throws IOException
     */
    public static void load(final Path path, final Path rightPath, final Point center, final double scaling, final Filter filter, final double opacity) throws IOException{
        String key = path.toString() + (rightPath == null ? "" : rightPath);
        // 如果图片已经加载过了, 就不需要再加载一遍了
        if (ImagePairs.contains(key)) {
            return;
        }

        final BufferedImage leftImage = scale(premultiply(ImageIO.read(Files.newInputStream(Main.IMAGE_DIRECTORY.resolve(path))), opacity), scaling, filter);
        final BufferedImage rightImage;
        // 如果向右图路径为空,就直接使用向左图水平翻转后的图片
        if (rightPath == null) {
            rightImage = flip(leftImage);
        } else {
            rightImage = scale(premultiply(ImageIO.read(Files.newInputStream(Main.IMAGE_DIRECTORY.resolve(rightPath))), opacity), scaling, filter);
        }

        ImagePair ip = new ImagePair(new MascotImage(leftImage, new Point((int) Math.round(center.x * scaling), (int) Math.round(center.y * scaling))),
                new MascotImage(rightImage, new Point(rightImage.getWidth() - (int) Math.round(center.x * scaling), (int) Math.round(center.y * scaling))));
        ImagePairs.put(key, ip);
    }

    /**
     * 水平翻转图片
     * @param src 需要水平反转的图片
     * @return 水平放置后的图片
     */
    private static BufferedImage flip(final BufferedImage src) {
        final BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(),
                src.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : src.getType());

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                copy.setRGB(copy.getWidth() - x - 1, y, src.getRGB(x, y));
            }
        }
        return copy;
    }

    private static BufferedImage premultiply(final BufferedImage source, final double opacity) {
        final BufferedImage returnImage = new BufferedImage(source.getWidth(), source.getHeight(),
                source.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB_PRE : source.getType());
        Color colour;
        float[] components;

        for (int y = 0; y < returnImage.getHeight(); y++) {
            for (int x = 0; x < returnImage.getWidth(); x++) {
                colour = new Color(source.getRGB(x, y), true);
                components = colour.getComponents(null);
                components[3] *= (float) opacity;
                components[0] = components[3] * components[0];
                components[1] = components[3] * components[1];
                components[2] = components[3] * components[2];
                colour = new Color(components[0], components[1], components[2], components[3]);
                returnImage.setRGB(x, y, colour.getRGB());
            }
        }

        return returnImage;
    }

    /**
     * 缩放图片
     * @param source 原图片数据
     * @param scaling 比例因子, 图片大小缩放的比例
     * @param filter 生成(缩放)图片的{@code filter}类型
     * @return 缩放后的图片
     */
    private static BufferedImage scale(final BufferedImage source, final double scaling, Filter filter) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage workingImage = null;

        // 如果可以使用的话, 使用hqx
        double effectiveScaling = scaling;
        if (filter == Filter.HQX && scaling > 1) {
            int[] buffer;
            int[] rbgValues = source.getRGB(0, 0, width, height, null, 0, width);

            if (scaling == 4 || scaling == 8) {
                width *= 4;
                height *= 4;
                buffer = new int[width * height];
                Hqx_4x.hq4x_32_rb(rbgValues, buffer, width / 4, height / 4);
                rbgValues = buffer;
                effectiveScaling = scaling > 4 ? 2 : 1;
            } else if (scaling == 3 || scaling == 6) {
                width *= 3;
                height *= 3;
                buffer = new int[width * height];
                Hqx_3x.hq3x_32_rb(rbgValues, buffer, width / 3, height / 3);
                rbgValues = buffer;
                effectiveScaling = scaling > 4 ? 2 : 1;
            } else if (scaling == 2) {
                width *= 2;
                height *= 2;
                buffer = new int[width * height];
                Hqx_2x.hq2x_32_rb(rbgValues, buffer, width / 2, height / 2);
                rbgValues = buffer;
                effectiveScaling = 1;
            } else {
                filter = Filter.NEAREST_NEIGHBOUR;
            }

            // if hqx is still on then apply the changes
            if (filter == Filter.HQX) {
                workingImage = new BufferedImage((int) Math.round(width * effectiveScaling), (int) Math.round(height * effectiveScaling), BufferedImage.TYPE_INT_ARGB_PRE);
                int srcColIndex = 0;
                int srcRowIndex = 0;

                for (int y = 0; y < workingImage.getHeight(); y++) {
                    for (int x = 0; x < workingImage.getWidth(); x++) {
                        workingImage.setRGB(x, y, rbgValues[srcColIndex / (int) effectiveScaling]);
                        srcColIndex++;
                    }

                    // resets the srcColIndex to re-use the same indexes and stretch horizontally
                    srcRowIndex++;
                    if (srcRowIndex == effectiveScaling) {
                        srcRowIndex = 0;
                    } else {
                        srcColIndex -= workingImage.getWidth();
                    }
                }
            }
        }

        width = (int) Math.round(width * effectiveScaling);
        height = (int) Math.round(height * effectiveScaling);

        final BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);

        Graphics2D g2d = copy.createGraphics();
        Object renderHint = filter == Filter.BICUBIC
                ? RenderingHints.VALUE_INTERPOLATION_BICUBIC
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, renderHint);
        g2d.drawImage(workingImage != null ? workingImage : source, 0, 0, width, height, null);

        g2d.dispose();

        return copy;
    }    
}
