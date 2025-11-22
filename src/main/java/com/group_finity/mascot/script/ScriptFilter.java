package com.group_finity.mascot.script;

import org.openjdk.nashorn.api.scripting.ClassFilter;

/**
 * 通过实现ClassFilter接口，限制通过Nashorn运行的脚本对特定Java类的访问。
 * 要求只能访问com.group_finity.mascot开头的类。
 */
public class ScriptFilter implements ClassFilter{
    @Override
    public boolean exposeToScripts(String className) {
        return className.startsWith("com.group_finity.mascot");
    }
}
