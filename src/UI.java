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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes care of the GUI of the program.
 *
 * View in MVC.
 */
public class UI extends Observable implements ActionListener {

    public static final Color BACKGROUND = new Color(34, 34, 34);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    public static final Color COLOR_FIELD = new Color(86, 86, 86);
    public static final Color COLOR_PANEL = new Color(56, 56, 56);
    public static final Color COLOR_SUCCESS = new Color(80, 200, 147);
    public static final Color COLOR_FAIL = new Color(252, 57, 36);
    public static final Color COLOR_WARNING = new Color(252, 152, 12);
    public static final Color COLOR_TEXT = new Color(255, 255, 255);

    private DefaultListModel<TestInfo> mOutput;
    private JTextField mInput;
    private JPanel mStatusBar;

    private Map<String, Integer> mCurrentTests;
    private AtomicBoolean mTestFinished = new AtomicBoolean();

    /**
     * Starts the GUI using swing
     */
    public UI() {
        SwingUtilities.invokeLater(this::setUpDarkUI);
    }

    /**
     * Start the ui and sets up all the necessary components.
     * Must be run from the sing thread.
     */
    private void setUpDarkUI() {

        JFrame window = new JFrame();
        window.getContentPane().setBackground(BACKGROUND);

        window.add(setUpToolbar(), BorderLayout.NORTH);

        window.add(setUpOutputArea());

        mStatusBar = setUpStatusBar();
        window.add(mStatusBar, BorderLayout.SOUTH);

        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(350, 430));

        window.setLocationRelativeTo(null);

        window.setVisible(true);
    }

    /**
     * Creates the toolbar. Must be run from the sing thread.
     * @return The toolbar
     */
    private JToolBar setUpToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setBackground(TRANSPARENT);
        toolBar.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
        toolBar.setFloatable(false);

        mInput = setUpInputField();
        toolBar.add(mInput);

        toolBar.add(setUpRunButton());
        return toolBar;
    }

    /**
     * Setups up the text field which user will input test class name.
     * Hitting the enter key will having the text field in focus will start
     * the tests. Must be run from the sing thread.
     * @return A editable text field
     */
    private JTextField setUpInputField() {
        JTextField input = new JTextField("Enter TestClass name");
        input.setBackground(COLOR_FIELD);
        input.setForeground(COLOR_TEXT);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setPreferredSize(new Dimension(204, 28));
        input.addActionListener(this);
        return input;
    }

    /**
     * Creates a button which will run the tests (if possible).
     * Must be run from the sing thread.
     * @return The run button
     */
    private JButton setUpRunButton() {
        JButton runButton = new JButton("▶");
        runButton.setFont(
                new Font(runButton.getFont().getName(), Font.PLAIN, 18));
        runButton.setForeground(COLOR_SUCCESS);
        runButton.setBackground(COLOR_FIELD);
        runButton.setOpaque(true);
        runButton.setBorderPainted(false);
        runButton.setPreferredSize(new Dimension(28, 28));
        runButton.addActionListener(this);
        return runButton;
    }

    /**
     * Setups a list which the result of the test will be shown. Uses
     * TestInfoRenderer to handle the actual showing of each results.
     * Must be run from the sing thread.
     * @return A scrollable pane
     */
    private JScrollPane setUpOutputArea() {
        mOutput = new DefaultListModel<>();
        JList<TestInfo> list = new JList<>(mOutput);
        list.setCellRenderer(new TestInfoRenderer());
        JScrollPane content = new JScrollPane(list);
        content.setBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 6, TRANSPARENT));
        content.setOpaque(false);
        list.setBackground(COLOR_PANEL);
        list.setForeground(Color.WHITE);
        content.setBackground(TRANSPARENT);
        return content;
    }

    /**
     * Setup a status bar which the summation of the test methods will be shown.
     * Must be run from the sing thread.
     * @return A status bar
     */
    private JPanel setUpStatusBar() {
        JPanel statusBar = new JPanel();
        statusBar.setBackground(TRANSPARENT);
        statusBar.add(new JLabel(" ")); // Sets correct height to fit text
        statusBar.setOpaque(false);
        return statusBar;
    }

    /**
     * Adds information to the user about something they did wrong/haven't done.
     * @param message Message to show user
     */
    public void addUserError(String message) {
        SwingUtilities.invokeLater(() -> {
            TestInfo info = new TestInfo(TestInfo.Status.FAILED, message);
            mOutput.addElement(info);
        });
    }

    /**
     * Adds a waring message about a test
     * @param message Message with a warning
     */
    public void addTestWarning(String message) {
        SwingUtilities.invokeLater(() -> {
            TestInfo info = new TestInfo(TestInfo.Status.WARNING, message);
            mOutput.addElement(info);
        });
    }

    /**
     * Should be added when a test have started
     * @param testName Name of test
     */
    public void addTestStarted(String testName) {

        SwingUtilities.invokeLater(() -> {
            TestInfo info = new TestInfo(TestInfo.Status.RUNNING, testName);
            int index = mOutput.size();
            mOutput.addElement(info);
            mCurrentTests.put(testName, index);
            updateStatusRunning();
        });
    }

    /**
     * Should be added when a test has finished
     * @param testName Name of test
     * @param success If the test succeeded
     */
    public void addTestResult(String testName, Boolean success) {
        addTestResult(testName, success, null);
    }

    /**
     * Should be added when a test finished with a exception
     * @param testName Name of test
     * @param success If the test succeeded
     * @param exception The exception which was thrown
     */
    public void addTestResult(String testName,
                              Boolean success,
                              Throwable exception) {

        SwingUtilities.invokeLater(() -> {
            if (mCurrentTests.containsKey(testName)) {
                TestInfo info =
                        mOutput.get(mCurrentTests.get(testName));

                info.setStatus(success ?
                               TestInfo.Status.SUCCESS :
                               TestInfo.Status.FAILED);

                if (exception != null) {
                    info.setException(exception);
                }
                mOutput.set(mCurrentTests.get(testName), info); // force refresh

                updateStatusRunning();

            } else {
                TestInfo.Status status = success ?
                                         TestInfo.Status.SUCCESS :
                                         TestInfo.Status.FAILED;

                TestInfo info = new TestInfo(status, testName);
                if (exception != null) {
                    info.setException(exception);
                }
                mOutput.set(mCurrentTests.get(testName), info);
                updateStatusRunning();
            }
        });
    }

    /**
     * Clear status bar
     */
    public void clearStatus() {
        SwingUtilities.invokeLater(() -> {
            mStatusBar.removeAll();
            mStatusBar.repaint();
        });
    }

    /**
     * Update the status bar with information from when every test have finished
     * @param done How many test have finished
     * @param succeeded How many succeeded
     * @param failed How many failed
     * @param exceptions How many gave an exception
     */
    public void updateStatusDone(int done, int succeeded,
                                 int failed, int exceptions) {
        mTestFinished.set(true);
        updateStatus(done, "done", succeeded, failed, exceptions);
    }

    /**
     * Update the status bar with the current information about the test
     * which are running.
     */
    public void updateStatusRunning() {
        if (mTestFinished.get()) {
            return; // don't update running tests if all have finished
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

    /**
     * Update status bar with specified information
     * @param firstCount How many of the first count
     * @param firstQuantifier What kind is the first counr
     * @param succeeded How many succeeded
     * @param failed How many failed
     * @param exceptions How many exceptions
     */
    private void updateStatus(int firstCount, String firstQuantifier,
                              int succeeded,
                              int failed,
                              int exceptions) {

        SwingUtilities.invokeLater(() -> {
            mStatusBar.removeAll();

            String first = firstCount + " "+firstQuantifier+"  ";
            mStatusBar.add(createLabel(first, COLOR_TEXT));

            String second = "  " + succeeded + " succeeded";
            mStatusBar.add(createLabel(second, COLOR_SUCCESS));

            mStatusBar.add(createLabel("/ ", COLOR_TEXT));

            String third = failed + " failed (" + exceptions + " exceptions)";
            mStatusBar.add(createLabel(third, COLOR_FAIL));

            mStatusBar.validate();
            mStatusBar.repaint();
        });
    }

    /**
     * Create a label in specified color
     * @param text Text of label
     * @param color Color of label
     * @return The label
     */
    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        return label;
    }

    /**
     * Triggered whenever the user hits the run button or the enter key
     * when focused on the input field
     * @param event The event which were triggered
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        mCurrentTests = new HashMap<>();
        mOutput.clear();
        clearStatus();
        setChanged();
        mTestFinished.set(false);

        notifyObservers(mInput.getText());
    }
}
