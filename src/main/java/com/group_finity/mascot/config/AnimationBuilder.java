package com.group_finity.mascot.config;


import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.image.ImagePairLoader.Filter;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.sound.SoundLoader;

/**
 * {@code AnimationBuilder}在 {@link ActionBuilder} 中被调用, {@code ActionBuilder}用于加载XML文件
 * 中的每个{@code Action}节点, 而{@code AnimationBuilder}用于加载Action节点的{@code Animation}子节点,
 * 它有一串有序的 {@link Pose} 组成, 每个{@code Pose}节点包含一对图片、图片坐标、持续时长、音频
 */
public class AnimationBuilder {
    private static final Logger log = Logger.getLogger(AnimationBuilder.class.getName());
    /**{@code schema}中对应的{@code animationNode}的{@code condition}属性的值, 默认为true */
    private final String condition;
    private String imageSet = "";
    private final List<Pose> poses = new ArrayList<>();
    private final List<Hotspot> hotspots = new ArrayList<>();
    /** xml文件框架 */
    private final ResourceBundle schema;
    /**{@code schema}中对应的{@code animationNode}的{@code IsTurn}属性的值, 默认为false */
    private final String turn;

    public AnimationBuilder(final ResourceBundle schema, final Entry animationNode, final String imageSet) throws ConfigurationException {
        if (!imageSet.isEmpty()) {
            this.imageSet = imageSet;
        }
        this.schema = schema;
        // 获取 animationNode 的 Condition 属性的值, 如无则默认为true
        this.condition = animationNode.getAttribute(schema.getString("Condition")) == null ? "true" : animationNode.getAttribute(schema.getString("Condition"));
        this.turn = animationNode.getAttribute(schema.getString("IsTurn")) == null ? "false" : animationNode.getAttribute(schema.getString("IsTurn"));

        log.log(Level.FINE, "Loading animations");
        
        // 遍历Animation的所有Pose子节点
        for (final Entry frameNode : animationNode.selectChildren(schema.getString("Pose"))) {
            try {
                this.poses.add(this.loadPose(frameNode));
            } catch (IOException e) {
                throw new ConfigurationException(e);
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Failed to load pose: {0}", e);
                throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedLoadPoseErrorMessage") + " " + frameNode.getAttributes().toString(), e);
            }
        }

        // 遍历Animation的所有Hotspot子节点（但是好像基本用不到，不用管）
        for (final Entry frameNode : animationNode.selectChildren(schema.getString("Hotspot"))) {
            try {
                hotspots.add(loadHotspot(frameNode));
            } catch (IOException e) {
                throw new ConfigurationException(e);
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Failed to load hotspot: {0}", e);
                throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedLoadHotspotErrorMessage") + " " + frameNode.getAttributes().toString(), e);
            }
        }

        log.log(Level.FINE, "Finished loading animations");

    }

    
    private Pose loadPose(final Entry frameNode) throws IOException {
        // 向左和向右图片的文件路径
        final Path imagePath = frameNode.getAttribute(schema.getString("Image")) != null ? Path.of(this.imageSet, frameNode.getAttribute(schema.getString("Image"))) : null;
        final Path imageRightPath = frameNode.getAttribute(schema.getString("ImageRight")) != null ? Path.of(imageSet, frameNode.getAttribute(schema.getString("ImageRight"))) : null;
        // 当前Pose的坐标, 一个x,y格式的字符串坐标
        final String anchorText = frameNode.getAttribute(schema.getString("ImageAnchor"));
        // 播放速度
        final String moveText = frameNode.getAttribute(schema.getString("Velocity"));
        // 持续时间
        final String durationText = frameNode.getAttribute(schema.getString("Duration"));
        // 音频文件
        String soundText = frameNode.getAttribute(schema.getString("Sound"));
        // 音频音量
        final String volumeText = frameNode.getAttribute(schema.getString("Volume")) != null ? frameNode.getAttribute(schema.getString("Volume")) : "0";

        // 不透明度
        final double opacity = Double.parseDouble(Main.getInstance().getProperties().getProperty("Opacity", "1.0"));
        // 缩放率
        final double scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        // 选择图片缩放时的插值方式, 默认为NEAREST_NEIGHBOUR, 如果配置文件中有设置Filter, 就是用设置的插值方式
        String filterText = Main.getInstance().getProperties().getProperty("Filter", "false");
        Filter filter = Filter.NEAREST_NEIGHBOUR;
        if (filterText.equalsIgnoreCase("true") || filterText.equalsIgnoreCase("hqx")) {
            filter = ImagePairLoader.Filter.HQX;
        } else if (filterText.equalsIgnoreCase("bicubic")) {
            filter = ImagePairLoader.Filter.BICUBIC;
        }

        if (imagePath != null) {
            // 处理图片坐标
            final String[] anchorCoordinates = anchorText.split(",");
            final Point anchor = new Point(Integer.parseInt(anchorCoordinates[0]), Integer.parseInt(anchorCoordinates[1]));

            // 加载图片
            try {
                ImagePairLoader.load(imagePath, imageRightPath, anchor, scaling, filter, opacity);
            } catch (IOException | NumberFormatException e) {
                // log加载失败的图片文件
                String error = imagePath.toString();
                if (imageRightPath != null) {
                    error += ", " + imageRightPath;
                }
                log.log(Level.SEVERE, "Failed to load image" + (imageRightPath != null ? "s" : "") + ": " + error, e);
                throw new IOException(Main.getInstance().getLanguageBundle().getString("FailedLoadImageErrorMessage") + " " + error, e);
            }
        }

        // 处理移动坐标
        final String[] moveCoordinates = moveText.split(",");
        int moveX = Integer.parseInt(moveCoordinates[0]);
        int moveY = Integer.parseInt(moveCoordinates[1]);
        moveX = Math.abs(moveX) > 0 && Math.abs(moveX * scaling) < 1 ? moveX > 0 ? 1 : -1 : (int) Math.round(moveX * scaling);
        moveY = Math.abs(moveY) > 0 && Math.abs(moveY * scaling) < 1 ? moveY > 0 ? 1 : -1 : (int) Math.round(moveY * scaling);

        // 处理持续时长
        final int duration = Integer.parseInt(durationText);

        // 处理声音文件(不过基本可以用不上)
        if (soundText != null) {
            try {
                Path soundPath;
                if (Files.exists(Main.SOUND_DIRECTORY.resolve(soundText))) {
                    soundPath = Main.SOUND_DIRECTORY.resolve(soundText);
                } else if (Files.exists(Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText))) {
                    soundPath = Main.SOUND_DIRECTORY.resolve(imageSet).resolve(soundText);
                } else {
                    soundPath = Main.IMAGE_DIRECTORY.resolve(imageSet).resolve(Main.SOUND_DIRECTORY).resolve(soundText);
                }
                soundText = soundPath.toString();

                float volume = Float.parseFloat(volumeText);
                SoundLoader.load(soundText, volume);
                soundText += volume;
            } catch (IOException | NumberFormatException | LineUnavailableException | UnsupportedAudioFileException e) {
                log.log(Level.SEVERE, "Failed to load sound: " + soundText, e);
                throw new IOException(Main.getInstance().getLanguageBundle().getString("FailedLoadSoundErrorMessage") + soundText, e);
            }
        }

        final Pose pose = new Pose(imagePath, imageRightPath, moveX, moveY, duration, soundText);
        log.log(Level.FINE, "Finished loading pose: {0}", pose);
        return pose;
    }

    private Hotspot loadHotspot(final Entry frameNode) throws IOException {
        final String shapeText = frameNode.getAttribute(schema.getString("Shape"));
        final String originText = frameNode.getAttribute(schema.getString("Origin"));
        final String sizeText = frameNode.getAttribute(schema.getString("Size"));
        final String behaviourText = frameNode.getAttribute(schema.getString("Behaviour"));
        final double scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        final String[] originCoordinates = originText.split(",");
        final String[] sizeCoordinates = sizeText.split(",");

        final Point origin = new Point((int) Math.round(Integer.parseInt(originCoordinates[0]) * scaling),
                (int) Math.round(Integer.parseInt(originCoordinates[1]) * scaling));
        final Dimension size = new Dimension((int) Math.round(Integer.parseInt(sizeCoordinates[0]) * scaling),
                (int) Math.round(Integer.parseInt(sizeCoordinates[1]) * scaling));

        Shape shape;
        if (shapeText.equalsIgnoreCase("Rectangle")) {
            shape = new Rectangle(origin, size);
        } else if (shapeText.equalsIgnoreCase("Ellipse")) {
            shape = new Ellipse2D.Float(origin.x, origin.y, size.width, size.height);
        } else {
            log.log(Level.SEVERE, "Failed to load hotspot shape: {0}", shapeText);
            throw new IOException(Main.getInstance().getLanguageBundle().getString("HotspotShapeNotSupportedErrorMessage") + " " + shapeText);
        }

        final Hotspot hotspot = new Hotspot(behaviourText, shape);

        log.log(Level.FINE, "Finished loading hotspot: {0}", hotspot);

        return hotspot;
    }

    public Animation buildAnimation() throws AnimationInstantiationException {
        try {
            return new Animation(Variable.parse(condition), poses.toArray(new Pose[0]), hotspots.toArray(new Hotspot[0]), Boolean.parseBoolean(turn));
        } catch (final VariableException e) {
            throw new AnimationInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }
    }
}
