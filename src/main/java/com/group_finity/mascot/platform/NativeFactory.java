package com.group_finity.mascot.platform;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.Environment;
import com.sun.jna.Platform;

/**
 * 提供返回当前环境的入口
 * {@link #getInstance()} 根据当前用户的系统，返回一个win/mac/linux(x11)/通用的子类
 */
public abstract class NativeFactory {
    private static NativeFactory instance;

    static {
        resetInstance();
    }

    /**
     * 根据当前运行的环境返回对应的子类实例
     * @return the environment-specific subclass
     */
    public static NativeFactory getInstance() {
        return instance;
    }

    /**
     * 创建子类实例
     */
    public static void resetInstance() {
        String environment = Main.getInstance().getProperties().getProperty("Environment", "generic");

        if (environment.equals("generic")) {
            if (Platform.isWindows()) {
                instance = new WindowsNativeFactory();
            } else if (Platform.isMac()) {
                // instance = new MacNativeFactory();
            } else if (/* Platform.isLinux() */ Platform.isX11()) {
                // Because Linux uses X11, this functions as the Linux support.
                // instance = new X11NativeFactory();
            }
        } else if (environment.equals("virtual")) {
            // instance = new VirtualNativeFactory();
        }
    }

    /**
     * Gets the {@link Environment} object.
     *
     * @return the {@link Environment} object
     */
    public abstract Environment getEnvironment();

    /**
     * Creates a window that can be displayed translucently.
     *
     * @return the new window
     */
    public abstract TranslucentWindow newTranslucentWindow();
}
