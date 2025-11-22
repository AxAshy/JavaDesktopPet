package com.group_finity.mascot.animation;

import com.group_finity.mascot.Mascot;
import java.awt.*;
/**
 * <p>
 * 代表了一个{@code Mascot}上可点击的区域, 以及当用户的这个区域交互时可执行的{@code Behavior}
 */
public class Hotspot {
    private final String behaviour;

    private final Shape shape;

    public Hotspot(String behaviour, Shape shape) {
        this.behaviour = behaviour;
        this.shape = shape;
    }

    public boolean contains(Mascot mascot, Point point) {
        // 如果面向右边，就翻转过来
        if (mascot.isLookRight()) {
            point = new Point(mascot.getBounds().width - point.x, point.y);
        }

        return shape.contains(point);
    }

    public String getBehaviour() {
        return behaviour;
    }

    public Shape getShape() {
        return shape;
    }
}
