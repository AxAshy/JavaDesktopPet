package com.group_finity.mascot.environment;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.group_finity.mascot.Manager;

/**
 * {@code Environment} 抽象类
 */
public abstract class Environment {
    protected abstract Area getWorkArea();

    public abstract Area getActiveIE();

    public abstract String getActiveIETitle();

    public abstract long getActiveWindowId();

    public abstract void moveActiveIE(final Point point);

    public abstract void restoreIE();

    public abstract void refreshCache();

    public abstract void dispose();

    /**
     * 屏幕方框，初始化为一个左上角为(0, 0)，以当前屏幕尺寸为维度的长方形
     */
    protected static Rectangle screenRect = new Rectangle(new Point(0, 0), Toolkit.getDefaultToolkit().getScreenSize());

    protected static Map<String, Rectangle> screenRects = new HashMap<>();

    /**
     * 一个专门用于更新窗口框的线程
     */
    private static final Thread thread = new Thread(() -> {
        try {
            while (true) {
                updateScreenRect();
                Thread.sleep(5000);
            }
        } catch (final InterruptedException ignored) {
        }
    }, "ScreenRectUpdater");

    public ComplexArea complexScreen = new ComplexArea();

    /**
     * 当前显示屏幕
     */
    public Area screen = new Area();

    /**
     * 鼠标指针
     */
    public Location cursor = new Location();

    public static void updateScreenRect() {
        Rectangle virtualBounds = new Rectangle();

        Map<String, Rectangle> screenRects = new HashMap<>();

        /**
         * GraphicsEnvironment 类描述了 Java(tm) 应用程序在特定平台上可用的 GraphicsDevice 对象和 Font 对象的集合。
         * 此 GraphicsEnvironment 中的资源可以是本地资源，也可以位于远程机器上。
         */
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        /**
         * GraphicsDevice 对象可以是屏幕、打印机或图像缓冲区，并且都是 Graphics2D 绘图方法的目标。
         * 每个 GraphicsDevice 都有许多与之相关的 GraphicsConfiguration 对象。这些对象指定了使用 GraphicsDevice 所需的不同配置。
         */
        final GraphicsDevice[] gs = ge.getScreenDevices();

        for (final GraphicsDevice gd : gs) {
            /**
             * GraphicsConfiguration 类描述图形目标（如打印机或监视器）的特征。使用getBounds()来获取设备的边界
             */
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            screenRects.put(gd.getIDstring(), bounds);
            virtualBounds = virtualBounds.union(bounds);
        }

        Environment.screenRects = screenRects;

        screenRect = virtualBounds;
    }

    /**
     * 获取当前屏幕的区域范围，这个范围包括了展示区域的左上角到右下角。
     * @return screen area
     */
    protected static Rectangle getScreenRect() {
        return screenRect;
    }

    /**
     * 获取鼠标指针的坐标
     * @return cursor coordinates
     */
    private static Point getCursorPos() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info != null ? info.getLocation() : new Point(0, 0);
    }

    /**
     * 更新当前{@code environment}的 screen、complexScreen、cursor。每 40 毫秒由 {@link Manager} 触发.
     */
    public void tick() {
        screen.set(screenRect);
        complexScreen.set(screenRects);
        cursor.set(getCursorPos());
    }

    /**
     * 当{@code environment}在创建时自动调用：①初始化用于刷新窗口框的守护线程；②调用tick方法，每40毫秒刷新一次窗口
     */
    public void init() {
        // 启动线程，并将线程设为守护线程
        if (!thread.isAlive()) {
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        this.tick();
    }

    public Area getScreen() {
        return screen;
    }

    public Collection<Area> getScreens() {
        return complexScreen.getAreas();
    }

    public ComplexArea getComplexScreen() {
        return complexScreen;
    }

    public Location getCursor() {
        return cursor;
    }

    /**
     * 判定给定{@code location}是否位于当前{@code environment}的上下边界
     * @param location
     * @return boolean
     */
    public boolean isScreenTopBottom(final Point location) {
        int count = 0;

        for (Area area : getScreens()) {
            if (area.getTopBorder().isOn(location)) {
                count++;
            }
            if (area.getBottomBorder().isOn(location)) {
                count++;
            }
        }

        if (count == 0) {
            if (getWorkArea().getTopBorder().isOn(location)) {
                return true;
            }
            if (getWorkArea().getBottomBorder().isOn(location)) {
                return true;
            }
        }

        return count == 1;
    }

    /**
     * 判定给定{@code location}是否位于当前{@code environment}的左右边界
     * @param location
     * @return boolean
     */
    public boolean isScreenLeftRight(final Point location) {
        int count = 0;

        for (Area area : getScreens()) {
            if (area.getLeftBorder().isOn(location)) {
                count++;
            }
            if (area.getRightBorder().isOn(location)) {
                count++;
            }
        }

        if (count == 0) {
            if (getWorkArea().getLeftBorder().isOn(location)) {
                return true;
            }
            if (getWorkArea().getRightBorder().isOn(location)) {
                return true;
            }
        }

        return count == 1;
    }
}
