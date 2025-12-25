package com.group_finity.mascot.animation;

import java.util.Arrays;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;

/**
 * 动画实例对象，对应一个XML文件中的{@code Action}节点
 */
public class Animation {
    /**{@code Action}节点的Condition属性 */
    private final Variable condition;
    private final Pose[] poses;
    private final Hotspot[] hotspots;
    private final boolean turn;

    public Animation(final Variable condition, final Pose[] poses, final Hotspot[] hotspots, final boolean turn) {
        if (poses.length == 0) {
            throw new IllegalArgumentException("poses.length==0");
        }

        this.condition = condition;
        this.poses = poses;
        this.hotspots = hotspots;
        this.turn = turn;
    }

    /**
     * 判断当前Animation是否符合其Condition条件
     * @param variables
     * @return boolean
     */
    public boolean isEffective(final VariableMap variables) throws VariableException {
        return (Boolean) condition.get(variables);
    }

    public void init() {
        condition.init();
    }

    public void initFrame() {
        condition.initFrame();
    }

    public void next(final Mascot mascot, final int time) {
        getPoseAt(time).next(mascot);
    }

    public Pose getPoseAt(int time) {
        time %= getDuration();

        for (final Pose pose : poses) {
            time -= pose.getDuration();
            if (time < 0) {
                return pose;
            }
        }

        return null;
    }

    public int getDuration() {
        return Arrays.stream(poses).mapToInt(Pose::getDuration).sum();
    }

    public Hotspot[] getHotspots() {
        return hotspots;
    }

    public boolean isTurn() {
        return turn;
    }
}
