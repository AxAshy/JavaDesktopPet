package com.group_finity.mascot.environment;

import java.awt.Point;

import com.group_finity.mascot.Mascot;

/**
 * 展示一个可移动的平面给 {@link Mascot Mascots} 进行交互.
 * 是一个实现了移动和判定是否在区域内功能的接口。
 */
public interface Border {
    /**
     * 返回给定的坐标点 {@link Point} 是否在当前 border 上.
     * <p>返回True需满足：
     *  <p>针对{@code Wall}：area 可见 && area.x == location.x && area.top <= location.y <= area.bottom</p>
     *  <p>针对{@code FloorCeiling}：area 可见 && area.y == location.y && area.left <= location.x <= area.right</p>
     * </p>
     * @param location 需要检查的坐标点 {@link Point}
     * @return 给定的坐标点 {@link Point} 是否在当前 border 上
     */
    boolean isOn(Point location);
    
    /**
     * 移动给定的坐标点 {@link Point} 以匹配当前 border 位置和尺寸的变化
     * <p>
     * 例如, 如果当前 border 向右移动了 X 个单位, 然后 {@code location} 也将向右移动 X 个单位。
     * 如果当前 border 是 {@linkplain FloorCeiling floor 或 ceiling} 并且被缩放至之前窗口的一半宽，
     * 那么 {@code location} 将会被更新为之前 border 做边界的一半w
     *
     * @param location 需要移动的坐标点 {@link Point}
     * @return 移动完成后的坐标点 {@link Point}
     */
    Point move(Point location);
}
