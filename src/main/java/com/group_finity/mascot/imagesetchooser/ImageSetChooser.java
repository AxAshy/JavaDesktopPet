package com.group_finity.mascot.imagesetchooser;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.ConfigurationException;

/**
 * 用于选择使用的图像集的选择器
 */
public class ImageSetChooser extends JDialog {
    private static final Logger log = Logger.getLogger(ImageSetChooser.class.getName());
    private final ArrayList<String> imageSets = new ArrayList<>();
    private boolean closeProgram = true; // 是否在关闭窗口后关闭整个程序
    private boolean selectAllSets = false; // Default all to selected

    /**
     * 创建ImageSetChooser实例
     * @param owner 顶层窗口/容器
     * @param modal 
     */
    public ImageSetChooser(Frame owner, boolean modal) {
        super(owner, modal);
        initComponents();
        setLocationRelativeTo(null);

        List<String> activeImageSets = readConfigFile(); // 从配置文件中读取启用的图片集

        List<ImageSetChooserPanel> data1 = new ArrayList<>();
        List<ImageSetChooserPanel> data2 = new ArrayList<>();
        Collection<Integer> si1 = new ArrayList<>();
        Collection<Integer> si2 = new ArrayList<>();

        // Get list of image sets (directories under img)
        // 过滤不适用的图片集
        DirectoryStream.Filter<Path> filter = entry -> {
            String fileName = entry.getFileName().toString();
            if (fileName.equalsIgnoreCase("unused") || fileName.startsWith(".")) {
                return false;
            }
            return Files.isDirectory(entry);
        };
        try (DirectoryStream<Path> imageSetDirs = Files.newDirectoryStream(Main.IMAGE_DIRECTORY, filter)) {
            // Create ImageSetChooserPanels for ShimejiList
            boolean onList1 = true; // 在两个列表之间增加toggle(开关按钮)
            int row = 0; // 初始化当前行索引
            for (Path imageSetDir : imageSetDirs) {
                String imageSet = imageSetDir.getFileName().toString();

                // Determine actions file
                // 先在conf目录下查找
                Path filePath = Main.CONFIG_DIRECTORY;
                Path actionsFile = filePath.resolve("actions.xml");

                // 在conf/<imageSetName>目录下查找
                filePath = Main.CONFIG_DIRECTORY.resolve(imageSet);
                if (Files.exists(filePath.resolve("actions.xml"))) {
                    actionsFile = filePath.resolve("actions.xml");
                // 下面这些就是不同可能命名方式的文件路径
                } else if (Files.exists(filePath.resolve("one.xml"))) {
                    actionsFile = filePath.resolve("one.xml");
                } else if (Files.exists(filePath.resolve("1.xml"))) {
                    actionsFile = filePath.resolve("1.xml");
                }

                // 在<imageSetName>/conf目录下查找
                filePath = imageSetDir.resolve(Main.CONFIG_DIRECTORY);
                if (Files.exists(filePath.resolve("actions.xml"))) {
                    actionsFile = filePath.resolve("actions.xml");
                } else if (Files.exists(filePath.resolve("one.xml"))) {
                    actionsFile = filePath.resolve("one.xml");
                } else if (Files.exists(filePath.resolve("1.xml"))) {
                    actionsFile = filePath.resolve("1.xml");
                }

                // Determine behaviours file
                filePath = Main.CONFIG_DIRECTORY;
                Path behaviorsFile = filePath.resolve("behaviors.xml");

                filePath = Main.CONFIG_DIRECTORY.resolve(imageSet);
                if (Files.exists(filePath.resolve("behaviors.xml"))) {
                    behaviorsFile = filePath.resolve("behaviors.xml");
                } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                    behaviorsFile = filePath.resolve("behavior.xml");
                } else if (Files.exists(filePath.resolve("two.xml"))) {
                    behaviorsFile = filePath.resolve("two.xml");
                } else if (Files.exists(filePath.resolve("2.xml"))) {
                    behaviorsFile = filePath.resolve("2.xml");
                }

                filePath = imageSetDir.resolve(Main.CONFIG_DIRECTORY);
                if (Files.exists(filePath.resolve("behaviors.xml"))) {
                    behaviorsFile = filePath.resolve("behaviors.xml");
                } else if (Files.exists(filePath.resolve("behavior.xml"))) {
                    behaviorsFile = filePath.resolve("behavior.xml");
                } else if (Files.exists(filePath.resolve("two.xml"))) {
                    behaviorsFile = filePath.resolve("two.xml");
                } else if (Files.exists(filePath.resolve("2.xml"))) {
                    behaviorsFile = filePath.resolve("2.xml");
                }

                // Determine information file (这个文件是官方没有提供的?)
                filePath = Main.CONFIG_DIRECTORY;
                Path infoFile = filePath.resolve("info.xml");

                filePath = Main.CONFIG_DIRECTORY.resolve(imageSet);
                if (Files.exists(filePath.resolve("info.xml"))) {
                    infoFile = filePath.resolve("info.xml");
                }

                filePath = imageSetDir.resolve(Main.CONFIG_DIRECTORY);
                if (Files.exists(filePath.resolve("info.xml"))) {
                    infoFile = filePath.resolve("info.xml");
                }

                // 获取图片文件
                Path imageFile = imageSetDir.resolve("shime1.png");
                String caption = imageSet;
                try {
                    if (Files.exists(infoFile)) {
                        Configuration configuration = new Configuration();

                        // HTML 或 XML 文件
                        final Document information = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Files.newInputStream(infoFile));

                        configuration.load(new Entry(information.getDocumentElement()), imageSet);

                        // 从info.xml中加载图片集的属性
                        if (configuration.containsInformationKey(configuration.getSchema().getString("Name"))) {
                            caption = configuration.getInformation(configuration.getSchema().getString("Name"));
                        }
                        if (configuration.containsInformationKey(configuration.getSchema().getString("PreviewImage"))) {
                            imageFile = imageSetDir.resolve(configuration.getInformation(configuration.getSchema().getString("PreviewImage")));
                        }
                    }
                } catch (ConfigurationException | ParserConfigurationException | IOException | SAXException ex) {
                    imageFile = imageSetDir.resolve("shime1.png");
                    caption = imageSet;
                }

                if (onList1) {
                    onList1 = false;
                    data1.add(new ImageSetChooserPanel(imageSet, actionsFile.toString(), behaviorsFile.toString(), imageFile, caption));
                    // 这个图片集是否已经被选中了?
                    if (activeImageSets.contains(imageSet) || selectAllSets) {
                        si1.add(row);
                    }
                } else {
                    onList1 = true;
                    data2.add(new ImageSetChooserPanel(imageSet, actionsFile.toString(),
                            behaviorsFile.toString(), imageFile, caption));
                    // Is this set initially selected?
                    if (activeImageSets.contains(imageSet) || selectAllSets) {
                        si2.add(row);
                    }
                    row++; // Only increment the row number after the second column
                }
                imageSets.add(imageSet);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to read image sets", e);
        }

        setUpList(jList1);
        jList1.setListData(data1.toArray(new ImageSetChooserPanel[0]));
        jList1.setSelectedIndices(convertIntegers(si1));

        setUpList(jList2);
        jList2.setListData(data2.toArray(new ImageSetChooserPanel[0]));
        jList2.setSelectedIndices(convertIntegers(si2));

        jScrollPane1.getVerticalScrollBar().setUnitIncrement(9);
    }

    /**
     * 统一把所有的组件都设为可见
     */
    public ArrayList<String> display() {
        setTitle(Main.getInstance().getLanguageBundle().getString("ShimejiImageSetChooser"));
        jLabel1.setText(Main.getInstance().getLanguageBundle().getString("SelectImageSetsToUse"));
        useSelectedButton.setText(Main.getInstance().getLanguageBundle().getString("UseSelected"));
        useAllButton.setText(Main.getInstance().getLanguageBundle().getString("UseAll"));
        cancelButton.setText(Main.getInstance().getLanguageBundle().getString("Cancel"));
        clearAllLabel.setText(Main.getInstance().getLanguageBundle().getString("ClearAll"));
        selectAllLabel.setText(Main.getInstance().getLanguageBundle().getString("SelectAll"));
        clearAllLabel.setFont(clearAllLabel.getFont().deriveFont(Font.BOLD));
        selectAllLabel.setFont(selectAllLabel.getFont().deriveFont(Font.BOLD));
        clearAllLabel.setForeground(UIManager.getColor("Button.focus"));
        selectAllLabel.setForeground(UIManager.getColor("Button.focus"));
        setVisible(true);
        if (closeProgram) {
            return null;
        }
        return imageSets;
    }

    /**
     * 读取配置中启用的所有图片集
     * @return activeImageSets 激活的图片集列表
     */
    private List<String> readConfigFile() {
        List<String> activeImageSets = new ArrayList<>(Arrays.asList(Main.getInstance().getProperties().getProperty("ActiveShimeji", "").split("/")));
        selectAllSets = activeImageSets.get(0).trim().isEmpty(); // if no active ones, activate them all!
        return activeImageSets;
    }

    private void updateConfigFile() {
        try (OutputStream output = Files.newOutputStream(Main.SETTINGS_FILE)) {
            // 将imageSets里启用的图片集更新到配置中
            Main.getInstance().getProperties().setProperty("ActiveShimeji", imageSets.toString().replace("[", "").replace("]", "").replace(", ", "/"));
            Main.getInstance().getProperties().store(output, "Shimeji-ee Configuration Options");
        } catch (IOException e) {
            // Doesn't matter at all
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jScrollPane1 = new JScrollPane();
        jPanel2 = new JPanel();
        jList1 = new ShimejiList();
        jList2 = new ShimejiList();
        jLabel1 = new JLabel();
        jPanel1 = new JPanel();
        useSelectedButton = new JButton();
        useAllButton = new JButton();
        cancelButton = new JButton();
        jPanel4 = new JPanel();
        clearAllLabel = new JLabel();
        slashLabel = new JLabel();
        selectAllLabel = new JLabel();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // 关闭方式
        setTitle("Shimeji-ee Image Set Chooser"); // 标题
        setMinimumSize(new Dimension(670, 495)); // 最小尺寸

        jScrollPane1.setPreferredSize(new Dimension(518, 100)); // 默认参考尺寸

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2); // 为一个容器使用GroupLayout,会分层次地归类组件
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup( // 水平元素组
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jList1, GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                                .addGap(0, 0, 0)
                                .addComponent(jList2, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)));
        jPanel2Layout.setVerticalGroup( // 垂直元素组
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jList2, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                        .addComponent(jList1, GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE));

        jScrollPane1.setViewportView(jPanel2);

        jLabel1.setText("Select Image Sets to Use:");

        jPanel1.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        // 监听使用选择项事件
        useSelectedButton.setText("Use Selected");
        useSelectedButton.setMaximumSize(new Dimension(130, 26));
        useSelectedButton.setPreferredSize(new Dimension(130, 26));
        useSelectedButton.addActionListener(this::useSelectedButtonActionPerformed);
        jPanel1.add(useSelectedButton);

        // 监听使用所有项事件
        useAllButton.setText("Use All");
        useAllButton.setMaximumSize(new Dimension(95, 23));
        useAllButton.setMinimumSize(new Dimension(95, 23));
        useAllButton.setPreferredSize(new Dimension(130, 26));
        useAllButton.addActionListener(this::useAllButtonActionPerformed);
        jPanel1.add(useAllButton);
        
        // 监听取消选择事件
        cancelButton.setText("Cancel");
        cancelButton.setMaximumSize(new Dimension(95, 23));
        cancelButton.setMinimumSize(new Dimension(95, 23));
        cancelButton.setPreferredSize(new Dimension(130, 26));
        cancelButton.addActionListener(this::cancelButtonActionPerformed);
        jPanel1.add(cancelButton);

        jPanel4.setLayout(new BoxLayout(jPanel4, BoxLayout.LINE_AXIS));

        clearAllLabel.setText("Clear All");
        clearAllLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearAllLabelMouseClicked(e);
            }
        });
        jPanel4.add(clearAllLabel);

        slashLabel.setText(" / ");
        jPanel4.add(slashLabel);

        selectAllLabel.setText("Select All");
        selectAllLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectAllLabelMouseClicked(e);
            }
        });
        jPanel4.add(selectAllLabel);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 384, Short.MAX_VALUE)
                                                .addComponent(jPanel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(jLabel1)
                                        .addComponent(jPanel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(11, 11, 11)));

        pack();
    }// </editor-fold>

    /**
     * 当鼠标点击clearAllLabel时,清空列表中所有的选中项
     * @param evt 鼠标动作事件
     */
    private void clearAllLabelMouseClicked(MouseEvent evt) {
        jList1.clearSelection();
        jList2.clearSelection();
    }

    /**
     * 当鼠标点击selectAllLabel时,选中列表中所有的选中项
     * @param evt 鼠标动作事件
     */
    private void selectAllLabelMouseClicked(MouseEvent evt) {
        jList1.setSelectionInterval(0, jList1.getModel().getSize() - 1);
        jList2.setSelectionInterval(0, jList2.getModel().getSize() - 1);
    }

    /**
     * 当点击useSelectedButton时, 将imageSets清空后, 把选中的图片集名称放入imageSets中
     * @param evt 鼠标动作事件
     */
    private void useSelectedButtonActionPerformed(ActionEvent evt) {
        imageSets.clear();

        for (ImageSetChooserPanel obj : jList1.getSelectedValuesList()) {
            if (obj != null) {
                imageSets.add(obj.getImageSetName());
            }
        }

        for (ImageSetChooserPanel obj : jList2.getSelectedValuesList()) {
            if (obj != null) {
                imageSets.add(obj.getImageSetName());
            }
        }

        updateConfigFile();
        closeProgram = false; // 不要关闭程序
        dispose(); // 关闭窗口
    }
    
    /**
     * 当点击useAllButton时, 将imageSets中已经存在的图片集更新到配置中
     * @param evt 鼠标动作事件
     */
    private void useAllButtonActionPerformed(ActionEvent evt) {
        updateConfigFile();
        closeProgram = false;
        dispose();
    }

    /**
     * 当点击cancelButton时, 关闭选择窗口
     * @param evt 鼠标动作事件
     */
    private void cancelButtonActionPerformed(ActionEvent evt) {
        dispose();
    }

    /**
     * 把Integer转成int
     */
    private int[] convertIntegers(Collection<Integer> integers) {
        return integers.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 自定义JList的选择方法
     */
    private void setUpList(JList<?> list) {
        list.setSelectionModel(new DefaultListSelectionModel() {
            private int i0 = -1;
            private int i1 = -1;

            @Override
            public void setSelectionInterval(int index0, int index1) {
                // These statements ensure that the buttons do not flicker whenever the cursor is dragged over them
                // This code was made by Francisco on StackOverflow (https://stackoverflow.com/a/5831609)
                if (i0 == index0 && i1 == index1) {
                    if (getValueIsAdjusting()) {
                        setValueIsAdjusting(false);
                        setSelection(index0, index1);
                    }
                } else {
                    i0 = index0;
                    i1 = index1;
                    setValueIsAdjusting(false);
                    setSelection(index0, index1);
                }
            }
            
            private void setSelection(int index0, int index1) {
                if (isSelectedIndex(index0)) {
                    removeSelectionInterval(index0, index1);
                } else {
                    addSelectionInterval(index0, index1);
                }
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Main.getInstance().run();
        EventQueue.invokeLater(() -> {
            new ImageSetChooser(new JFrame(), true).display();
            System.exit(0);
        });
    }

    // Variables declaration - do not modify
    /**取消选择按钮 */
    private JButton cancelButton;
    /**清空全选-标签 */
    private JLabel clearAllLabel;
    private JLabel jLabel1;
    /**图片选择-列表1 */
    private JList<ImageSetChooserPanel> jList1;
    /**图片选择-列表2 */
    private JList<ImageSetChooserPanel> jList2;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel4;
    /**滚动条 */
    private JScrollPane jScrollPane1;
    /**全选-标签 */
    private JLabel selectAllLabel;
    private JLabel slashLabel;
    /**使用全部-按钮 */
    private JButton useAllButton;
    /**使用选中项-按钮 */
    private JButton useSelectedButton;
    // End of variables declaration
}
