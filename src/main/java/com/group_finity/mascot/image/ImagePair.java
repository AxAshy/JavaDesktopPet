package com.group_finity.mascot.image;

/**
 * 一对向左和向右的 mascot 图像.
 * <p>
 * 主要是因为如果 mascot 图像的向左和向右版本能同时操作会更加方便.
 */
public class ImagePair {

    /**
     * 向左的图片
     */
    private final MascotImage lefImage;

    /**
     * 向右的图片
     */
    private final MascotImage rightImage;

    /**
     * 将两个存在的图片创建为图片对
     * @param leftImage 向左的图片
     * @param rightImage 向右的图片
     */
    public ImagePair(final MascotImage lefImage, final MascotImage righImage) {
        this.lefImage = lefImage;
        this.rightImage = righImage;
    }

    /**
     * 获取面向特定方法的图片
     *
     * @param lookRight 是否获取面向右边的图片
     * @return 面向特定的方向的图片
     */
    public MascotImage getImage(final boolean lookRight) {
        return lookRight ? this.rightImage : this.lefImage;
    }
}
