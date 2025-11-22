package com.group_finity.mascot.script;

import javax.script.CompiledScript;
import javax.script.ScriptException;

import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.exception.VariableException;

/**
 * 一个用于执行JavaScript资源的类
 */
public class Script extends Variable {
    
    /**Nashorn引擎，用于运行和编译JavaScript资源 */
    private static final NashornScriptEngine ENGINE = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine(new ScriptFilter());


    private final String source;

    /** 是否在初始化Frame前清空Script.value */
    private final boolean clearAtInitFrame;

    /**CompiledScript可以将ScriptEngine解析一段脚本的结果存起来，方便多次调用。要用的时候每次调用一下CompiledScript.eval()即可，一般适合用于js函数的使用。*/
    private final CompiledScript compiled;

    private Object value;

    /**
     * 创建用于执行JS资源的Script对象
     * @param source JS资源
     * @param clearAtInitFrame 是否在初始化Frame前清空Script.value
     * @throws VariableException
     */
    public Script(final String source, final boolean clearAtInitFrame) throws VariableException{
        this.source = source;
        this.clearAtInitFrame = clearAtInitFrame;
        // 尝试让Nashorn引擎编译读取的本地JS资源
        try {
            this.compiled = ENGINE.compile(source);
        } catch (final ScriptException e) {
            throw new VariableException(Main.getInstance().getLanguageBundle().getString("ScriptCompilationErrorMessage") + ": " + this.source, e);
        }
    }

    @Override
    public String toString() {
        return clearAtInitFrame ? "#{" + source + "}" : "${" + source + "}";
    }

    @Override
    public void init() {
        value = null;
    }

    @Override
    public void initFrame() {
        if (clearAtInitFrame) {
            value = null;
        }
    }

    @Override
    public synchronized Object get(final VariableMap variables) throws VariableException {
        if (this.value != null) {
            return this.value;
        }

        try {
            // 传入variables占位符，执行CompiledScript后返回结果
            this.value = compiled.eval(variables);
        } catch (Exception e) {
            throw new VariableException(Main.getInstance().getLanguageBundle().getString("ScriptEvaluationErrorMessage") + ": " + this.source, e);
        }

        return this.value;
    }
}

