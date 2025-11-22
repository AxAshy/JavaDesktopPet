package com.group_finity.mascot.config;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Configuration {
    private static final Logger log = Logger.getLogger(Configuration.class.getName());

    private static final ResourceBundle SCHEMA_EN = ResourceBundle.getBundle("schema", Locale.US);
    private ResourceBundle schema;

    private final Map<String, String> constants = new LinkedHashMap<>(2);
    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<>();
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<>();
    private final Map<String, String> information = new LinkedHashMap<>(8);
}
