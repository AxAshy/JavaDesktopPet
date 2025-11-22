package com.group_finity.mascot;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.Configuration;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.xml.sax.SAXParseException;

public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    /**配置文件目录 */
    public static final Path CONFIG_DIRECTORY = Path.of("conf"); 
    /**图片文件目录 */
    public static final Path IMAGE_DIRECTORY = Path.of("img");
    /**音频文件目录 */
    public static final Path SOUND_DIRECTORY = Path.of("sound");
    /**设置文件路径 */
    public static final Path SETTINGS_FILE = CONFIG_DIRECTORY.resolve("settings.properties");
    /**logging文件路径 */
    public static final Path LOGGING_FILE = CONFIG_DIRECTORY.resolve("logging.properties"); 
    /**软件图标文件路径 */
    public static final Path ICON_FILE = IMAGE_DIRECTORY.resolve("icon.png");
    
    /**Action that matches the "Gather Around Mouse!" context menu command */
    static final String BEHAVIOR_GATHER = "ChaseMouse"; // 鼠标追踪指令？

    static {
        try (InputStream input = Files.newInputStream(LOGGING_FILE)) {
            LogManager.getLogManager().readConfiguration(input);
        } catch (final SecurityException | IOException e) {
            log.log(Level.SEVERE, "Failed to load log properties", e);
        } catch (OutOfMemoryError err) {
            log.log(Level.SEVERE, "Out of memory. There are probably too many "
                    + "Shimeji mascots in the image folder for your computer to handle. "
                    + "Select fewer image sets or move some to the "
                    + "img/unused folder and try again.", err);
            showError("Out of memory. There are probably too many\n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the\n"
                    + "img/unused folder and try again.");
            System.exit(0);
        }
    }

    /**Main实例 */
    private static final Main instance = new Main();
    /**管理多个 {@link Mascot mascot} 的 {@link Manager manager} 实例 */
    private final Manager manager = new Manager();
    /**图片数组 */
    private ArrayList<String> imageSets = new ArrayList<>();
    /**线程安全的哈希表-配置集合 */
    private final ConcurrentHashMap<String, Configuration> configurations = new ConcurrentHashMap<>();
    /**线程安全的和哈希表-子图集合 */
    private final ConcurrentHashMap<String, ArrayList<String>> childImageSets = new ConcurrentHashMap<>();
    private final Properties properties = new Properties();
    private ResourceBundle languageBundle;

    /**
     * 程序的图标
     * Should be accessed through {@link #getIcon}, which initializes this field if it is {@code null}.
     */
    private static BufferedImage icon;

    /**应用的窗口容器 */
    private static JFrame frame;
    /**JDialog 是Java Swing 库提供的一个用于创建对话框窗口的组件，可以在应用程序中显示模态或非模态的自定义对话框。 */
    private JDialog form;

    public static Main getInstance() {
        return instance;
    }

    /**
     * 遇到错误时弹出警告弹窗，并打印警告信息
     * @param message 警告信息文本
     */
    public static void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 遇到错误时弹出警告弹窗，并打印警告信息
     * @param message 警告信息文本
     * @param exception 抛出异常
     */
    public static void showError(String message, Throwable exception) {
        StringBuilder messageBuilder = new StringBuilder(message);
        do {
            if (exception.getClass().getName().startsWith("com.group_finity.mascot.exception")) {
                messageBuilder.append("\n").append(exception.getMessage());
            } else if (exception instanceof SAXParseException) {
                messageBuilder.append("\n" + "Line ").append(((SAXParseException) exception).getLineNumber()).append(": ").append(exception.getMessage());
            } else {
                messageBuilder.append("\n").append(exception);
            }
            exception = exception.getCause();
        }
        while (exception != null);
        message = messageBuilder.toString();
        showError(message + "\n" + instance.languageBundle.getString("SeeLogForDetails"));
    }

    public static void main(String[] args) {
        try {
            instance.run();
        } catch (OutOfMemoryError err) {
            log.log(Level.SEVERE, "Out of memory. There are probably too many "
                    + "Shimeji mascots in the image folder for your computer to handle. "
                    + "Select fewer image sets or move some to the "
                    + "img/unused folder and try again.", err);
            showError("Out of memory. There are probably too many\n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the\n"
                    + "img/unused folder and try again.");
            System.exit(0);
        }
    }

    public void run() {
        frame = new JFrame();

        // 加载配置
        if (Files.isRegularFile(SETTINGS_FILE)) {
            try (InputStream input = Files.newInputStream(SETTINGS_FILE)) {
                properties.load(input);
            } catch(IOException e) {
                log.log(Level.SEVERE, "Failed to load settings", e);
            }
        }

        // 加载语言
        Locale locale = Locale.forLanguageTag(properties.getProperty("Language", Locale.UK.toLanguageTag())); // 设置语言，优先按配置文件的设置，否则默认设为英语
        try {
            URL[] urls = {CONFIG_DIRECTORY.toUri().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls)) {
                languageBundle = ResourceBundle.getBundle("language", locale, loader);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load language file for locale " + locale.toLanguageTag(), e);
            showError("The language file for locale " + locale.toLanguageTag() + " could not be loaded. Ensure that you have the latest Shimeji language.properties in your conf directory.");
            this.exit();
        }
    }

    public Properties getProperties() {
        return this.properties;
    }

    public ResourceBundle getLanguageBundle() {
        return this.languageBundle;
    }

    public void exit() {
        this.manager.disposeAll();
        this.manager.stop();
        System.exit(0);
    }

}