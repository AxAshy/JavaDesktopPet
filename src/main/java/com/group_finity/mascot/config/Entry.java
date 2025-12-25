package com.group_finity.mascot.config;

import org.w3c.dom.*;
import java.util.*;

/**
 * {@code Entry}本质上是用一个对象存储了XML的一个元素{@code Element}或者节点{@code Node}，将其的属性存在 entry 中方便访问。
 * 其中此 element 的属性以键值对{@code Map<String, String>}的形式存储。
 */
public class Entry {
    /**
     * org.w3c.dom是用于解析XML的api，元素类Element是Node类最主要的子对象。
     * 在元素中可以包含属性，因而Element中有存取其属性的方法
     */
    private final Element element;
    private Map<String, String> attributes;
    private List<Entry> children;
    /** 选中的子节点列表, String是节点名, List<Entry>是子节点列表 */
    private final Map<String, List<Entry>> selected = new HashMap<>();

    public Entry(final Element element) {
        this.element = element;
    }

    public String getName() {
        return element.getTagName();
    }

    public String getText() {
        return element.getTextContent();
    }

    public boolean hasAttribute(final String attributeName) {
        return element.hasAttribute(attributeName);
    }

    /**
     * 以键值对哈希表的方式返回 element 的所有 attributes
     */
    public Map<String, String> getAttributes() {
        if (this.attributes != null) {
            return this.attributes;
        }

        this.attributes = new LinkedHashMap<>();
        final NamedNodeMap attrs = this.element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            // 这里我自己修改过
            this.attributes.put(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
        }

        return this.attributes;
    }

    /**
     * 返回 element 指定 attribute 的值
     * @param attrName 指定 attribute 的名称
     * @return 指定 attribute 的值
     */
    public String getAttribute(final String attrName) {
        final Attr attr = this.element.getAttributeNode(attrName);
        if (attr == null) {
            return null;
        }
        return attr.getValue();
    }

    public List<Entry> getChildren() {
        if (this.children != null) {
            return this.children;
        } 

        this.children = new ArrayList<>();
        final NodeList childNodes = this.element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                this.children.add(new Entry((Element) childNode));
            }
        }

        return this.children;
    }

    /**
     * 根据给定的 tagName 获取指定的子节点
     * @param tagName
     * @return NodeName 符合 tagName 的节点, 会转成Entry后加入数组返回
     */
    public List<Entry> selectChildren(final String tagName) {
        List<Entry> children = this.selected.get(tagName);
        if (children != null) {
            return children;
        }
        children = new ArrayList<>();
        for (final Entry child : this.getChildren()) {
            if (child.getName().equals(tagName)) {
                children.add(child);
            }
        }

        this.selected.put(tagName, children);
        
        return children;
    }

    public boolean hasChild(final String tagName) {
        return this.getChildren().stream().anyMatch(child -> child.getName().equals(tagName));
    }
}
