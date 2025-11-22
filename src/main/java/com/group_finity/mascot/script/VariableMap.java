package com.group_finity.mascot.script;

import java.util.*;

import javax.script.Bindings;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.exception.VariableException;

/**
 * 一个实现了Bindings接口的哈希表，是用来存放数据的容器。它有3个层级，为Global级、Engine级和Local级，前2者通过ScriptEngine.getBindings()获得，是唯一的对象，而Local Binding由ScriptEngine.createBindings()获得，每次都产生一个新的实例。
 * Global对应到工厂，Engine对应到ScriptEngine，向这2者里面加入任何数据或者编译后的脚本执行对象，在每一份新生成的Local Binding里面都会存在。
 * 可以理解为Bindings就是JS中的占位符，目的是动态地控制Java代码的输入和输出。
 */
public class VariableMap extends AbstractMap<String, Object> implements Bindings{
    private final Map<String, Variable> rawMap = new LinkedHashMap<>();

    public Map<String, Variable> getRawMap() {
        return this.rawMap;
    }

    public void init() {
        for (final Variable var : rawMap.values()) {
            var.init();
        }
    }

    public void initFrame() {
        for (final Variable var : rawMap.values()) {
            var.initFrame();
        }
    }

    /**
     * 将原始的Map<String, Variable>对象的每个Entry以成Set格式存储
     */
    public final Set<Map.Entry<String, Object>> entrySet = new AbstractSet<>() {
        // 定义entrySet的迭代器
        @Override
        public Iterator<Map.Entry<String, Object>> iterator() {
            return new Iterator<>() {
                private final Iterator<Map.Entry<String, Variable>> rawIterator = getRawMap().entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return rawIterator.hasNext();
                }

                @Override
                public Map.Entry<String, Object> next() {
                    final Map.Entry<String, Variable> rawKeyValue = rawIterator.next();
                    final Variable value = rawKeyValue.getValue();

                    return new Map.Entry<>() {
                        @Override
                        public String getKey() {
                            return rawKeyValue.getKey();
                        }

                        @Override
                        public Object getValue() {
                            try {
                                return value.get(VariableMap.this);
                            } catch (final VariableException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public Object setValue(final Object value) {
                            throw new UnsupportedOperationException(Main.getInstance().getLanguageBundle().getString("SetValueNotSupportedErrorMessage"));
                        }
                    };
                }

                @Override
                public void remove() {
                    rawIterator.remove();
                }
            };
        }

        @Override
        public int size() {
            return getRawMap().size();
        }
    };

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return this.entrySet;
    }

    @Override
    public Object put(final String key, final Object value) {
        Object result;

        if (value instanceof Variable) {
            result = this.rawMap.put(key, (Variable) value);
        } else {
            result = this.rawMap.put(key, new Constant(value));
        }

        return result;
    }
}
