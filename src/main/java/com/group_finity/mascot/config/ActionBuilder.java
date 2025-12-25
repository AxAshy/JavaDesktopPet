package com.group_finity.mascot.config;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.action.*;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.*;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

/**
 * 从XML文件中加载动作, 一个{@code Action}节点对应一个{@code ActionBuilder}，它会调用 {@link AnimationBuilder} 用于加载Action节点的{@code Animation}子节点,
 * 它有一串有序的 {@link Pose} 组成, 每个{@code Pose}节点包含一对图片、图片坐标、持续时长、音频
 */
public class ActionBuilder implements IActionBuilder {
    private static final Logger log = Logger.getLogger(ActionBuilder.class.getName());
    /**当期Action节点的type属性 */
    private final String type;
    /**当期Action节点的name属性 */
    private final String name;
    /**当期Action节点的className属性 */
    private final String className;
    /**参数表 */
    private final Map<String, String> params = new LinkedHashMap<>();
    /**用于生成Animation子节点的AnimationBuilder列表 */
    private final List<AnimationBuilder> animationBuilders = new ArrayList<>();
    /**用于生成Action子节点的IActionBuilder列表 */
    private final List<IActionBuilder> actionRefs = new ArrayList<>();
    private final ResourceBundle schema;

    public ActionBuilder(final Configuration configuration, final Entry actionNode, final String imageSet) throws ConfigurationException {
        // 将 Action node 的属性加载进来
        schema = configuration.getSchema();
        name = actionNode.getAttribute(schema.getString("Name"));
        type = actionNode.getAttribute(schema.getString("Type"));
        className = actionNode.getAttribute(schema.getString("Class"));

        log.log(Level.FINE, "Loading action: {0}", this);

        try {
            params.putAll(actionNode.getAttributes());
            // 遍历每个Action节点的Animation子节点, Animation子节点里是一串按顺序排列的图片
            for (final Entry node : actionNode.selectChildren(schema.getString("Animation"))) {
                animationBuilders.add(new AnimationBuilder(schema, node, imageSet));
            }

            // 遍历每个Action节点的子节点，按照子节点的名称进行不同的处理
            for (final Entry node : actionNode.getChildren()) {
                if (node.getName().equals(schema.getString("ActionReference"))) {
                    actionRefs.add(new ActionRef(configuration, node));
                } else if (node.getName().equals(schema.getString("Action"))) {
                    actionRefs.add(new ActionBuilder(configuration, node, imageSet));
                }
            }

        } catch (ConfigurationException e) {
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedLoadActionErrorMessage") + " \"" + name + "\" " + Main.getInstance().getLanguageBundle().getString("ForShimeji") + " \"" + imageSet + "\".", e);
        }

        log.log(Level.FINE, "Finished loading action: {0}", this);
    }

    @Override
    public String toString() {
        return "Action(" + name + "," + type + "," + className + ")";
    }

    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
        try {
            // Create Variable Map
            final VariableMap variables = this.createVariables(params);

            // Create Animations
            final List<Animation> animations = this.createAnimations();
            
            // Create Child Actions
            final List<Action> actions = this.createActions();

            if (type.equals(schema.getString("Embedded"))) {
                try {
                    @SuppressWarnings("unchecked")
                    // 根据当前Action节点的className属性，获取对应的类
                    final Class<? extends Action> cls = (Class<? extends Action>) Class.forName(className);
                    try {
                        try {
                            // 返回这几个类的构造器？
                            return cls.getConstructor(ResourceBundle.class, List.class, VariableMap.class).newInstance(schema, animations, variables);
                        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException |
                                 NoSuchMethodException | SecurityException | InvocationTargetException e) {
                            // TODO: handle exception
                        }
                        return cls.getConstructor(ResourceBundle.class, VariableMap.class).newInstance(schema, variables);
                    } catch (IllegalAccessException | IllegalArgumentException | InstantiationException |
                             NoSuchMethodException | SecurityException | InvocationTargetException e) {
                        // NOTE There seems to be no constructor, so move on to the next
                    }
                    return cls.getConstructor().newInstance();
                } catch (final InstantiationException e) {
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedClassActionInitialiseErrorMessage") + "(" + this + ")", e);
                } catch (final IllegalAccessException e) {
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("CannotAccessClassActionErrorMessage") + "(" + this + ")", e);
                } catch (final ClassNotFoundException e) {
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("ClassNotFoundErrorMessage") + "(" + this + ")", e);
                } catch (NoSuchMethodException e) {
                    // TODO Get translations for the following error message
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("ClassConstructorNotFoundErrorMessage") + "(" + this + ")", e);
                } catch (InvocationTargetException e) {
                    // TODO Think of a unique error message for this without wording it confusingly
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedClassActionInitialiseErrorMessage") + "(" + this + ")", e);
                }
            } else if (type.equals(schema.getString("Move"))) {
                return new Move(schema, animations, variables);
            } else if (type.equals(schema.getString("Stay"))) {
                return new Stay(schema, animations, variables);
            } else if (type.equals(schema.getString("Animate"))) {
                return new Animate(schema, animations, variables);
            } else if (type.equals(schema.getString("Sequence"))) {
                return new Sequence(schema, variables, actions.toArray(new Action[0]));
            } else if (type.equals(schema.getString("Select"))) {
                return new Select(schema, variables, actions.toArray(new Action[0]));
            } else {
                throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("UnknownActionTypeErrorMessage") + "(" + this + ")");
            }

        } catch (final AnimationInstantiationException e) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedCreateAnimationErrorMessage") + "(" + this + ")", e);
        } catch (final VariableException e) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage") + "(" + this + ")", e);
        }
    }

    @Override
    public void validate() throws ConfigurationException {
        for (final IActionBuilder ref : actionRefs) {
            ref.validate();
        }
    }

    public List<Action> createActions() throws ActionInstantiationException {
        final List<Action> actions = new ArrayList<>();
        for (final IActionBuilder ref : this.actionRefs) {
            actions.add(ref.buildAction(new HashMap<>()));
        }
        return actions;
    }

    private List<Animation> createAnimations() throws AnimationInstantiationException {
        final List<Animation> animations = new ArrayList<>();
        for (final AnimationBuilder animationFactory : animationBuilders) {
            animations.add(animationFactory.buildAnimation());
        }
        return animations;
    }

    private VariableMap createVariables(final Map<String, String> params) throws VariableException {
        final VariableMap variables = new VariableMap();
        for (final Map.Entry<String, String> param : this.params.entrySet()) {
            variables.put(param.getKey(), Variable.parse(param.getValue()));
        }
        for (final Map.Entry<String, String> param : params.entrySet()) {
            variables.put(param.getKey(), Variable.parse(param.getValue()));
        }
        return variables;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
