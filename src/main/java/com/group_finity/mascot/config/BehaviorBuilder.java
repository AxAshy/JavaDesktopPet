package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.exception.*;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object that builds {@link Behavior}s.
 */
public class BehaviorBuilder {
    private static final Logger log = Logger.getLogger(BehaviorBuilder.class.getName());

    private final Configuration configuration;

    private final String name;

    private final String actionName;

    private final int frequency;

    private final List<String> conditions;

    private final boolean hidden;

    private final boolean toggleable;

    private final boolean nextAdditive;

    private final List<BehaviorBuilder> nextBehaviorBuilders = new ArrayList<>();

    private final Map<String, String> params = new LinkedHashMap<>();

    public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) {
        this.configuration = configuration;
        ResourceBundle schema = configuration.getSchema();
        name = behaviorNode.getAttribute(schema.getString("Name"));
        actionName = behaviorNode.getAttribute(schema.getString("Action")) == null ? name : behaviorNode.getAttribute(schema.getString("Action"));
        frequency = Integer.parseInt(behaviorNode.getAttribute(schema.getString("Frequency")));
        hidden = Boolean.parseBoolean(behaviorNode.getAttribute(schema.getString("Hidden")));
        this.conditions = new ArrayList<>(conditions);
        this.conditions.add(behaviorNode.getAttribute(schema.getString("Condition")));

        // override of toggleable state for required fields
        // fall/throw/dragged这个三个动作是不可开关的
        if (name.equals(UserBehavior.BEHAVIOURNAME_FALL) ||
                name.equals(UserBehavior.BEHAVIOURNAME_THROWN) ||
                name.equals(UserBehavior.BEHAVIOURNAME_DRAGGED)) {
            toggleable = false;
        } else {
            toggleable = Boolean.parseBoolean(behaviorNode.getAttribute(schema.getString("Toggleable")));
        }
        log.log(Level.FINE, "Loading behavior: {0}", this);

        params.putAll(behaviorNode.getAttributes());
        params.remove(schema.getString("Name"));
        params.remove(schema.getString("Action"));
        params.remove(schema.getString("Frequency"));
        params.remove(schema.getString("Hidden"));
        params.remove(schema.getString("Condition"));
        params.remove(schema.getString("Toggleable"));

        // 处理下一个行为
        boolean nextAdditive = true;
        for (final Entry nextList : behaviorNode.selectChildren(schema.getString("NextBehaviourList"))) {
            nextAdditive = Boolean.parseBoolean(nextList.getAttribute(schema.getString("Add")));
            loadBehaviors(nextList, new ArrayList<>());
        }
        this.nextAdditive = nextAdditive;
        log.log(Level.FINE, "Finished loading behavior: {0}", this);
    }

    @Override
    public String toString() {
        return "Behavior(" + name + "," + frequency + "," + actionName + ")";
    }

    private void loadBehaviors(final Entry list, final List<String> conditions) {
        for (final Entry node : list.getChildren()) {
            if (node.getName().equals(configuration.getSchema().getString("Condition"))) {
                final List<String> newConditions = new ArrayList<>(conditions);
                newConditions.add(node.getAttribute(configuration.getSchema().getString("Condition")));

                loadBehaviors(node, newConditions);
            } else if (node.getName().equals(configuration.getSchema().getString("BehaviourReference"))) {
                final BehaviorBuilder behavior = new BehaviorBuilder(configuration, node, conditions);
                nextBehaviorBuilders.add(behavior);
            }
        }
    }

    /**
     * Validates the behavior. Should be called after {@link #buildBehavior()} has been called.
     *
     * @throws ConfigurationException if the behavior references a nonexistent action
     */
    public void validate() throws ConfigurationException {
        if (!configuration.getActionBuilders().containsKey(actionName)) {
            log.log(Level.SEVERE, "There is no corresponding action for behavior: {0}", this);
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("NoActionFoundErrorMessage") + "(" + this + ")");
        }
    }

    /**
     * Builds the behavior, its corresponding action, and all of the corresponding action's children actions/action
     * references.
     *
     * @return the built behavior
     * @throws BehaviorInstantiationException if the behavior's corresponding action fails to be built
     * @see IActionBuilder#buildAction(Map)
     */
    public Behavior buildBehavior() throws BehaviorInstantiationException {
        try {
            return new UserBehavior(name,
                    configuration.buildAction(actionName,
                            params), configuration);
        } catch (final ActionInstantiationException e) {
            log.log(Level.SEVERE, "Failed to initialize the corresponding action for behavior: " + this, e);
            throw new BehaviorInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseCorrespondingActionErrorMessage") + "(" + this + ")", e);
        }
    }

    public boolean isEffective(final VariableMap context) throws VariableException {
        if (frequency == 0) {
            return false;
        }

        for (final String condition : conditions) {
            if (condition != null) {
                if (!(Boolean) Variable.parse(condition).get(context)) {
                    return false;
                }
            }
        }

        return true;
    }

    public String getName() {
        return name;
    }

    public int getFrequency() {
        return frequency;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public boolean isNextAdditive() {
        return nextAdditive;
    }

    public List<BehaviorBuilder> getNextBehaviorBuilders() {
        return nextBehaviorBuilders;
    }
}
