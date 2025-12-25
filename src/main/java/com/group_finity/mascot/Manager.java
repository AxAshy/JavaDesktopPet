package com.group_finity.mascot;

import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.platform.NativeFactory;

/**
 * 用于管理宠物 {@link Mascot Mascots} 列表，并且计时的类。如果每个 {@link Mascot} 同时移动, 会有各种各样的问题 (比如需要抛出窗口时),
 * 因此这个类用于调整和管理整个宠物的计时.
 * <p>
 * {@link #tick()} 方法会首先获取最新环境信息，让后移动所有的 {@link Mascot Mascots}.
 */
public class Manager {

    private static final Logger log = Logger.getLogger(Manager.class.getName());

    /**
     * 每次tick的持续时长(单位毫秒)
     */
    public static final int TICK_INTERVAL = 40;

    /**
     * 被这个 {@code Manager} 管理的 {@link Mascot Mascots} 列表
     */
    private final List<Mascot> mascots = new ArrayList<>();

    /**
     * 被添加的 {@link Mascot Mascots} 列表. 
     * 为了防止 {@link ConcurrentModificationException}, {@link Mascot} 的增加要被反射到每一次 {@link #tick()}.
     */
    private final Set<Mascot> added = new LinkedHashSet<>();

    /**
     * 被移除的 {@link Mascot Mascots} 列表. 
     * 为了防止 {@link ConcurrentModificationException}, {@link Mascot} 的移除要被反射到每一次 {@link #tick()}.
     */
    private final Set<Mascot> removed = new LinkedHashSet<>();

    /**
     * 最后一个 {@link Mascot} 被删除后程序是否要退出.
     * 如果你没能成功创建一个托盘icon, 进程会一直保留直到你在 {@link Mascot} 消失时关闭程序.
     */
    private boolean exitOnLastRemoved = true;

    /**
     * 循环 {@link #tick()} 的线程.
     */
    private Thread thread;

    public Manager() {
        // 这是为了修复java在win时运行时的bug: 短期内频繁的调用Thread.sleep会打乱win的时钟
        // 你可以通过调用一个很长时间的 Thread.sleep 来避免这个问题.
        new Thread() {
            {
                setDaemon(true);
                start();
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(Integer.MAX_VALUE);
                    } catch (final InterruptedException ignored) {
                    }
                }
            }
        };
    }

    /**
     * 设置这个manager 最后一个 {@link Mascot} 被删除后程序是否要退出 的属性
     * @param exitOnLastRemoved 最后一个 {@link Mascot} 被删除后程序是否要退出.
     */
    public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
        this.exitOnLastRemoved = exitOnLastRemoved;
    }

    /**
     * @return boolean 最后一个 {@link Mascot} 被删除后程序是否要退出.
     */
    public boolean isExitOnLastRemoved() {
        return exitOnLastRemoved;
    }

    /**
     * 开始线程
     */
    public void start() {
        if (thread != null && thread.isAlive()) {
            // Thread is already running
            return;
        }

        thread = new Thread(() -> {
            // I think nanoTime() is used instead of currentTimeMillis() because it may be more accurate on some systems that way.

            // Previous time
            long prev = System.nanoTime() / 1000000;
            try {
                while (true) {
                    // Current time
                    // Loop until TICK_INTERVAL has passed.
                    final long cur = System.nanoTime() / 1000000;
                    if (cur - prev >= TICK_INTERVAL) {
                        if (cur > prev + TICK_INTERVAL * 2) {
                            prev = cur;
                        } else {
                            prev += TICK_INTERVAL;
                        }
                        // Move the mascots.
                        tick();
                        continue;
                    }
                    Thread.sleep(1, 0);
                }
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "Ticker");
        thread.setDaemon(false);
        thread.start();
    }

    /**
     * 停止线程
     */
    public void stop() {
        if (thread == null || !thread.isAlive()) {
            // Thread is no longer running
            return;
        }
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * 逐帧移动 {@link Mascot Mascots}
     */
    private void tick() {
        // Update the environmental information first
        NativeFactory.getInstance().getEnvironment().tick();

        synchronized (mascots) {
            // Add the mascots which should be added
            mascots.addAll(added);
            added.clear();

            // Remove the mascots which should be removed
            for (final Mascot mascot : removed) {
                mascots.remove(mascot);
            }
            removed.clear();

            // Advance the mascots' time
            for (final Mascot mascot : mascots) {
                mascot.tick();
            }

            // Advance the mascots' images and positions
            for (final Mascot mascot : mascots) {
                mascot.apply();
            }
        }

        if (exitOnLastRemoved && mascots.isEmpty()) {
            // exitOnLastRemoved is true and there are no mascots left, so exit.
            Main.getInstance().exit();
        }
    }

    public void add(final Mascot mascot) {
        synchronized (added) {
            added.add(mascot);
            removed.remove(mascot);
        }
        mascot.setManager(this);
    }

    /**
     * Removes a {@link Mascot}.
     * Removal is done at the next {@link #tick()} timing.
     *
     * @param mascot the {@link Mascot} to remove
     */
    public void remove(final Mascot mascot) {
        synchronized (added) {
            added.remove(mascot);
            removed.add(mascot);
        }
        mascot.setManager(null);
        // Clear affordances so the mascot is not participating in any interactions, as that can cause an NPE
        mascot.getAffordances().clear();
    }

    /**
     * Sets the {@link Behavior} for all {@link Mascot Mascots}.
     *
     * @param name the name of the {@link Behavior}
     */
    public void setBehaviorAll(final String name) {
        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
                try {
                    Configuration configuration = Main.getInstance().getConfiguration(mascot.getImageSet());
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name), mascot));
                } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                    log.log(Level.SEVERE, "Failed to set behavior to \"" + name + "\" for mascot \"" + mascot + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), e);
                    mascot.dispose();
                }
            }
        }
    }

    /**
     * Sets the {@link Behavior} for all {@link Mascot Mascots} with the specified image set.
     *
     * @param configuration the {@link Configuration} to use to build the {@link Behavior}
     * @param name the name of the {@link Behavior}
     * @param imageSet the image set for which to check
     */
    public void setBehaviorAll(final Configuration configuration, final String name, String imageSet) {
        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
                try {
                    if (mascot.getImageSet().equals(imageSet)) {
                        mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(name), mascot));
                    }
                } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                    log.log(Level.SEVERE, "Failed to set behavior to \"" + name + "\" for mascot \"" + mascot + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), e);
                    mascot.dispose();
                }
            }
        }
    }

    /**
     * Dismisses mascots until one remains.
     */
    public void remainOne() {
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i > 0; i--) {
                mascots.get(i).dispose();
            }
        }
    }

    /**
     * Dismisses all mascots except for the one specified.
     *
     * @param mascot the mascot to not dismiss
     */
    public void remainOne(Mascot mascot) {
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                if (!mascots.get(i).equals(mascot)) {
                    mascots.get(i).dispose();
                }
            }
        }
    }

    /**
     * Dismisses mascots which use the specified image set until one mascot remains.
     *
     * @param imageSet the image set for which to check
     */
    public void remainOne(String imageSet) {
        synchronized (mascots) {
            int totalMascots = mascots.size();
            boolean isFirst = true;
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
                if (m.getImageSet().equals(imageSet) && isFirst) {
                    isFirst = false;
                } else if (m.getImageSet().equals(imageSet) && !isFirst) {
                    m.dispose();
                }
            }
        }
    }

    /**
     * Dismisses mascots which use the specified image set until only the specified mascot remains.
     *
     * @param imageSet the image set for which to check
     * @param mascot the mascot to not dismiss
     */
    public void remainOne(String imageSet, Mascot mascot) {
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
                if (m.getImageSet().equals(imageSet) && !m.equals(mascot)) {
                    m.dispose();
                }
            }
        }
    }

    /**
     * Dismisses all mascots which use the specified image set.
     *
     * @param imageSet the image set for which to check
     */
    public void remainNone(String imageSet) {
        synchronized (mascots) {
            int totalMascots = mascots.size();
            for (int i = totalMascots - 1; i >= 0; i--) {
                Mascot m = mascots.get(i);
                if (m.getImageSet().equals(imageSet)) {
                    m.dispose();
                }
            }
        }
    }

    /**
     * 处理掉所有的 {@link Mascot Mascots}.
     */
    public void disposeAll() {
        synchronized (this.mascots) {
            this.mascots.forEach(mascot -> mascot.dispose());
        }
    }

    /**
     * 先判断是否所有的Mascot都被暂停了，如果所有都是暂停了，就把所有mascot的暂停状态设为true，否则所有设为false
     */
    public void togglePauseAll() {
        synchronized (mascots) {
            boolean isPaused = mascots.stream().allMatch(Mascot::isPaused); // 是否所有的Mascot都被暂停了
            // 如果所有都是暂停了，就把所有mascot的暂停状态设为true，否则所有设为false
            for (final Mascot mascot : mascots) {
                mascot.setPaused(isPaused);
            }
        }
    }

    /**
     * @return boolean 是否这个manager管理的所有mascots都暂停了
     */
    public boolean isPaused() {
        synchronized (mascots) {
            return mascots.stream().allMatch(Mascot::isPaused);
        }
    }

    /**
     * Gets the current number of {@link Mascot Mascots}.
     *
     * @return the current number of {@link Mascot Mascots}
     */
    public int getCount() {
        return getCount(null);
    }

    /**
     * Gets the current number of {@link Mascot Mascots} with the given image set.
     *
     * @param imageSet the image set for which to check
     * @return the current number of {@link Mascot Mascots}
     */
    public int getCount(String imageSet) {
        synchronized (mascots) {
            if (imageSet == null) {
                return mascots.size();
            } else {
                return (int) mascots.stream().filter(m -> m.getImageSet().equals(imageSet)).count();
            }
        }
    }

     /**
     * Returns a Mascot with the given affordance.
     *
     * @param affordance the affordance for which to check
     * @return a {@link WeakReference} to a mascot with the required affordance, or {@code null} if none was found
     */
    public WeakReference<Mascot> getMascotWithAffordance(String affordance) {
        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
                if (mascot.getAffordances().contains(affordance)) {
                    return new WeakReference<>(mascot);
                }
            }
        }

        return null;
    }

    public boolean hasOverlappingMascotsAtPoint(Point anchor) {
        int count = 0;

        synchronized (mascots) {
            for (final Mascot mascot : mascots) {
                if (mascot.getAnchor().equals(anchor)) {
                    count++;
                }
                if (count > 1) {
                    return true;
                }
            }
        }

        return false;
    }
}
