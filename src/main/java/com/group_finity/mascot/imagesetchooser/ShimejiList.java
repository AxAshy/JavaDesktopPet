package com.group_finity.mascot.imagesetchooser;

import java.awt.*;

import javax.swing.*;

/**
 * A {@link JList} that can be populated with {@link ImageSetChooserPanel} objects.
 */
public class ShimejiList extends JList<ImageSetChooserPanel> {

    public ShimejiList() {
        this.setCellRenderer(new CustomCellRenderer<>()); // 渲染列表内的每个单元块
    }

    /**
     * 自定义列表内单元格的渲染方法
     */
    static class CustomCellRenderer<T> implements ListCellRenderer<T> {
        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ImageSetChooserPanel component) {
                component.setCheckbox(isSelected);
                return component;
            } else {
                return new JLabel("???");
            }
        }
        
    }
}