package com.group_finity.mascot.config;

import java.util.Map;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;

/**
 * 用于创建 actions 和 action引用的对象的接口
 */
public interface IActionBuilder {
    /**
     * 根据给定的参数创建 action 和 它全部的children actions
     *
     * @param params 一个属性的 {@link Map}. This will contain the attributes from all actions in this action's
     * inheritance tree, as well as the non-functional attributes from the behavior corresponding to the root action
     * (non-functional referring to attributes that are not read by the program when building the behavior).
     * @return 创建的 action
     * @throws ActionInstantiationException if the action contains an action reference without a corresponding
     * action, an action's class can not be instantiated, or a script inside the action fails to be compiled
     */
    Action buildAction(final Map<String, String> params) throws ActionInstantiationException;

    /**
     * 合法化当前{@code action}. 应该在 {@link #buildAction(Map)} 调用后才能调用.
     *
     * @throws ConfigurationException 如果当前{@code action}或者它的其中一个 children actions 引用了不存在的action时抛出
     */
    void validate() throws ConfigurationException;
}
