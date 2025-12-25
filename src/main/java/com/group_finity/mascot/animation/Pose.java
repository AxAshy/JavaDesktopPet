package com.group_finity.mascot.animation;

import java.awt.*;
import java.nio.file.Path;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.config.AnimationBuilder;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairs;

/**
 * {@code Pose}在 {@link AnimationBuilder} 中被调用, {@code ActionBuilder}用于加载XML文件
 * 中的每个{@code Action}节点, 而{@code AnimationBuilder}用于加载Action节点的{@code Animation}子节点,
 * 它有一串有序的 {@link Pose} 组成, 每个{@code Pose}节点包含一对图片、图片坐标、持续时长、音频，
 * 如：Pose Image="/shime1.png" ImageAnchor="64,128" Velocity="0,0" Duration="250"
 */
public class Pose {
    private final Path image; // 图片路径
    private final Path rightImage;
    private final int dx; 
    private final int dy;
    private final int duration; // 姿势持续的时间
    private final String sound;

    public Pose(final Path image) {
        this(image, null, 0, 0, 1);
    }

    public Pose(final Path image, final int duration) {
        this(image, null, 0, 0, duration);
    }

    public Pose(final Path image, final int dx, final int dy, final int duration) {
        this(image, null, dx, dy, duration);
    }

    public Pose(final Path image, final Path rightImage) {
        this(image, rightImage, 0, 0, 1);
    }

    public Pose(final Path image, final Path rightImage, final int duration) {
        this(image, rightImage, 0, 0, duration);
    }

    public Pose(final Path image, final Path rightImage, final int dx, final int dy, final int duration) {
        this(image, rightImage, dx, dy, duration, null);
    }

    public Pose(final Path image, final Path rightImage, final int dx, final int dy, final int duration, final String sound) {
        this.image = image;
        this.rightImage = rightImage;
        this.dx = dx;
        this.dy = dy;
        this.duration = duration;
        this.sound = sound;
    }

    @Override
    public String toString() {
        return "Pose(" + (this.getImage() == null ? "" : this.getImage()) + "," + dx + "," + dy + "," + duration + ", " + sound + ")";
    }

    /**
     * 播放绑定的 mascot 的当前 pose 的下一帧图片
     * @param mascot
     */
    public void next(final Mascot mascot) {
        mascot.setAnchor(new Point(mascot.getAnchor().x + (mascot.isLookRight() ? -dx : dx),
                mascot.getAnchor().y + dy));
        mascot.setImage(ImagePairs.getImage(this.getImageName(), mascot.isLookRight()));
        mascot.setSound(sound);
    }

    /**
     * 返回当前 pose 的 图片路径文本串
     * @return String 图片路径文本串
     */
    public String getImageName() {
        return (image == null ? "" : image.toString()) + (rightImage == null ? "" : rightImage.toString());
    }

    public ImagePair getImage() {
        return ImagePairs.getImagePair(this.getImageName());
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDuration() {
        return duration;
    }

    public String getSoundName() {
        return sound;
    }
}
