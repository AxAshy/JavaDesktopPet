package com.group_finity.mascot;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 宠物类：Mascot object.
 * <p>
 * 宠物的移动使用 {@link Behavior}（表示长期和复杂的行为）,
 * 以及 {@link Action}（表示瞬时和单调简单的行为）.
 * <p>
 * 宠物有一个内部的计时器，并会按规律的时间间隔唤醒 {@link Action} 。
 * {@link Action} 会通过唤醒 {@link Animation} 来让宠物动起来.
 * <p>
 * 当 {@link Action} 结束或者在某些特定的时机唤醒了 {@link Behavior} ，就会执行下一个 {@link Action}.
 */
public class Mascot {
    /**
     * 是否要画出 mascots 的边界和热点, 用于debug.
     */
    public static final boolean DRAW_DEBUG = false;

    private static final Logger log = Logger.getLogger(Mascot.class.getName());

    /**
     * 上一个生成的  {@code Mascot} 的ID。
     */
    private static final AtomicInteger lastId = new AtomicInteger();

    /**
     * 当前 {@code Mascot} 的ID。单纯是为了查看debug日志时更容易找到对象。
     */
    private final int id;

    private String imageSet;

    /**
     * Mascot 展示的环境.
     */
    private final MascotEnvironment environment = new MascotEnvironment(this);
    
    /**
     * 展示 {@code Mascot} 的透明窗口.
     */
    private final TranslucentWindow window = NativeFactory.getInstance().newTranslucentWindow();

    /**
     * 管理此 {@code Mascot} 的 {@link Manager}.
     */
    private Manager manager = null;

    /**
     * 此 {@code Mascot} 的地面坐标，即当手或角悬浮时，以地面坐标为准.
     */
    private Point anchor = new Point(0, 0);

    /**
     * 展示的图片.
     */
    private MascotImage image = null;

    /**
     * 此 {@code Mascot} 是否正面向右边. 初始图像时面向左边的，因此设置此参数为{@code true}会造成相反的效果.
     */
    private boolean lookRight = false;

    /**
     * 一个用于代表此{@code Mascot}的长期行为的对象.
     */
    private Behavior behavior = null;

    /**
     * <p>计时器每tick一下，Time就会增加.</p>
     *
     * <p>
     * 技术上来说这个操作可能造成{@code overflow}, 当用户保持程序运行以下时间后就会发生：
     * </p>
     *
     * <pre>
     *     Max Integer Value: 2,147,483,647
     *     FPS: 60
     *
     *     2,147,483,647 / 60 = ~35,791,394.1 seconds
     *     ~35,791,394.1 / 60 = ~596,523.2 minutes
     *     ~596,523.2 / 60 = ~9,942.0 hours
     *     ~9,942.0 / 24 = ~414.2 days
     * </pre>
     */
    private int time = 0;

    /**
     * 动画是否在运行
     */
    private boolean animating = true;

    private boolean paused = false;

    /**
     * 当这个{@code Mascot}被鼠标拖拽时，相关的behaviours会设置这个属性。
     */
    private boolean dragging = false;

    private String sound = null;

    protected DebugWindow debugWindow = null;

    private final List<String> affordances = new ArrayList<>(5);

    private final List<Hotspot> hotspots = new ArrayList<>(5);

    /**
     * 当用户触发这个{@code Mascot}上的{@code hotspot}时，由{@code behaviours}设置，
     * 因此这个{@code Mascot}就知道去检查鼠标被长按时出现的新的{@code hotspot}？
     */
    private Point cursor = null;

    private VariableMap variables = null;

    public Mascot(final String imageSet) {
        id = this.lastId.incrementAndGet(); // ID为上一个实例的ID+1
        this.imageSet = imageSet;

        log.log(Level.INFO, "Created mascot \"{0}\" with image set \"{1}\"", new Object[]{this, imageSet});

        // 初始化的Mascot新实例总是展示在顶层
        this.window.setAlwaysOnTop(true);

        // 注册鼠标的控制器，用于监控鼠标的按下和放开
        this.window.asComponent().addMouaddMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                Mascot.this.mousePressed(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                Mascot.this.mouseReleased(e);
            }
        });
        this.window.asComponent().addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                if (paused) {
                    refreshCursor(false);
                } else {
                    if (isHotspotClicked()) {
                        setCursorPosition(e.getPoint());
                    } else {
                        refreshCursor(e.getPoint());
                    }
                }
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (paused) {
                    refreshCursor(false);
                } else {
                    if (isHotspotClicked()) {
                        setCursorPosition(e.getPoint());
                    } else {
                        refreshCursor(e.getPoint());
                    }
                }
            }
        });

        if (DRAW_DEBUG) {
            // For drawing the outlines of hotspots and the mascot's bounds, for debugging purposes
            JComponent debugComp = new JComponent() {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    // Draw hotspots
                    g.setColor(Color.BLUE);
                    Dimension imageSize = getImage().getSize();
                    for (Hotspot hotspot : getHotspots()) {
                        Shape shape = hotspot.getShape();
                        if (shape instanceof Rectangle) {
                            Rectangle rectangle = (Rectangle) shape;
                            int x = lookRight ? imageSize.width - rectangle.x - rectangle.width : rectangle.x;
                            g.drawRect(x, rectangle.y, rectangle.width, rectangle.height);
                        } else if (shape instanceof Ellipse2D) {
                            Ellipse2D ellipse = (Ellipse2D) shape;
                            double x = lookRight ? imageSize.width - ellipse.getX() - ellipse.getWidth() : ellipse.getX();
                            g.drawOval((int) x, (int) ellipse.getY(), (int) ellipse.getWidth(), (int) ellipse.getHeight());
                        }
                    }

                    // Draw bounds
                    g.setColor(Color.RED);
                    Rectangle bounds = getBounds();
                    g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);

                    // Draw image anchor
                    g.setColor(Color.GREEN);
                    Point imageAnchor = getImage().getCenter();
                    // Because the image anchor is a single point, it is drawn as a circle and several lines for visibility
                    g.drawOval(imageAnchor.x - 5, imageAnchor.y - 5, 10, 10);
                    g.drawLine(imageAnchor.x - 10, imageAnchor.y, imageAnchor.x + 10, imageAnchor.y);
                    g.drawLine(imageAnchor.x, imageAnchor.y - 10, imageAnchor.x, imageAnchor.y + 10);
                    g.drawLine(imageAnchor.x - 10, imageAnchor.y - 10, imageAnchor.x + 10, imageAnchor.y + 10);
                    g.drawLine(imageAnchor.x - 10, imageAnchor.y + 10, imageAnchor.x + 10, imageAnchor.y - 10);
                }
            };
            debugComp.setBackground(new Color(0, 0, 0, 0));
            debugComp.setOpaque(false);
            debugComp.setPreferredSize(window.asComponent().getPreferredSize());
            window.asComponent().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    debugComp.setPreferredSize(e.getComponent().getPreferredSize());
                }
            });
            ((Container) window.asComponent()).add(debugComp);
        }
    }

    @Override
    public String toString() {
        return "mascot" + id;
    }

    /**
     * popup menu在不同系统中的触发条件并不相同。因此，为了正确实现跨平台功能，必须同时监听mousePressed（鼠标按下）和mouseReleased（鼠标释放）两个动作，以此来确保能够检测到isPopupTrigger
     * @param event
     */
    private void mousePressed(final MouseEvent event) {
        // Check for popup triggers in both mousePressed and mouseReleased
        // because it works differently on different systems
        if (event.isPopupTrigger()) {
            SwingUtilities.invokeLater(() -> this.showPopup(event.getX(), event.getY())); // 在鼠标事件发生的地方弹出菜单
        } else {
            // Switch to drag animation when mouse is pressed
            if (!paused && behavior != null) {
                try {
                    behavior.mousePressed(event);
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Severe error in mouse press handler for mascot \"" + this + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage"), e);
                    dispose();
                }
            }
        }
    }

    /**
     * popup menu在不同系统中的触发条件并不相同。因此，为了正确实现跨平台功能，必须同时监听mousePressed（鼠标按下）和mouseReleased（鼠标释放）两个动作，以此来确保能够检测到isPopupTrigger
     * @param event
     */
    private void mouseReleased(final MouseEvent event) {
        // Check for popup triggers in both mousePressed and mouseReleased
        // because it works differently on different systems
        if (event.isPopupTrigger()) {
            SwingUtilities.invokeLater(() -> this.showPopup(event.getX(), event.getY()));
        } else {
            if (!paused && behavior != null) {
                try {
                    behavior.mouseReleased(event);
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Severe error in mouse release handler for mascot \"" + this + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage"), e);
                    dispose();
                }
            }
        }
    }

    /**
     * 在给定的(x, y)坐标弹出窗口
     * @param x
     * @param y
     */
    public void showPopup(final int x, final int y) {
        final JPopupMenu popup = new JPopupMenu();
        final ResourceBundle languageBundle = Main.getInstance().getLanguageBundle();

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                setAnimating(true); // 将动画运行状态设为True
            }

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                setAnimating(false);
            }
        });

        // "Another One!" 菜单项：创建新的Mascot
        final JMenuItem increaseMenu = new JMenuItem(languageBundle.getString("CallAnother"));
        increaseMenu.addActionListener(event -> Main.getInstance().createMascot(imageSet));

        // "Bye Bye!" 菜单项：销毁当前Mascot
        final JMenuItem disposeMenu = new JMenuItem(languageBundle.getString("Dismiss"));
        disposeMenu.addActionListener(e -> this.dispose());

        // "Follow Mouse!" 菜单项：跟随鼠标
        final JMenuItem gatherMenu = new JMenuItem(languageBundle.getString("FollowCursor"));
        gatherMenu.addActionListener(event -> manager.setBehaviorAll(Main.getInstance().getConfiguration(imageSet), Main.BEHAVIOR_GATHER, imageSet));

        // "Reduce to One!" 菜单项：将Mascot减少到任意的一个
        final JMenuItem oneMenu = new JMenuItem(languageBundle.getString("DismissOthers"));
        oneMenu.addActionListener(event -> manager.remainOne(imageSet, this));

        // "Reduce to One!" 菜单项：将Mascot减少到仅剩当前的这个
        final JMenuItem onlyOneMenu = new JMenuItem(languageBundle.getString("DismissAllOthers"));
        onlyOneMenu.addActionListener(event -> manager.remainOne(this));

        // "Restore IE!" 菜单项：重启窗口
        final JMenuItem restoreMenu = new JMenuItem(languageBundle.getString("RestoreWindows"));
        restoreMenu.addActionListener(event -> environment.restoreIE());

        // Debug menu item
        final JMenuItem debugMenu = new JMenuItem(languageBundle.getString("RevealStatistics"));
        debugMenu.addActionListener(event -> {
            if (debugWindow == null) {
                debugWindow = new DebugWindow();
            }
            debugWindow.setVisible(true);
        });

        // "Bye Everyone!" 菜单项：退出程序
        final JMenuItem closeMenu = new JMenuItem(languageBundle.getString("DismissAll"));
        closeMenu.addActionListener(e -> Main.getInstance().exit());

        // "Paused" 菜单项：暂停当前的Mascot
        final JMenuItem pauseMenu = new JMenuItem(isAnimating() ? languageBundle.getString("PauseAnimations") : languageBundle.getString("ResumeAnimations"));
        pauseMenu.addActionListener(event -> this.paused = !this.paused);

        // 添加 Behaviors 子才当. 现在有点点bug，有些时候菜单会消失不见.
        // JLongMenu submenu = new JLongMenu(languageBundle.getString("SetBehaviour"), 30);
        JMenu subMenu = new JMenu(languageBundle.getString("SetBehaviour")); 
        JMenu allowedSubmenu = new JMenu(languageBundle.getString("AllowedBehaviours"));
        subMenu.setAutoscrolls(true);
        JMenuItem item; // 菜单项
        JCheckBoxMenuItem toggleItem; // 选框菜单项
        final Conf config = Main.getInstance().getConfiguration(this.imageSet);
    }

    void tick() {
        TO DO

    }

    public void apply() {
        to do
    }

    public void dispose() {
        synchronized (this) {
            log.log(Level.INFO, "Destroying mascot \"{0}\"", this);

            // 如果debugWindow窗口存在，还需要把debugWindow处理掉
            if (this.debugWindow != null) {
                this.debugWindow.setVisible(false);
                this.debugWindow = null;
            }

            this.animating = false;
            this.window.dispose();
            this.affordances.clear();
            if (this.manager != null) {
                this.manager.remove(this);
            }
        }
    }

    private void refreshCursor(Point position) {
        synchronized (hotspots) {
            boolean useHand = hotspots.stream().anyMatch(hotspot -> hotspot.contains(this, position) &&
                    Main.getInstance().getConfiguration(imageSet).isBehaviorEnabled(hotspot.getBehaviour(), this));

            refreshCursor(useHand);
        }
    }

    private void refreshCursor(Boolean useHand) {
        window.asComponent().setCursor(Cursor.getPredefinedCursor(useHand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public Manager getManager() {
        return manager;
    }
    
    public Point getAnchor() {
        return this.anchor;
    }

    public void setAnchor(Point anchor) {
        this.anchor = anchor;
    }

    public MascotImage getImage() {
        return image;
    }

    public void setImage(final MascotImage image) {
        // 如果image为空，则直接返回
        if (this.image == null && image == null) {
            return;
        }
        // 如果image已经是当前 mascot 的 image 了，也直接返回
        if (this.image != null && image != null && this.image.getImage().equals(image.getImage())) {
            return;
        }

        this.image = image;

        final Component windowComponent = window.asComponent();
        if (image == null) {
            windowComponent.setVisible(false);
            return;
        }
        // 如果图片能正常读取，则窗口更新为传入的图片
        window.setImage(image.getImage());
        windowComponent.setVisible(true);
        window.updateImage();
    }

    public boolean isLookRight() {
        return this.lookRight;
    }

    public void setLookRight(final boolean lookRight) {
        this.lookRight = lookRight;
    }

    /**
     * 返回图像的边框，如果没有，就返回最后一个窗口的边框
     * @return Rectangle
     */
    public Rectangle getBounds() {
        if (image != null) {
            // Find the window area from the ground coordinates and image center coordinates. The center has already been adjusted for scaling.
            final int top = anchor.y - image.getCenter().y;
            final int left = anchor.x - image.getCenter().x;

            return new Rectangle(left, top, image.getSize().width, image.getSize().height);
        } else {
            // 当我们没有图像时，就返回最后一个窗口
            return window.asComponent().getBounds();
        }
    }

    public int getTime() {
        return time;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(final Behavior behavior) throws CantBeAliveException {
        this.behavior = behavior;
        this.behavior.init(this);
    }

    public int getCount() {
        return manager != null ? manager.getCount(imageSet) : 0;
    }

    public int getTotalCount() {
        return manager != null ? manager.getCount() : 0;
    }

    private boolean isAnimating() {
        return animating && !paused;
    }

    private void setAnimating(final boolean animating) {
        this.animating = animating;
    }

    public MascotEnvironment getEnvironment() {
        return environment;
    }

    public List<String> getAffordances() {
        return affordances;
    }

    public List<Hotspot> getHotspots() {
        return hotspots;
    }

    public void setImageSet(final String set) {
        imageSet = set;
    }

    public String getImageSet() {
        return imageSet;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(final String name) {
        sound = name;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(final boolean paused) {
        this.paused = paused;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(final boolean isDragging) {
        dragging = isDragging;
    }

    public boolean isHotspotClicked() {
        return cursor != null;
    }

    public Point getCursorPosition() {
        return cursor;
    }

    public void setCursorPosition(final Point point) {
        cursor = point;

        if (point == null) {
            refreshCursor(false);
        } else {
            refreshCursor(point);
        }
    }

    public VariableMap getVariables() {
        if (variables == null) {
            variables = new VariableMap();
        }
        return variables;
    }
}
