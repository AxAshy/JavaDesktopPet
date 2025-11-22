package com.group_finity.mascot.image;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class ImagePairs {
    /**
     * 用线程安全哈希表存储的图片对imagePairs
     */
    private static final ConcurrentHashMap<String, ImagePair> imagePairs = new ConcurrentHashMap<>();

    public static void put(final String filename, final ImagePair imagePair) {
        if (!imagePairs.containsKey(filename)) {
            imagePairs.put(filename, imagePair);
        }
    }

    public static ImagePair getImagePair(String filename) {
        if (!imagePairs.containsKey(filename)) {
            return null;
        }
        return imagePairs.get(filename);
    }

    public static boolean contains(String filename) {
        return imagePairs.containsKey(filename);
    }

    public static void clear() {
        imagePairs.clear();
    }

    public static void removeAll(String searchTerm) {
        if (imagePairs.isEmpty()) {
            return;
        }

        // imagePairs 的 key 是文件名，使用一个filter来清除所有符合条件的imagePair
        // 图片存储时，图片的名称为 父文件+编号
        imagePairs.keySet().removeIf(key -> searchTerm.equals(Path.of(key).getParent().toString()));
    }

    public static MascotImage getImage(String filename, boolean isLookRight) {
        if (!imagePairs.containsKey(filename)) {
            return null;
        }
        return imagePairs.get(filename).getImage(isLookRight);
    }
}
