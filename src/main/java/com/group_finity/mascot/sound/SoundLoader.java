package com.group_finity.mascot.sound;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * 加载新的{@code Clip}对象到 {@link Sounds} 集合中. 已经存在的对象不会重复加入.
 */
public class SoundLoader {
    /**
     * 加载新的{@code Clip}对象到 {@link Sounds} 集合中. 已经存在的对象不会重复加入.
     * @param name 音频文件名称
     * @param volume 音频播放的音量
     */
    public static void load(final String name, final float volume) throws IOException, LineUnavailableException, UnsupportedAudioFileException {
        if (Sounds.contains(name + volume)) {
            return;
        }

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(name));
        final Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(volume);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                ((Clip) event.getLine()).stop();
            }
        });

        Sounds.put(name + volume, clip);
    }
}
