package com.group_finity.mascot.behavior;


import java.awt.event.MouseEvent;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.CantBeAliveException;

/**
 * 表示一个 {@link Mascot} 的长期行为的对象.
 * <p>
 * 调用 {@link Mascot#setBehavior(Behavior)} 配置.
 */
public interface Behavior {
    /**
     * 当开始一个行为时触发
     * @param mascot the {@link Mascot} with which to associate
     * @throws CantBeAliveException 当前{@code behavior}, 它相关的{@code action}, 或任意下一个{@code behavior} 发起失败，则关联的这个 {@link Mascot} 应该被处理掉
     */
    void init(Mascot mascot) throws CantBeAliveException;

    /**
     * 将这个 mascot 提到下一个框{@code frame}.
     * @throws CantBeAliveException 如果下一个{@code behavior}发起失败，则关联的这个 {@link Mascot} 应该被处理掉
     */
    void next() throws CantBeAliveException;

    /**
     * 当鼠标按下时触发
     * @param e the event created by a mouse button being pressed
     * @throws CantBeAliveException if the next behavior fails to initialize and the associated {@link Mascot} should be
     * disposed
     */
    void mousePressed(MouseEvent e) throws CantBeAliveException;

    /**
     * 当鼠标按下时触发
     * @param e the event created by a mouse button being released
     * @throws CantBeAliveException if the next behavior fails to initialize and the associated {@link Mascot} should be
     * disposed
     */
    void mouseReleased(MouseEvent e) throws CantBeAliveException;
}
