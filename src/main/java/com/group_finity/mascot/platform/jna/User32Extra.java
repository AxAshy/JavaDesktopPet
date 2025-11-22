package com.group_finity.mascot.platform.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface User32Extra extends StdCallLibrary {
    User32Extra INSTANCE = Native.load("user32", User32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-isiconic">Microsoft docs: IsIconic</a>
     * <p>
     * 判断当前指定的窗口是否最小化 (iconic).
     *
     * @param hWnd 被检验的窗口句柄
     * @return If the window is iconic, the return value is true.
     */
    boolean IsIconic(HWND hWnd);

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-iszoomed">Microsoft docs: IsZoomed</a>
     * <p>
     * 判断当前指定的窗口是否最大化
     *
     * @param hWnd 被检验的窗口句柄
     * @return If the window is zoomed, the return value is true.
     */
    boolean IsZoomed(HWND hWnd);
}
