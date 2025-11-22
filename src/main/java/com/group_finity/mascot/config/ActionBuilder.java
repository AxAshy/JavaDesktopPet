package com.group_finity.mascot.config;

import java.util.*;
import java.util.logging.Logger;

public class ActionBuilder implements IActionBuilder {
    private static final Logger log = Logger.getLogger(ActionBuilder.class.getName());
    private final String type;
    private final String name;
    private final String className;
    private final Map<String, String> params = new LinkedHashMap<>();
    private final List<AnimationBuilder> animationBuilders = new ArrayList<>();
    private final List<IActionBuilder> actionRefs = new ArrayList<>();
    private final ResourceBundle schema;
}
