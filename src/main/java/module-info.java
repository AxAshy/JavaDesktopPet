module com.group_finity.mascot {
    requires java.desktop;
    requires java.management;
    requires java.logging;
    requires java.scripting;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.openjdk.nashorn;
    requires AbsoluteLayout.RELEASE280;

    exports com.group_finity.mascot;
    exports com.group_finity.mascot.action;
    exports com.group_finity.mascot.animation;
    exports com.group_finity.mascot.behavior;
    exports com.group_finity.mascot.config;
    exports com.group_finity.mascot.environment;
    exports com.group_finity.mascot.exception;
    exports com.group_finity.mascot.image;
    exports com.group_finity.mascot.imagesetchooser;
    exports com.group_finity.mascot.menu;
    exports com.group_finity.mascot.platform;
    exports com.group_finity.mascot.platform.jna;
    exports com.group_finity.mascot.script;
    exports com.group_finity.mascot.sound;
    exports hqx;
}
