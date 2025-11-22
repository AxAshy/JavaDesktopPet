package com.group_finity.mascot.platform;

import com.group_finity.mascot.environment.Environment;

public class WindowsNativeFactory extends NativeFactory{
    private final Environment environment = new WindowsEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        return new WindowsTranslucentWindow();
    }
}
