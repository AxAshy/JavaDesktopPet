package com.group_finity.mascot.environment;

import java.awt.Rectangle;

/**
 * {@code Mascot} 交互环境的区域范围，可以想象成一个正方形
 */
public class Area {
    /**当前区域是否可见 */
    private boolean visible = true;
    /**当前区域的左上角点的x坐标 */
    private int left;
    /**当前区域的左上角点的y坐标 */
    private int top;
    /**当前区域的右下角点的x坐标 */
    private int right;
    /**当前区域的右下角点的y坐标 */
    private int bottom;
    /**当area的尺寸或位置发生变化时，调用set()方法，area的新位置和尺寸，与就位置和尺寸会产生距离，此为左距离 */
    private int dleft;

    private int dtop;

    private int dright;

    private int dbottom;

    // 定义上下左右边界
    private final Wall leftBorder = new Wall(this, false);
    private final Wall rightBorder = new Wall(this, true);
    private final FloorCeiling topBorder = new FloorCeiling(this, false);
    private final FloorCeiling bottomBorder = new FloorCeiling(this, true);

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public int getLeft() {
        return this.left;
    }

    public void setLeft(final int left) {
        this.left = left;
    }

    public int getTop() {
        return this.top;
    }

    public void setTop(final int top) {
        this.top = top;
    }

    public int getRight() {
        return right;
    }

    public void setRight(final int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(final int bottom) {
        this.bottom = bottom;
    }

    public int getDleft() {
        return dleft;
    }

    public void setDleft(final int dleft) {
        this.dleft = dleft;
    }

    public int getDtop() {
        return dtop;
    }

    public void setDtop(final int dtop) {
        this.dtop = dtop;
    }

    public int getDright() {
        return dright;
    }

    public void setDright(final int dright) {
        this.dright = dright;
    }

    public int getDbottom() {
        return dbottom;
    }

    public void setDbottom(final int dbottom) {
        this.dbottom = dbottom;
    }

    public Wall getLeftBorder() {
        return leftBorder;
    }

    public FloorCeiling getTopBorder() {
        return topBorder;
    }

    public Wall getRightBorder() {
        return rightBorder;
    }

    public FloorCeiling getBottomBorder() {
        return bottomBorder;
    }

    public int getWidth() {
        return right - left;
    }

    public int getHeight() {
        return bottom - top;
    }

    /**
     * 根据给定的一个{@code Rectangle}实例创建设置当前 area 的属性
     * @param value 一个{@code Rectangle(int x, int y, int width, int height)}实例
     */
    public void set(final Rectangle value) {
        dleft = value.x - left;
        dtop = value.y - top;
        dright = value.x + value.width - right;
        dbottom = value.y + value.height - bottom;

        left = value.x;
        top = value.y;
        right = value.x + value.width;
        bottom = value.y + value.height;
    }

    /**
     * 返回当前 area 是否包含给定的{@code (x, y)}坐标点
     */
    public boolean contains(final int x, final int y) {
        return left <= x && x <= right && top <= y && y <= bottom;
    }

    /**
     * 以当前 area 的 (left, top) 为左上角起始点，创建一个宽(right - left)，高(bottom - top)的{@code Rectangle}实例
     */
    public Rectangle toRectangle() {
        return new Rectangle(left, top, right - left, bottom - top);
    }

    @Override
    public String toString() {
        return "Area [left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + "]";
    }
}
