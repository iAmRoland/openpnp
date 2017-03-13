package org.openpnp.gui;

import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LogEntryListCellRenderer;
import org.openpnp.gui.support.LogEntryListModel;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.openpnp.logging.SystemLogger;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.LogEntry;

public class LogPanel extends JPanel {

    private Preferences prefs = Preferences.userNodeForPackage(LogPanel.class);

    private static final String PREF_LOG_LEVEL = "LogPanel.logLevel";
    private static final String PREF_LOG_LEVEL_DEF = Level.INFO.toString();

    private static final String PREF_LOG_AUTO_SCROLL = "LogPanel.autoScroll";
    private static final boolean PREF_LOG_AUTO_SCROLL_DEF = true;

    private boolean autoScroll = PREF_LOG_AUTO_SCROLL_DEF;
    private boolean systemOutEnabled = true;

    private LogEntryListModel logEntries = new LogEntryListModel();
    private JList<LogEntry> logEntryJList = new JList<>(logEntries);
    private JScrollPane scrollPane = new JScrollPane(logEntryJList);

    private Level filterLogLevel = Level.TRACE;
    private LogEntryListModel.LogEntryFilter logLevelFilter = new LogEntryListModel.LogEntryFilter();
    private LogEntryListModel.LogEntryFilter searchBarFilter = new LogEntryListModel.LogEntryFilter();
    private LogEntryListModel.LogEntryFilter systemOutFilter = new LogEntryListModel.LogEntryFilter();

    public LogPanel() {

        loadLoggingPreferences();

        logEntries.addFilter(logLevelFilter);
        logEntries.addFilter(searchBarFilter);
        logEntries.addFilter(systemOutFilter);

        setLayout(new BorderLayout(0, 0));

        JPanel settingsAndFilterPanel = new JPanel();
        settingsAndFilterPanel.setLayout(new BorderLayout(0, 0));
        add(settingsAndFilterPanel, BorderLayout.NORTH);

        // The Global Filter settings
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        settingsPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Global Logging Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        settingsPanel.add(createGlobalLogLevelPanel());

        settingsAndFilterPanel.add(settingsPanel, BorderLayout.NORTH);

        // The filter settings
        JPanel filterPanel = new JPanel(new BorderLayout(0, 0));

        filterPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Filter Logging Panel", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

        JPanel filterContentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filterContentPanel.add(createSearchFieldPanel());
        filterContentPanel.add(createFilterLogLevelPanel());

        filterContentPanel.add(createSystemOutputCheckbox());

        filterPanel.add(filterContentPanel, BorderLayout.WEST);

        JPanel filterControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnClear = new JButton(Icons.delete);
        btnClear.setToolTipText("Clear log");

        btnClear.addActionListener(e -> logEntries.clear());

        filterControlPanel.add(btnClear);

        JButton btnCopyToClipboard = new JButton(Icons.copy);
        btnCopyToClipboard.setToolTipText("Copy to clipboard");

        btnCopyToClipboard.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            logEntries.getFilteredLogEntries().forEach(logEntry -> sb.append(logEntry.getRenderedLogEntry()));
            copyStringToClipboard(sb.toString());
        });

        filterControlPanel.add(btnCopyToClipboard);

        JToggleButton btnScroll = new JToggleButton(Icons.scrollDownDisabled);
        btnScroll.setSelectedIcon(Icons.scrollDown);
        btnScroll.setToolTipText("Auto scrolling");

        btnScroll.setSelected(autoScroll);

        btnScroll.addActionListener(e -> {
            autoScroll = ((JToggleButton) e.getSource()).isSelected();
            if (autoScroll) {
                scrollDownLogPanel();
            }
            prefs.putBoolean(PREF_LOG_AUTO_SCROLL, autoScroll);
        });

        filterControlPanel.add(btnScroll);

        filterPanel.add(filterControlPanel, BorderLayout.EAST);

        settingsAndFilterPanel.add(filterPanel);

        // Log Panel
        logEntryJList.setCellRenderer(new LogEntryListCellRenderer());

        logEntryJList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "log-copy");
        logEntryJList.getActionMap().put("log-copy", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<LogEntry> logList = (ArrayList<LogEntry>) logEntryJList.getSelectedValuesList();
                StringBuilder sb = new StringBuilder();
                logList.forEach(logEntry -> sb.append(logEntry.getRenderedLogEntry()));
                copyStringToClipboard(sb.toString());
            }
        });


        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Scroll down if enabled
        logEntries.addListDataListener(new ListDataListener() {

            @Override
            public void intervalAdded(ListDataEvent e) {
                scrollDownLogPanel();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                scrollDownLogPanel();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                scrollDownLogPanel();
            }
        });

    }

    private void scrollDownLogPanel() {
        if (autoScroll) {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            AdjustmentListener downScroller = new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    Adjustable adjustable = e.getAdjustable();
                    adjustable.setValue(adjustable.getMaximum());
                    verticalBar.removeAdjustmentListener(this);
                }
            };
            verticalBar.addAdjustmentListener(downScroller);
        }
    }

    private JCheckBox createSystemOutputCheckbox() {
        JCheckBox systemOutCheckbox = new JCheckBox("System output enable");
        systemOutCheckbox.setSelected(systemOutEnabled);
        systemOutCheckbox.addActionListener(e -> {
            systemOutEnabled = systemOutCheckbox.isSelected();
            if (systemOutEnabled) {
                systemOutFilter.setFilter(logEntry -> true);
            } else {
                systemOutFilter.setFilter(logEntry -> !Objects.equals(logEntry.getClassName(), SystemLogger.class.getName()));
            }
            logEntries.filter();
        });
        return systemOutCheckbox;
    }

    private void loadLoggingPreferences() {

        autoScroll = prefs.getBoolean(PREF_LOG_AUTO_SCROLL, PREF_LOG_AUTO_SCROLL_DEF);

        // This weird check is here because I mistakenly reused the same config key when
        // switching from slf to tinylog. This meant that some users had an int based
        // value in the key rather than the string. This caused initialization failures.
        Level level = null;
        try {
            level = Level.valueOf(prefs.get(PREF_LOG_LEVEL, PREF_LOG_LEVEL_DEF));
        } catch (Exception ignored) {
        }
        if (level == null) {
            level = Level.INFO;
        }

        Configurator
                .currentConfig()
                .level(level)
                .activate();

        Configurator
                .currentConfig()
                .addWriter(logEntries)
                .activate();
    }

    private JPanel createSearchFieldPanel() {
        JPanel searchField = new JPanel();

        JLabel lblSearch = new JLabel("Search");
        searchField.add(lblSearch);

        JTextField searchTextField = new JTextField();
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {

            private void updateSearchBarFilter() {
                String searchText = searchTextField.getText();
                searchBarFilter.setFilter((logEntry -> logEntry.getRenderedLogEntry().toLowerCase().contains(searchText.toLowerCase())));
                logEntries.filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSearchBarFilter();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSearchBarFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSearchBarFilter();
            }
        });

        searchTextField.setColumns(20);
        searchField.add(searchTextField);
        return searchField;
    }

    private JPanel createFilterLogLevelPanel() {
        JPanel filterLogLevelPanel = new JPanel();
        filterLogLevelPanel.add(new JLabel("Log Level:"));
        JComboBox<Level> logLevelFilterComboBox = createLogLevelFilterComboBox(filterLogLevel);
        logLevelFilterComboBox.addActionListener(e -> {
            Level logLevel = (Level) logLevelFilterComboBox.getSelectedItem();
            logLevelFilter.setFilter(logEntry -> logEntry.getLevel().compareTo(logLevel) >= 0);
            logEntries.filter();
        });
        filterLogLevelPanel.add(logLevelFilterComboBox);
        return filterLogLevelPanel;
    }

    private JPanel createGlobalLogLevelPanel() {
        JPanel globalLogLevelPanel = new JPanel();
        globalLogLevelPanel.add(new JLabel("Global Log Level:"));
        JComboBox<Level> logLevelComboBox = createLogLevelFilterComboBox(Level.valueOf(prefs.get(PREF_LOG_LEVEL, PREF_LOG_LEVEL_DEF)));
        logLevelComboBox.addActionListener(e -> {
            Level logLevel = (Level) logLevelComboBox.getSelectedItem();
            prefs.put(PREF_LOG_LEVEL, logLevel.toString());
            Configurator
                    .currentConfig()
                    .level(logLevel)
                    .activate();
        });
        globalLogLevelPanel.add(logLevelComboBox);
        return globalLogLevelPanel;
    }

    private JComboBox<Level> createLogLevelFilterComboBox(Level selectedLogLevel) {
        JComboBox<Level> logLevelFilterComboBox = new JComboBox<>();

        for (Level logLevel : Level.values()) {
            logLevelFilterComboBox.addItem(logLevel);
        }

        logLevelFilterComboBox.setSelectedItem(selectedLogLevel);

        return logLevelFilterComboBox;
    }

    private void copyStringToClipboard(String s) {
        StringSelection selection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

}
