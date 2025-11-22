package com.group_finity.mascot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 用于管理宠物 {@link Mascot Mascots} 列表，并且计时的类。如果每个 {@link Mascot} 同时移动, 会有各种各样的问题 (比如需要抛出窗口时),
 * 因此这个类用于调整和管理整个宠物的计时.
 * <p>
 * {@link #tick()} 方法会首先获取最新环境信息，让后移动所有的 {@link Mascot Mascots}.
 */
public class Manager {

    private static final Logger log = Logger.getLogger(Manager.class.getName());

    /**
     * The duration of each tick, in milliseconds.
     */
    public static final int TICK_INTERVAL = 40;

    /**
     * A list of {@link Mascot Mascots} which are managed by this {@code Manager}.
     */
    private final List<Mascot> mascots = new ArrayList<>();

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
     * 处理掉所有的 {@link Mascot Mascots}.
     */
    public void disposeAll() {
        synchronized (this.mascots) {
            this.mascots.forEach(mascot -> mascot.dispose());
        }
    }
}
