package com.group_finity.mascot.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.Clip;

import com.group_finity.mascot.Main;

/**
 * 这个静态类包含了所有加载到Shemeji程序的声音文件
 */
public class Sounds {
    private static final ConcurrentHashMap<String, Clip> SOUNDS = new ConcurrentHashMap<>();

    /**
     * 向 {@link Sounds} 这个静态类中添加声音片段
     * @param fileName 声音文件名
     * @param clip 声音片段对象
     */
    public static void put(final String fileName, final Clip clip) {
        if (!SOUNDS.containsKey(fileName)) {
            SOUNDS.put(fileName, clip);
        }
    }

    public static boolean contains(String fileName) {
        return SOUNDS.containsKey(fileName);
    }

    public static Clip getSound(String fileName) {
        if (!SOUNDS.containsKey(fileName)) {
            return null;
        }
        return SOUNDS.get(fileName);
    }

    public static List<Clip> getSoundsIgnoringVolume(String fileName) {
        List<Clip> sounds = new ArrayList<>(5);
        for (Map.Entry<String, Clip> entry : SOUNDS.entrySet()) {
            String soundName = entry.getKey();
            Clip soundClip = entry.getValue();
            if (soundName.startsWith(fileName)) {
                sounds.add(soundClip);
            }
        }
        return sounds;
    }

    /**
     * 从配置setting中获取Sounds属性值, 即是否静音
     * @return boolean
     */
    public static boolean isMuted() {
        return !Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Sounds", "true"));
    }

    public static void setMuted(boolean mutedFlag) {
        if (mutedFlag) {
            // mute everything
            for (Clip clip : SOUNDS.values()) {
                clip.stop();
            }
        }
    }
}
