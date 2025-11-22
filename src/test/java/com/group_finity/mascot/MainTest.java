package com.group_finity.mascot;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.WindowsEnvironment;

public class MainTest {
    public static void main(String[] args) throws Exception{
        Environment impl = new WindowsEnvironment();
        // impl.init();
    }
}