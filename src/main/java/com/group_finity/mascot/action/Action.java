package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

/**
 * 用于表示一个 {@link Mascot} 的短期移动的对象.
 * <p>
 * 会以固定的间隔调用{@link #next()}.
 */
public interface Action {

    /**
     * 开始一个 action 时调用.
     * @param mascot 绑定的 {@link Mascot} 
     * @throws VariableException 当传给 action 的参数时不合法或无法解析时抛出
     */
    void init(Mascot mascot) throws VariableException;

    /**
     * 确认是否有下一帧
     *
     * @return 是否有下一帧
     * @throws VariableException 当传给 action 的参数时不合法或无法解析时抛出
     */
    boolean hasNext() throws VariableException;

    /**
     * 将绑定的 {@link Mascot} 播放当前 action 的下一帧.
     * @throws LostGroundException 如果 {@link Mascot} 不在任意 {@link Border} 上 或 要开始 falling 动画时抛出
     * @throws VariableException 当传给 action 的参数时不合法或无法解析时抛出
     */
    void next() throws LostGroundException, VariableException;
}
