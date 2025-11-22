package com.group_finity.mascot.script;

/**
 * {@link Variable Variable} 的子类，常量变量类型
 */
public class Constant extends Variable{
    private final Object value;

    public Constant(final Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value == null ? "null" : value.toString();
    }

    @Override
    public void init() {
    }

    @Override
    public void initFrame() {
    }

    @Override
    public Object get(final VariableMap variables) {
        return value;
    }
}
