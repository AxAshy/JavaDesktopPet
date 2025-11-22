package com.group_finity.mascot.config;


import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.ConfigurationException;

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
        
        // 遍历框架中的Pose节点
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


    }

    
    private Pose loadPose(final Entry frameNode) throws IOException {
        final Path imagePath = frameNode.getAttribute(schema.getString("Image")) != null ? Path.of(this.imageSet, frameNode.getAttribute(schema.getString("Image"))) : null;
        final Path imageRightPath = frameNode.getAttribute(schema.getString("ImageRight")) != null ? Path.of(imageSet, frameNode.getAttribute(schema.getString("ImageRight"))) : null;
        final String anchorText = frameNode.getAttribute(schema.getString("ImageAnchor"));
        final String moveText = frameNode.getAttribute(schema.getString("Velocity"));
        final String durationText = frameNode.getAttribute(schema.getString("Duration"));
        String soundText = frameNode.getAttribute(schema.getString("Sound"));
        final String volumeText = frameNode.getAttribute(schema.getString("Volume")) != null ? frameNode.getAttribute(schema.getString("Volume")) : "0";

        final double opacity = Double.parseDouble(Main.getInstance().getProperties().getProperty("Opacity", "1.0"));
        final double scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        String filterText = Main.getInstance().getProperties().getProperty("Filter", "false");


        return pose
    }
}
