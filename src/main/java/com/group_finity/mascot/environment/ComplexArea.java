package com.group_finity.mascot.environment;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 复杂区域，有多个{@link Area Area}组合而成
 */
public class ComplexArea {

    private final Map<String, Area> areas = new HashMap<>();

    // 通过overload实现处理多个rectangles的哈希表，或单个name-rectangle对
    public void set(Map<String, Rectangle> rectangles) {
        this.retain(rectangles.keySet()); // 从areas中删除key不在rectangs的keySet中的数据
        for (Map.Entry<String, Rectangle> e : rectangles.entrySet()) {
            this.set(e.getKey(), e.getValue());
        }
    }

    /**
     * 将{@code this.areas}中的 key 为给定{@code name}的 area 调用其set()方法，将属性设为给定的{@code value}
     * @param name
     * @param value
     */
    public void set(String name, final Rectangle value) {
        // 如果areas里已经有area和当前给定的Rectangle匹配了，就不处理，直接返回
        for (Area area : this.areas.values()) {
            if (area.getLeft() == value.x &&
                    area.getTop() == value.y &&
                    area.getWidth() == value.width &&
                    area.getHeight() == value.height) {
                return;
            }
        }

        Area area = this.areas.get(name);
        // 如果不存在就创建新的area
        if (area == null) {
            area = new Area();
            areas.put(name, area);
        }
        area.set(value);
    }

    /**
     * 从{@code areas}中删除 key 不在给定的{@code deviceNames}中的数据
     * @param deviceNames 给定的设备名称数组
     */
    public void retain(Collection<String> deviceNames) {
        areas.keySet().removeIf(key -> !deviceNames.contains(key));
    }

    /**
     * 给定一个{@code Point}，返回下边界和其重合的area的下边界area.BottomBorder
     * @param location
     * @return ret 返回下边界和其重合的area的下边界area.BottomBorder
     */
    public FloorCeiling getBottomBorder(Point location) {
        FloorCeiling ret = null;
        for (Area area : this.areas.values()) {
            if (area.getBottomBorder().isOn(location)) {
                ret = area.getBottomBorder();
            }
        }

        for (Area area : areas.values()) {
            if (area.getTopBorder().isOn(location)) {
                ret = null;
            }
        }

        return ret;
    }

    public FloorCeiling getTopBorder(Point location) {
        FloorCeiling ret = null;

        for (Area area : areas.values()) {
            if (area.getTopBorder().isOn(location)) {
                ret = area.getTopBorder();
            }
        }

        for (Area area : areas.values()) {
            if (area.getBottomBorder().isOn(location)) {
                ret = null;
            }
        }

        return ret;
    }

    public Wall getLeftBorder(Point location) {
        Wall ret = null;

        for (Area area : areas.values()) {
            if (area.getLeftBorder().isOn(location)) {
                ret = area.getRightBorder();
            }
        }
        for (Area area : areas.values()) {
            if (area.getRightBorder().isOn(location)) {
                ret = null;
            }
        }
        return ret;
    }

    public Wall getRightBorder(Point location) {
        Wall ret = null;

        for (Area area : areas.values()) {
            if (area.getRightBorder().isOn(location)) {
                ret = area.getRightBorder();
            }
        }
        for (Area area : areas.values()) {
            if (area.getLeftBorder().isOn(location)) {
                ret = null;
            }
        }
        return ret;
    }

    public Collection<Area> getAreas() {
        return this.areas.values();
    }
}