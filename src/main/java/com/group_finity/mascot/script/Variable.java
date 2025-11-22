package com.group_finity.mascot.script;

import com.group_finity.mascot.exception.VariableException;

/**
 * 用于获取在 JavaScript资源 中读取的参数
 */
public abstract class Variable {
    public static Variable pasrse(final String source) throws VariableException {
        Variable result = null;

        if (source != null) {
            if (source.startsWith("${") && source.endsWith("}")) {
                result = new Script(source.substring(2, source.length() -1 ), false);
            } else if (source.startsWith("#{") && source.endsWith("}")) {
                result = new Script(source.substring(2, source.length() - 1), true);
            } else {
                result = new Constant(parseConstant(source));
            }
        }

        return result;
    }

    /**
     * 将JS资源解析为常量变量{@link Constant Constant}
     * @param source JS资源
     * @return Object 常量变量，如true、false、double等
     */
    public static Object parseConstant(final String source) {
        Object result = null;

        if (source != null) {
            if (source.equals("true")) {
                result = Boolean.TRUE;
            } else if (source.equals("false")) {
                result = Boolean.FALSE;
            } else {
                try {
                    result = Double.parseDouble(source);
                } catch (final NumberFormatException e) {
                    result = source;
                }
            }
        }
        
        return result;
    }

    public abstract void init();

    public abstract void initFrame();

    /**
     * 传入占位符{@code variables}给一个{@code CompiledScript}实例执行后，返回执行结果
     * @param variables VariableMap是存储{@code variables}的哈希表
     * @return Object {@code CompiledScript}实例执行后返回的结果
     * @throws VariableException
     */
    public abstract Object get(VariableMap variables) throws VariableException;
}
