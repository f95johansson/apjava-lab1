/*
 * File: UI.java
 * Author: Fredrik Johansson
 * Date: 2016-10-29
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

/**
 * View
 */
public class UI extends Observable implements ActionListener {

    public static final Color BACKGROUND = new Color(34, 34, 34);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    public static final Color COLOR_FIELD = new Color(86, 86, 86);
    public static final Color COLOR_PANEL = new Color(56, 56, 56);
    public static final Color COLOR_SUCCESS = new Color(80, 200, 147);
    public static final Color COLOR_FAIL = new Color(252, 57, 36);
    public static final Color COLOR_WARNING = new Color(252, 152, 12);
    public static Color COLOR_TEXT = new Color(255, 255, 255);

    private DefaultListModel<TestInfo> mOutput;
    private JTextField mInput;
    private JButton mRunButton;
    private JPanel mStatusBar;

    private Map<String, Integer> mCurrentTests;
    private boolean mTestFinished;

    public UI() {
        SwingUtilities.invokeLater(this::setUpDarkUI);
    }

    private void setUpDarkUI() {

        JFrame window = new JFrame();
        //window.setUndecorated(true);
        window.getContentPane().setBackground(BACKGROUND);

        JToolBar toolBar = new JToolBar("Toolbar");
        toolBar.setBackground(TRANSPARENT);
        toolBar.setFloatable(false);
        toolBar.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));

        mInput = new JTextField("Enter TestClass name");
        mInput.setBackground(COLOR_FIELD);
        mInput.setForeground(COLOR_TEXT);
        //mInput.setBorder(new RoundedBorder(5));
        mInput.setBorder(BorderFactory.createEmptyBorder());
        mInput.setPreferredSize(new Dimension(204, 28));
        mInput.addActionListener(this);
        toolBar.add(mInput);


        mRunButton = new JButton("▶");
        mRunButton.setFont(new Font(mRunButton.getFont().getName(), Font.PLAIN, 18));
        mRunButton.setForeground(COLOR_SUCCESS);
        mRunButton.setBackground(COLOR_FIELD);
        mRunButton.setOpaque(true);
        mRunButton.setBorderPainted(false);
        mRunButton.setPreferredSize(new Dimension(28, 28));
        mRunButton.addActionListener(this);
        toolBar.add(mRunButton);

        window.add(toolBar, BorderLayout.NORTH);

        mOutput = new DefaultListModel<>();
        JList<TestInfo> list = new JList<>(mOutput);
        list.setCellRenderer(new TestInfoRenderer());
        JScrollPane content = new JScrollPane(list);
        content.setBorder(BorderFactory.createMatteBorder(0, 6, 0, 6, TRANSPARENT));
        content.setOpaque(false);
        list.setBackground(COLOR_PANEL);
        list.setForeground(Color.WHITE);
        content.setBackground(TRANSPARENT);
        window.add(content);

        mStatusBar = new JPanel();
        mStatusBar.setBackground(TRANSPARENT);
        mStatusBar.add(new JLabel(" ")); // Sets correct height to fit text
        mStatusBar.setOpaque(false);
        window.add(mStatusBar, BorderLayout.SOUTH);

        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(350, 430));

        window.setLocationRelativeTo(null);

        window.setVisible(true);
    }

    private void setUpNativeUI() {
        COLOR_TEXT = new Color(0, 0, 0);

        JFrame window = new JFrame();
        window.getRootPane().putClientProperty("apple.awt.brushMetalLook", true);

        JPanel toolBar = new JPanel();
        //toolBar.setFloatable(false);
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolBar.add(new JLabel("Enter class name:"));
        mInput = new JTextField(14);
        mInput.addActionListener(this);
        toolBar.add(mInput);
        mRunButton = new JButton("Run tests");
        mRunButton.addActionListener(this);
        toolBar.add(mRunButton);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            mCurrentTests.clear();
            mOutput.clear();
            clearStatus();
        });
        toolBar.add(clearButton);
        window.add(toolBar, BorderLayout.NORTH);

        mOutput = new DefaultListModel<>();
        JList<TestInfo> list = new JList<>(mOutput);
        list.setCellRenderer(new TestInfoRenderer());
        JScrollPane content = new JScrollPane(list);
        content.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(168, 168, 168)));
        content.setBackground(TRANSPARENT);
        window.add(content);

        mStatusBar = new JPanel();
        mStatusBar.setBackground(TRANSPARENT);
        mStatusBar.add(new JLabel(" "));
        window.add(mStatusBar, BorderLayout.SOUTH);

        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(460, 328));

        window.setVisible(true);
    }

    public void addUserError(String message) {
        SwingUtilities.invokeLater(() -> {
            TestInfo info = new TestInfo(TestInfo.Status.FAILED, message);
            mOutput.addElement(info);
        });
    }

    public void addTestWarning(String message) {
        SwingUtilities.invokeLater(() -> {
            TestInfo info = new TestInfo(TestInfo.Status.WARNING, message);
            mOutput.addElement(info);
        });
    }

    public void addTestStarted(String testName) {

        SwingUtilities.invokeLater(() -> {
            TestInfo info = new TestInfo(TestInfo.Status.RUNNING, testName);
            int index = mOutput.size();
            mOutput.addElement(info);
            mCurrentTests.put(testName, index);
            updateStatusRunning();
        });
    }

    public void addTestResult(String testName, Boolean success) {
        addTestResult(testName, success, null);
    }

    public void addTestResult(String testName, Boolean success, Throwable exception) {
        SwingUtilities.invokeLater(() -> {
            if (mCurrentTests.containsKey(testName)) {
                TestInfo info = mOutput.get(mCurrentTests.get(testName));
                info.setStatus(success ? TestInfo.Status.SUCCESS : TestInfo.Status.FAILED);

                if (exception != null) {
                    info.setException(exception);
                }
                mOutput.set(mCurrentTests.get(testName), info); // force refresh

                updateStatusRunning();
            }
        });
    }

    public void clearStatus() {
        SwingUtilities.invokeLater(() -> {
            mStatusBar.removeAll();
            mStatusBar.repaint();
        });
    }

    public void updateStatusDone(int done, int succeeded, int failed, int exceptions) {
        mTestFinished = true;
        updateStatus(done, "done", succeeded, failed, exceptions);
    }

    public void updateStatusRunning() {
        if (mTestFinished) {
            return;
        }

        int running = 0;
        int succeeded = 0;
        int failed = 0;
        int exceptions = 0;

        for (int index : mCurrentTests.values()) {
            TestInfo info = mOutput.get(index);
            if (info.getStatus() == TestInfo.Status.RUNNING) {
                running += 1;
            } else if (info.getStatus() == TestInfo.Status.SUCCESS) {
                succeeded += 1;
            } else {
                failed += 1;
                if (info.hasException()) {
                    exceptions += 1;
                }
            }
        }
        if (running > 0) {
            updateStatus(running, "running", succeeded, failed, exceptions);
        }
    }

    private void updateStatus(int firstCount, String firstQuantifier,
                              int succeeded,
                              int failed,
                              int exceptions) {

        SwingUtilities.invokeLater(() -> {
            mStatusBar.removeAll();
            mStatusBar.add(createLabel(firstCount + " "+firstQuantifier+"  ", COLOR_TEXT));
            mStatusBar.add(createLabel("  " + succeeded + " succeeded", COLOR_SUCCESS));
            mStatusBar.add(createLabel("/ ", COLOR_TEXT));
            mStatusBar.add(createLabel(failed + " failed (" + exceptions + " exceptions)", COLOR_FAIL));
            mStatusBar.validate();
            mStatusBar.repaint();
        });
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        return label;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        mCurrentTests = new HashMap<>();
        mOutput.clear();
        clearStatus();
        setChanged();
        mTestFinished = false;

        notifyObservers(mInput.getText());
    }
}
