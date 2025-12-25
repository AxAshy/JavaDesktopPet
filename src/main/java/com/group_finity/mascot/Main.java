package com.group_finity.mascot;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.*;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.sound.Sounds;

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
    /**线程安全的哈希表-子图集合，key是主图片集的名称，value是子图片集的列表，
     * 如果一个 mascot "Mascot1" 能够变换成另一个 mascot 类型 "Mascot2", Mascot2 就会作为 Mascot1 的子图片集存储. */
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
    /**JDialog 是Java Swing 库提供的一个用于创建对话框窗口的组件，可以在应用程序中显示模态或非模态的自定义对话框。
     * 这是一个函数变量，用一个在不同的函数中付给新创建的窗口，然后在交互事件开始前重置
     */
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
            exit();
        }

        // 获取使用的图像集合
        if (!Boolean.parseBoolean(properties.getProperty("AlwaysShowShimejiChooser", "false"))) { // 判断是否每次都展示图像集选择窗口
            // 如果配置文件里有指定使用的图像集
            for (String set : properties.getProperty("ActiveShimeji", "").split("/")) {
                if (!set.trim().isEmpty()) {
                    imageSets.add(set.trim());
                }
            }
        }
        // 一直循环直到获得可用的图像集
        do {
            if (imageSets.isEmpty()) {
                // 创建一个图像集选择器
                imageSets = new ImageSetChooser(frame, true).display();
                // 让用户选择后还是为空, 就直接退出程序
                if (imageSets == null) {
                    exit();
                }
            }

            // 遍历图片集, 加载所有的 mascot configurations
            for (int index =0; index < imageSets.size(); index++) {
                // 确认图片集的配置能够正常加载,加载失败的删掉
                if (!loadConfiguration(imageSets.get(index))) {
                    configurations.remove(imageSets.get(index));
                    imageSets.remove(imageSets.get(index));
                    index--;
                }
            }
        }
        while (imageSets.isEmpty());

        // Create the tray icon
        createTrayIcon();

        // Create mascots
        for (String imageSet : imageSets) {
            String informationAlreadySeen = properties.getProperty("InformationDismissed", "");
            if (configurations.get(imageSet).containsInformationKey("SplashImage") &&
                    (Boolean.parseBoolean(properties.getProperty("AlwaysShowInformationScreen", "false")) ||
                            !informationAlreadySeen.contains(imageSet))) {
                InformationWindow info = new InformationWindow();
                info.init(imageSet, configurations.get(imageSet));
                info.display();
                setMascotInformationDismissed(imageSet);
                updateConfigFile();
            }
            createMascot(imageSet);
        }

        manager.start();
    }

     /**
     * Loads the configuration files for the given image set.
     *
     * @param imageSet the image set to load
     */
    private boolean loadConfiguration(final String imageSet) {
        try {
            // Determine actions file, 注意一下路径的覆盖, 即<imageSetName>/conf > conf/<imageSetName> > conf
            // 先在conf目录下查找
            Path filePath = CONFIG_DIRECTORY;
            Path actionsFile = filePath.resolve("actions.xml");

            // 在conf/<imageSetName>目录下查找
            filePath = CONFIG_DIRECTORY.resolve(imageSet);
            if (Files.exists(filePath.resolve("actions.xml"))) {
                actionsFile = filePath.resolve("actions.xml");
            // 下面这些就是不同可能命名方式的文件路径
            } else if (Files.exists(filePath.resolve("one.xml"))) {
                actionsFile = filePath.resolve("one.xml");
            } else if (Files.exists(filePath.resolve("1.xml"))) {
                actionsFile = filePath.resolve("1.xml");
            }

            // 在<imageSetName>/conf目录下查找
            filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
            if (Files.exists(filePath.resolve("actions.xml"))) {
                actionsFile = filePath.resolve("actions.xml");
            } else if (Files.exists(filePath.resolve("one.xml"))) {
                actionsFile = filePath.resolve("one.xml");
            } else if (Files.exists(filePath.resolve("1.xml"))) {
                actionsFile = filePath.resolve("1.xml");
            }

            log.log(Level.INFO, "Reading action file \"{0}\" for image set \"{1}\"", new Object[]{actionsFile, imageSet});
            // 解析actions.xml文件
            final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Files.newInputStream(actionsFile));
            Configuration configuration = new Configuration();
            configuration.load(new Entry(actions.getDocumentElement()), imageSet); // 加载actions配置
            // Save the schema for the actions file so we can use it later
            ResourceBundle actionsSchema = configuration.getSchema();

            // Determine behaviours file
            filePath = CONFIG_DIRECTORY;
            Path behaviorsFile = filePath.resolve("behaviors.xml");

            filePath = CONFIG_DIRECTORY.resolve(imageSet);
            if (Files.exists(filePath.resolve("behaviors.xml"))) {
                behaviorsFile = filePath.resolve("behaviors.xml");
            } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                behaviorsFile = filePath.resolve("behavior.xml");
            } else if (Files.exists(filePath.resolve("two.xml"))) {
                behaviorsFile = filePath.resolve("two.xml");
            } else if (Files.exists(filePath.resolve("2.xml"))) {
                behaviorsFile = filePath.resolve("2.xml");
            }

            filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
            if (Files.exists(filePath.resolve("behaviors.xml"))) {
                behaviorsFile = filePath.resolve("behaviors.xml");
            } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                behaviorsFile = filePath.resolve("behavior.xml");
            } else if (Files.exists(filePath.resolve("two.xml"))) {
                behaviorsFile = filePath.resolve("two.xml");
            } else if (Files.exists(filePath.resolve("2.xml"))) {
                behaviorsFile = filePath.resolve("2.xml");
            }

            log.log(Level.INFO, "Reading behavior file \"{0}\" for image set \"{1}\"", new Object[]{behaviorsFile, imageSet});

            final Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    Files.newInputStream(behaviorsFile));

            configuration.load(new Entry(behaviors.getDocumentElement()), imageSet);

            // Determine info file
            filePath = CONFIG_DIRECTORY;
            Path infoFile = filePath.resolve("info.xml");

            filePath = CONFIG_DIRECTORY.resolve(imageSet);
            if (Files.exists(filePath.resolve("info.xml"))) {
                infoFile = filePath.resolve("info.xml");
            }

            filePath = IMAGE_DIRECTORY.resolve(imageSet).resolve(CONFIG_DIRECTORY);
            if (Files.exists(filePath.resolve("info.xml"))) {
                infoFile = filePath.resolve("info.xml");
            }

            if (Files.exists(infoFile)) {
                log.log(Level.INFO, "Reading information file \"{0}\" for image set \"{1}\"", new Object[]{infoFile, imageSet});

                final Document information = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Files.newInputStream(infoFile));

                configuration.load(new Entry(information.getDocumentElement()), imageSet);
            }


            configuration.validate();
            configurations.put(imageSet, configuration);


            ArrayList<String> childMascots = new ArrayList<>();
            // mascot的分类代码在这里执行
            for (final Entry list : new Entry(actions.getDocumentElement()).selectChildren(actionsSchema.getString("ActionList"))) {
                for (final Entry node : list.selectChildren(actionsSchema.getString("Action"))) {
                    if (node.getAttributes().containsKey(actionsSchema.getString("BornMascot"))) {
                        String set = node.getAttribute(actionsSchema.getString("BornMascot"));
                        if (!childMascots.contains(set)) {
                            childMascots.add(set);
                        }
                        if (!configurations.containsKey(set)) {
                            loadConfiguration(set);
                        }
                    }
                    if (node.getAttributes().containsKey(actionsSchema.getString("TransformMascot"))) {
                        String set = node.getAttribute(actionsSchema.getString("TransformMascot"));
                        if (!childMascots.contains(set)) {
                            childMascots.add(set);
                        }
                        if (!configurations.containsKey(set)) {
                            loadConfiguration(set);
                        }
                    }
                }
            }
            childImageSets.put(imageSet, childMascots);

            return true;

        } catch (ConfigurationException | IOException | ParserConfigurationException | SAXException e) {
            log.log(Level.SEVERE, "Failed to load configuration files", e);
            showError(languageBundle.getString("FailedLoadConfigErrorMessage"), e);
        }
        return false;
    }

    /**
     * Creates a tray icon.
     */
    private void createTrayIcon() {
        if (!SystemTray.isSupported()) {
            log.log(Level.INFO, "System tray not supported");
            return;
        }
        log.log(Level.INFO, "Creating tray icon");

        // get the tray icon image
        BufferedImage image = getIcon();

        try {
            // 程序的名称, 也就是工具提示的那一串字符
            String tooltip = properties.getProperty("ShimejiEENameOverride", "").trim();
            if (tooltip.isEmpty()) {
                tooltip = languageBundle.getString("ShimejiEE");
            }
            // Create the tray icon
            final TrayIcon icon = new TrayIcon(image, tooltip);
            icon.setImageAutoSize(true);

            // 为托盘绑定菜单
            icon.addMouseListener(new MouseListener() {
                /*Debounce 的中文是防抖。它是一种优化函数执行频率的技术，具体来说，它会在函数被触发后，等待一段设定的时间。
                只有在这段时间内没有再次触发，函数才会最终执行。如果在这段时间内又发生了触发，那么计时将重新开始。 */
                final AtomicBoolean debouncing = new AtomicBoolean(false); // 线程安全的Boolean
                final Timer debounceTimer = new Timer(1000, event -> debouncing.set(false));

                // 处理点击事件的方法
                @Override
                public void mouseClicked(MouseEvent e) {
                    // 返回deboucing的当前值,如果还在debounceTimer的延迟时间内,就不执行事件处理
                    if (debouncing.get()) { 
                        return;
                    }

                    // 重置debounceTimer计时器
                    debouncing.set(true);
                    debounceTimer.setRepeats(false);
                    debounceTimer.restart();

                    if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                        // 左键点击托盘icon就生成新的mascot
                        createMascot();
                    } else if (SwingUtilities.isMiddleMouseButton(e) && e.getClickCount() == 2) {
                        // 当托盘icon被两次中键点击, 就处理掉所有的 mascots, 但是不要关闭程序
                        if (manager.isExitOnLastRemoved()) {
                            // 如果manager.isExitOnLastRemoved设为了true, 这里就要更新为false
                            manager.setExitOnLastRemoved(false);
                            manager.disposeAll(); // 让manager处理掉所有的mascots
                        } else {
                        // 如果所有的mascots已经消失, 给每一个启用的image set创建一个mascot
                            for (String imageSet : imageSets) {
                                createMascot(imageSet);
                                manager.setExitOnLastRemoved(true);
                            }
                        }
                    }
                }

                // 处理长按鼠标事件的方法
                @Override
                public void mousePressed(MouseEvent e) {
                    // 确认鼠标popup事件在 mousePressed and mouseReleased 时都会被触发
                    // 因为不同的系统间可能有所不同
                    if (e.isPopupTrigger()) {
                        onPopupTrigger(e);
                    }
                }

                // 处理放开鼠标事件的方法
                @Override
                public void mouseReleased(MouseEvent e) {
                    // Check for popup triggers in both mousePressed and mouseReleased
                    // because it works differently on different systems
                    if (e.isPopupTrigger()) {
                        onPopupTrigger(e);
                    }
                }

                /**
                 * 弹出窗口
                 * @param e mousePressed 或 mouseReleased 时触发的事件
                 */
                private void onPopupTrigger(MouseEvent event) {
                    // 如果窗口是打开的, 就关闭窗口
                    if (form != null) {
                        form.dispose();
                    }

                    // 这里是点击托盘icon后的弹窗
                    form = new JDialog(frame, false);
                    final JPanel panel = new JPanel();
                    panel.setBorder(new EmptyBorder(5, 5, 5, 5));
                    form.add(panel); // 添加容器面板

                    // 生成按钮并处理对应的行动监听
                    // 召唤Shimeji
                    JButton btnCallShimeji = new JButton(languageBundle.getString("CallShimeji"));
                    btnCallShimeji.addActionListener(event17 -> {
                        createMascot();
                        form.dispose(); // 执行完后关闭托盘弹窗
                    });

                    // 跟随鼠标
                    JButton btnFollowCursor = new JButton(languageBundle.getString("FollowCursor"));
                    btnFollowCursor.addActionListener(event16 -> {
                        manager.setBehaviorAll("ChaseMouse");
                        form.dispose();
                    });

                    // 仅剩一个
                    JButton btnReduceToOne = new JButton(languageBundle.getString("ReduceToOne"));
                    btnReduceToOne.addActionListener(event15 -> {
                        manager.remainOne();
                        form.dispose();
                    });

                    // 窗口重置
                    JButton btnRestoreWindows = new JButton(languageBundle.getString("RestoreWindows"));
                    btnRestoreWindows.addActionListener(event14 -> {
                        NativeFactory.getInstance().getEnvironment().restoreIE();
                        form.dispose();
                    });

                    // BEGIN-允许所有行为
                    final JButton btnAllowedBehaviours = new JButton(languageBundle.getString("AllowedBehaviours"));
                    // 给btnAllowedBehaviours按钮添加鼠标事件监听
                    btnAllowedBehaviours.addMouseListener(new MouseListener() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                        }

                        @Override
                        public void mousePressed(MouseEvent e) {
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            btnAllowedBehaviours.setEnabled(true); // 将按钮变为可用
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                        }
                    });
                    // BEGIN-给btnAllowedBehaviours按钮添加单机按钮或双击列表项时触发的事件
                    btnAllowedBehaviours.addActionListener(event13 -> {
                        // 生成一个新的弹出菜单, 可以在一个组件内的指定位置动态地弹出
                        JPopupMenu behaviourPopup = new JPopupMenu();
                        behaviourPopup.show(btnAllowedBehaviours, 0, btnAllowedBehaviours.getHeight());
                        
                        // 下面开始增加弹出菜单的下拉项
                        // "禁止克隆" 菜单项
                        final JCheckBoxMenuItem breedingMenu = new JCheckBoxMenuItem(languageBundle.getString("BreedingCloning"), Boolean.parseBoolean(properties.getProperty("Breeding", "true")));
                        breedingMenu.addItemListener(e -> {
                            breedingMenu.setState(toggleBooleanSetting("Breeding", true)); // 把Breeding配置设为false
                            updateConfigFile(); // 更新配置文件到本地
                            btnAllowedBehaviours.setEnabled(true); // 将"允许所有行为"按钮设为可用, 因为又一项行为——Breeding被禁用了
                        });
                        behaviourPopup.add(breedingMenu);

                        // "禁止临时生成物" 菜单项
                        final JCheckBoxMenuItem transientMenu = new JCheckBoxMenuItem(languageBundle.getString("BreedingTransient"), Boolean.parseBoolean(properties.getProperty("Transients", "true")));
                        transientMenu.addItemListener(e -> {
                            transientMenu.setState(toggleBooleanSetting("Transients", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });
                        behaviourPopup.add(transientMenu);

                        // "禁止变换" 菜单项
                        final JCheckBoxMenuItem transformationMenu = new JCheckBoxMenuItem(languageBundle.getString("Transformation"), Boolean.parseBoolean(properties.getProperty("Transformation", "true")));
                        transformationMenu.addItemListener(e -> {
                            transformationMenu.setState(toggleBooleanSetting("Transformation", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });
                        behaviourPopup.add(transformationMenu);

                        // "投掷窗口" menu item
                        final JCheckBoxMenuItem throwingMenu = new JCheckBoxMenuItem(languageBundle.getString("ThrowingWindows"), Boolean.parseBoolean(properties.getProperty("Throwing", "true")));
                        throwingMenu.addItemListener(e -> {
                            throwingMenu.setState(toggleBooleanSetting("Throwing", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });
                        behaviourPopup.add(throwingMenu);

                        // "静音" menu item
                        final JCheckBoxMenuItem soundsMenu = new JCheckBoxMenuItem(languageBundle.getString("SoundEffects"), Boolean.parseBoolean(properties.getProperty("Sounds", "true")));
                        soundsMenu.addItemListener(e -> {
                            boolean result = toggleBooleanSetting("Sounds", true);
                            soundsMenu.setState(result);
                            Sounds.setMuted(!result);
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });
                        behaviourPopup.add(soundsMenu);

                        // "在屏幕间移动" menu item
                        final JCheckBoxMenuItem multiscreenMenu = new JCheckBoxMenuItem(languageBundle.getString("Multiscreen"), Boolean.parseBoolean(properties.getProperty("Multiscreen", "true")));
                        multiscreenMenu.addItemListener(e -> {
                            multiscreenMenu.setState(toggleBooleanSetting("Multiscreen", true));
                            updateConfigFile();
                            btnAllowedBehaviours.setEnabled(true);
                        });
                        behaviourPopup.add(multiscreenMenu);

                        // 再给弹窗添加弹窗事件监听器，覆写在弹窗消失时的事件处理方法
                        behaviourPopup.addPopupMenuListener(new PopupMenuListener() {
                            @Override
                            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            }

                            @Override
                            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                if (panel.getMousePosition() != null) {
                                    btnAllowedBehaviours.setEnabled(!(panel.getMousePosition().x > btnAllowedBehaviours.getX() &&
                                            panel.getMousePosition().x < btnAllowedBehaviours.getX() + btnAllowedBehaviours.getWidth() &&
                                            panel.getMousePosition().y > btnAllowedBehaviours.getY() &&
                                            panel.getMousePosition().y < btnAllowedBehaviours.getY() + btnAllowedBehaviours.getHeight()));
                                } else {
                                    btnAllowedBehaviours.setEnabled(true);
                                }
                            }

                            @Override
                            public void popupMenuCanceled(PopupMenuEvent e) {
                            }
                        });
                        btnAllowedBehaviours.requestFocusInWindow();
                    });
                    // END-给btnAllowedBehaviours按钮添加单机按钮或双击列表项时触发的事件
                    // END-允许所有行为

                    // 选择Shimeji
                    final JButton btnChooseShimeji = new JButton(languageBundle.getString("ChooseShimeji"));
                    // 给btnChooseShimeji按钮添加单机按钮或双击列表项时触发的事件
                    btnChooseShimeji.addActionListener(event12 -> {
                        form.dispose();
                        if (!manager.isPaused()) {
                            // Needed to stop the guys from potentially throwing away the image set chooser window
                            manager.togglePauseAll();
                        }

                        ImageSetChooser chooser = new ImageSetChooser(frame, true);
                        chooser.setIconImage(icon.getImage());
                        Collection<String> result = chooser.display(); // 用户选择的结果

                        if (manager.isPaused()) {
                            // Unpause them before setting the active image sets to be sure they actually get unpaused
                            manager.togglePauseAll();
                        }
                        setActiveImageSets(result); // 几乎用户选择要激活的图片集
                    });

                    // BEGIN-设置按钮
                    final JButton btnSettings = new JButton(languageBundle.getString("Settings"));
                    btnSettings.addActionListener(event1 -> {
                        form.dispose();
                        if (!manager.isPaused()) {
                            // Needed to stop the guys from potentially throwing away the settings window
                            manager.togglePauseAll();
                        }

                        // 创建专门的设置窗口
                        SettingsWindow dialog = new SettingsWindow(frame, true);
                        dialog.setIconImage(icon.getImage());
                        dialog.init();
                        dialog.display();

                        if (dialog.getEnvironmentReloadRequired()) {
                            NativeFactory.getInstance().getEnvironment().dispose();
                            NativeFactory.resetInstance();
                        }
                        if (dialog.getEnvironmentReloadRequired() || dialog.getImageReloadRequired()) {
                            // need to reload the shimeji as the images have rescaled
                            boolean isExit = manager.isExitOnLastRemoved();
                            manager.setExitOnLastRemoved(false);
                            manager.disposeAll();

                            // Wipe all loaded data
                            ImagePairs.clear();
                            configurations.clear();

                            // Load settings
                            for (String imageSet : imageSets) {
                                loadConfiguration(imageSet);
                            }

                            // Create the first mascot
                            for (String imageSet : imageSets) {
                                createMascot(imageSet);
                            }

                            manager.setExitOnLastRemoved(isExit);
                        } else {
                            if (manager.isPaused()) {
                                manager.togglePauseAll();
                            }
                        }
                        if (dialog.getInteractiveWindowReloadRequired()) {
                            NativeFactory.getInstance().getEnvironment().refreshCache();
                        }
                    });
                    // END-设置按钮

                    // BEGIN-语言设置按钮
                    final JButton btnLanguage = new JButton(languageBundle.getString("Language"));
                    btnLanguage.addMouseListener(new MouseListener() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                        }

                        @Override
                        public void mousePressed(MouseEvent e) {
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            btnLanguage.setEnabled(true);
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                        }
                    });
                    btnLanguage.addActionListener(e -> {
                        JPopupMenu languagePopup = new JPopupMenu();
                        // English menu item
                        final JMenuItem englishMenu = new JMenuItem("English");
                        englishMenu.addActionListener(e121 -> {
                            form.dispose();
                            updateLanguage(Locale.UK);
                            updateConfigFile();
                        });
                        languagePopup.add(englishMenu);
                        languagePopup.addSeparator();

                        // Chinese menu item
                        final JMenuItem chineseMenu = new JMenuItem("\u7b80\u4f53\u4e2d\u6587");
                        chineseMenu.addActionListener(e14 -> {
                            form.dispose();
                            updateLanguage(Locale.SIMPLIFIED_CHINESE);
                            updateConfigFile();
                        });
                        languagePopup.add(chineseMenu);

                        languagePopup.addPopupMenuListener(new PopupMenuListener() {
                            @Override
                            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            }

                            @Override
                            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                                if (panel.getMousePosition() != null) {
                                    btnLanguage.setEnabled(!(panel.getMousePosition().x > btnLanguage.getX() &&
                                            panel.getMousePosition().x < btnLanguage.getX() + btnLanguage.getWidth() &&
                                            panel.getMousePosition().y > btnLanguage.getY() &&
                                            panel.getMousePosition().y < btnLanguage.getY() + btnLanguage.getHeight()));
                                } else {
                                    btnLanguage.setEnabled(true);
                                }
                            }

                            @Override
                            public void popupMenuCanceled(PopupMenuEvent e) {
                            }
                        });
                        languagePopup.show(btnLanguage, 0, btnLanguage.getHeight());
                        btnLanguage.requestFocusInWindow();
                    });
                    // END-语言设置按钮

                    // 恢复动画按钮
                    JButton btnPauseAll = new JButton(manager.isPaused() ? languageBundle.getString("ResumeAnimations") : languageBundle.getString("PauseAnimations"));
                    btnPauseAll.addActionListener(e -> {
                        form.dispose();
                        manager.togglePauseAll();
                    });

                    // 关闭程序按钮
                    JButton btnDismissAll = new JButton(languageBundle.getString("DismissAll"));
                    btnDismissAll.addActionListener(e -> exit());

                    // layout
                    panel.setLayout(new GridBagLayout());
                    GridBagConstraints gridBag = new GridBagConstraints();
                    gridBag.fill = GridBagConstraints.HORIZONTAL;
                    gridBag.gridx = 0;
                    gridBag.gridy = 0;
                    panel.add(btnCallShimeji, gridBag);
                    gridBag.insets = new Insets(5, 0, 0, 0);
                    gridBag.gridy++;
                    panel.add(btnFollowCursor, gridBag);
                    gridBag.gridy++;
                    panel.add(btnReduceToOne, gridBag);
                    gridBag.gridy++;
                    panel.add(btnRestoreWindows, gridBag);
                    gridBag.gridy++;
                    panel.add(new JSeparator(), gridBag);
                    gridBag.gridy++;
                    panel.add(btnAllowedBehaviours, gridBag);
                    gridBag.gridy++;
                    panel.add(btnChooseShimeji, gridBag);
                    gridBag.gridy++;
                    panel.add(btnSettings, gridBag);
                    gridBag.gridy++;
                    panel.add(btnLanguage, gridBag);
                    gridBag.gridy++;
                    panel.add(new JSeparator(), gridBag);
                    gridBag.gridy++;
                    panel.add(btnPauseAll, gridBag);
                    gridBag.gridy++;
                    panel.add(btnDismissAll, gridBag);

                    form.setIconImage(icon.getImage());
                    form.setTitle(icon.getToolTip());
                    form.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    form.setAlwaysOnTop(true);

                    // set the form dimensions
                    FontMetrics metrics = btnCallShimeji.getFontMetrics(btnCallShimeji.getFont());
                    int width = metrics.stringWidth(btnCallShimeji.getText());
                    width = Math.max(metrics.stringWidth(btnFollowCursor.getText()), width);
                    width = Math.max(metrics.stringWidth(btnReduceToOne.getText()), width);
                    width = Math.max(metrics.stringWidth(btnRestoreWindows.getText()), width);
                    width = Math.max(metrics.stringWidth(btnAllowedBehaviours.getText()), width);
                    width = Math.max(metrics.stringWidth(btnChooseShimeji.getText()), width);
                    width = Math.max(metrics.stringWidth(btnSettings.getText()), width);
                    width = Math.max(metrics.stringWidth(btnLanguage.getText()), width);
                    width = Math.max(metrics.stringWidth(btnPauseAll.getText()), width);
                    width = Math.max(metrics.stringWidth(btnDismissAll.getText()), width);
                    panel.setPreferredSize(new Dimension(width + 64,
                            24 + // 12 padding on top and bottom
                                    75 + // 13 insets of 5 height normally
                                    10 * metrics.getHeight() + // 10 button faces
                                    84));
                    form.pack();
                    form.setMinimumSize(form.getSize());

                    // get the DPI of the screen, and divide 96 by it to get a ratio
                    double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();

                    // setting location of the form
                    form.setLocation((int) Math.round(event.getX() * dpiScaleInverse) - form.getWidth(), (int) Math.round(event.getY() * dpiScaleInverse) - form.getHeight());

                    // make sure that it is on the screen if people are using exotic taskbar locations
                    Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    if (form.getX() < screen.getX()) {
                        form.setLocation((int) Math.round(event.getX() * dpiScaleInverse), form.getY());
                    }
                    if (form.getY() < screen.getY()) {
                        form.setLocation(form.getX(), (int) Math.round(event.getY() * dpiScaleInverse));
                    }

                    form.setVisible(true);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}

            });

            // Show tray icon
            SystemTray.getSystemTray().add(icon);

        } catch (final AWTException e) {
            log.log(Level.SEVERE, "Failed to create tray icon", e);
            // TODO: Figure out whether it is better to use the showError(String, Throwable) method for this
            showError(languageBundle.getString("FailedDisplaySystemTrayErrorMessage") + "\n" + languageBundle.getString("SeeLogForDetails"));
            exit();
        }
    }

    /**
     * Creates a random {@link Mascot}.
     */
    public void createMascot() {
        int length = imageSets.size();
        int random = (int) (length * Math.random());
        createMascot(imageSets.get(random));
    }

    /**
     * Creates a {@link Mascot} with the specified image set.
     *
     * @param imageSet the image set to use
     */
    public void createMascot(String imageSet) {
        log.log(Level.INFO, "Creating mascot with image set \"{0}\"", imageSet);

        // 创建一个 mascot
        final Mascot mascot = new Mascot(imageSet);

        // 在屏幕的边界外放置
        mascot.setAnchor(new Point(-4000, -4000));

        // Randomize the initial orientation
        mascot.setLookRight(Math.random() < 0.5);

        try {
            mascot.setBehavior(getConfiguration(imageSet).buildNextBehavior(null, mascot));
            manager.add(mascot);
        } catch (final BehaviorInstantiationException e) {
            // Not sure why this says "first action" instead of "first behavior", but changing it would require changing all of the translations, so...
            log.log(Level.SEVERE, "Failed to initialize the first action for mascot \"" + mascot + "\"", e);
            showError(languageBundle.getString("FailedInitialiseFirstActionErrorMessage"), e);
            mascot.dispose();
        } catch (final CantBeAliveException e) {
            log.log(Level.SEVERE, "Could not create mascot \"" + mascot + "\"", e);
            showError(languageBundle.getString("FailedInitialiseFirstActionErrorMessage"), e);
            mascot.dispose();
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Could not create mascot \"" + mascot + "\"", e);
            showError(languageBundle.getString("CouldNotCreateShimejiErrorMessage") + " " + imageSet, e);
            mascot.dispose();
        }
    }

    /**
     * 刷新语言包{@code languageBundle}
     * @param locale 指定的语言包
     */
    private void refreshLanguage(Locale locale) {
        try {
            URL[] urls = {CONFIG_DIRECTORY.toUri().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls)) {
                languageBundle = ResourceBundle.getBundle("language", locale, loader);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load language file for locale " + locale.toLanguageTag(), e);
            showError("The language file for locale " + locale.toLanguageTag() + " could not be loaded. Ensure that you have the latest Shimeji language.properties in your conf directory.");
            exit();
        }

        boolean isExit = manager.isExitOnLastRemoved();
        manager.setExitOnLastRemoved(false);
        manager.disposeAll();

        // Load mascot configurations
        for (String imageSet : imageSets) {
            loadConfiguration(imageSet);
        }

        // Create mascots
        for (String imageSet : imageSets) {
            createMascot(imageSet);
        }

        manager.setExitOnLastRemoved(isExit);
    }

    /**
     * 更新语言设置到本地{@code settings.properties}文件，并调用 {@link refreshLanguage} 刷新语言包
     * @param locale 指定的语言包
     */
    private void updateLanguage(Locale locale) {
        if (!properties.getProperty("Language", Locale.UK.toLanguageTag()).equals(locale.toLanguageTag())) {
            properties.setProperty("Language", locale.toLanguageTag());
            refreshLanguage(locale);
        }
    }

    /**
     * 更新语言设置到本地{@code settings.properties}文件，并调用 {@link refreshLanguage} 刷新语言包
     * @param languageTag 指定的语言包
     */
    private void updateLanguage(String languageTag) {
        if (!properties.getProperty("Language", Locale.UK.toLanguageTag()).equals(languageTag)) {
            properties.setProperty("Language", languageTag);
            refreshLanguage(Locale.forLanguageTag(languageTag));
        }
    }

    /**
     * 将给定的配置项名称对应配置值(boolean类型)调为相反值, 可用想象成拨一下开关. 如果不存在该配置项, 就设为默认值, 然后再设为默认值的相反值.
     * 例如: toggleBooleanSetting("breeding", true), breeding配置不存在, 返回true, 再把properties中的breeding配置为false
     * @param propertyName 给定的配置项名称
     * @param defaultValue 如果配置项不存在在properties中, 需要设为的默认值
     * @return boolean 设置后的配置值
     */
    private boolean toggleBooleanSetting(String propertyName, boolean defaultValue) {
        if (Boolean.parseBoolean(properties.getProperty(propertyName, String.valueOf(defaultValue)))) {
            properties.setProperty(propertyName, "false");
            return false;
        } else {
            properties.setProperty(propertyName, "true");
            return true;
        }
    }

    /**
     * 忽略指定{@code Mascot}的指定信息，忽略的信息在settings.properties中使用"InformationDismissed"设置
     * @param imageSet 指定{@code Mascot}的名称
     */
    private void setMascotInformationDismissed(final String imageSet) {
        ArrayList<String> list = new ArrayList<>();
        String[] data = properties.getProperty("InformationDismissed", "").split("/");

        if (data.length > 0 && !data[0].isEmpty()) {
            list.addAll(Arrays.asList(data));
        }
        if (!list.contains(imageSet)) {
            list.add(imageSet);
        }

        properties.setProperty("InformationDismissed", list.toString().replace("[", "").replace("]", "").replace(", ", "/"));
    }

    /**
     * 将指定{@code Mascot}的行为设为指定的true/false状态
     * @param name 只能定的{@code Behavior}名称
     * @param mascot 指定{@code Mascot}的名称
     * @param enabled true/false
     */
    public void setMascotBehaviorEnabled(final String name, final Mascot mascot, boolean enabled) {
        ArrayList<String> list = new ArrayList<>();
        String[] data = properties.getProperty("DisabledBehaviours." + mascot.getImageSet(), "").split("/");

        if (data.length > 0 && !data[0].isEmpty()) {
            list.addAll(Arrays.asList(data));
        }

        if (list.contains(name) && enabled) {
            list.remove(name);
        } else if (!list.contains(name) && !enabled) {
            list.add(name);
        }

        if (list.isEmpty()) {
            properties.remove("DisabledBehaviours." + mascot.getImageSet());
        } else {
            properties.setProperty("DisabledBehaviours." + mascot.getImageSet(), list.toString().replace("[", "").replace("]", "").replace(", ", "/"));
        }

        updateConfigFile();
    }
    
    /**
     * 将当前的properties存储到settings.properties本地文件中
     */
    private void updateConfigFile() {
        try (OutputStream output = Files.newOutputStream(SETTINGS_FILE)) {
            properties.store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to save settings", e);
        }
    }

    /**
     * 在不修改当前已经激活的有效图片集的情况下激活或更新{@code newImageSets}中的图片集，如果 {@code newImageSets == null} 就什么都不做
     * @param newImageSets 所有应该被激活的图片集
     */
    private void setActiveImageSets(Collection<String> newImageSets) {
        if (newImageSets == null) {
            return;
        }

        // I don't think there would be enough image sets chosen at any given
        // time for it to be worth using HashSet, but I might be wrong
        Collection<String> toRemove = new ArrayList<>(imageSets); // 要移除的图片集列表先复制原来的全量imageSets
        toRemove.removeAll(newImageSets); // 再把要调用的newImageSets剔除掉

        Collection<String> toAdd = new ArrayList<>();
        ArrayList<String> toRetain = new ArrayList<>();
        for (String set : newImageSets) {
            if (!imageSets.contains(set)) {
                toAdd.add(set);
            }
            if (!toRetain.contains(set)) {
                toRetain.add(set);
            }
            // 把图片集的子集加入到要保留的图片集中，因为子图片集只有在父图片集被移除时才能被移除
            populateArrayListWithChildSets(set, toRetain);

            boolean isExit = manager.isExitOnLastRemoved(); // 先获取程序原来的isExitOnLastRemoved属性
            manager.setExitOnLastRemoved(false); // 然后将isExitOnLastRemoved属性设为false，因为等下执行替换要删除图片集，可能导致程序关闭

            for (String r : toRemove)
                removeLoadedImageSet(r, toRetain);

            for (String a : toAdd)
                addImageSet(a);

            manager.setExitOnLastRemoved(isExit); // 把manager的isExitOnLastRemoved属性设为原来的值
        }
    }

    /**
     * 一个用于展开多层图片集的迭代函数
     * @param imageSet 待处理的包含子集的图片集名称
     * @param childList 展开的图片集存放的列表
     */
    private void populateArrayListWithChildSets(String imageSet, ArrayList<String> childList) {
        if (childImageSets.contains(imageSet)) {
            for (String set: childImageSets.get(imageSet)) {
                if (!childList.contains(set)) {
                    populateArrayListWithChildSets(set, childList);
                    childList.add(set);
                }
            }
        }
    }

    /**
     * 从{@code imageSets}中移除{@ode imageSet}，但是由于子图片集只有在父图片集被移除时才能被移除，需要提供一个{@code setsToIgnore}来保证子集不会被意外移除
     * @param imageSet
     * @param setsToIgnore
     */
    private void removeLoadedImageSet(String imageSet, ArrayList<String> setsToIgnore) {
        /*
         * If a mascot "Mascot1" has the ability to transform into another mascot type "Mascot2", Mascot2 is stored as a child image set of Mascot1.
         * If Mascot2 is unchecked in the Shimeji chooser, the existing Mascot2 mascots will only be removed if no Mascot1 instances exist, because Mascot2 is a child of Mascot1.
         * It's confusing, but it prevents errors.
         */
        if (childImageSets.containsKey(imageSet)) {
            for (String set : childImageSets.get(imageSet)) {
                if (!setsToIgnore.contains(set)) {
                    setsToIgnore.add(set);
                    imageSets.remove(imageSet);
                    manager.remainNone(imageSet);
                    configurations.remove(imageSet);
                    ImagePairs.removeAll(imageSet);
                    removeLoadedImageSet(set, setsToIgnore);
                }
            }
        }

        if (!setsToIgnore.contains(imageSet)) {
            imageSets.remove(imageSet);
            manager.remainNone(imageSet);
            configurations.remove(imageSet);
            ImagePairs.removeAll(imageSet);
        }
    }

    private void addImageSet(String imageSet) {
        if (configurations.containsKey(imageSet)) {
            imageSets.add(imageSet);
            createMascot(imageSet);
        } else {
            if (loadConfiguration(imageSet)) {
                imageSets.add(imageSet);
                String informationAlreadySeen = properties.getProperty("InformationDismissed", "");
                if (configurations.get(imageSet).containsInformationKey("SplashImage") &&
                        (Boolean.parseBoolean(properties.getProperty("AlwaysShowInformationScreen", "false")) ||
                                !informationAlreadySeen.contains(imageSet))) {
                    InformationWindow info = new InformationWindow();
                    info.init(imageSet, configurations.get(imageSet));
                    info.display();
                    setMascotInformationDismissed(imageSet);
                    updateConfigFile();
                }
                createMascot(imageSet);
            } else {
                // conf failed
                configurations.remove(imageSet); // maybe move this to the loadConfig catch
            }
        }
    }
    
    /**
     * 返回指定图片集的Configuration配置
     */
    public Configuration getConfiguration(String imageSet) {
        return configurations.get(imageSet);
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

    /**
     * Loads the icon file and returns it as a {@link BufferedImage}.
     * If a custom icon has been placed at the path {@code img/icon.png}, then it will be loaded. Otherwise, the default
     * icon will be loaded.
     *
     * @return The loaded {@link BufferedImage} icon, or a blank image if loading fails.
     */
    public static BufferedImage getIcon() {
        if (icon != null) {return icon;}

        if (Files.exists(ICON_FILE)) {
            try {
                icon = ImageIO.read(Files.newInputStream(ICON_FILE));
                return icon;
            } catch (final IOException e) {
                log.log(Level.WARNING, "Failed to load custom icon file", e);
            }
        }

        // 从resource文件夹读取Icon
        try {
            icon = ImageIO.read(Objects.requireNonNull(Main.class.getResourceAsStream("/icon.png")));
            return icon;
        } catch (final IOException e) {
            log.log(Level.WARNING, "Failed to load default icon file", e);
        }

        // 加载失败就只能用空白图片表示
        icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        return icon;
    }

}
