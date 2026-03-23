import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.Enumeration;

public class PwaManager extends JFrame {
    private final String BASE_PATH = System.getProperty("user.home") + "/.local/share/applications/";
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> fileList = new JList<>(listModel);
    private JTextArea contentDisplay = new JTextArea();
    private JLabel detailTitle = new JLabel("Select a file to view details");
    private JTextField searchInput = new JTextField();
    private String currentSelectedFile = null;

    public PwaManager() {
        setTitle("Chrome PWA Desktop Manager");
        setSize(1100, 800); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- Left Sidebar ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setPreferredSize(new Dimension(320, 0));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 5));
        
        searchInput.setBorder(BorderFactory.createTitledBorder("Search..."));
        searchInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) { filterFiles(); }
        });

        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setFixedCellHeight(45); // Larger clickable area
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) readFileContent(fileList.getSelectedValue());
        });

        leftPanel.add(searchInput, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);

        // --- Right Content ---
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(new EmptyBorder(10, 5, 10, 10));
        
        detailTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        contentDisplay.setEditable(false);
        contentDisplay.setFont(new Font("Monospaced", Font.PLAIN, 16));
        contentDisplay.setMargin(new Insets(10, 10, 10, 10));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        JButton addBtn = new JButton("Add Shortcut");
        JButton iconBtn = new JButton("Change Icon");
        JButton delBtn = new JButton("Delete");

        addBtn.addActionListener(e -> showAddDialog());
        iconBtn.addActionListener(e -> showIconDialog());
        delBtn.addActionListener(e -> deleteShortcut());

        buttonPanel.add(addBtn); buttonPanel.add(iconBtn); buttonPanel.add(delBtn);

        rightPanel.add(detailTitle, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(contentDisplay), BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(320);
        add(splitPane, BorderLayout.CENTER);

        refreshAll();
    }

    private void refreshAll() {
        filterFiles();
        contentDisplay.setText("");
        detailTitle.setText("Select a file");
    }

    private void filterFiles() {
        String term = searchInput.getText().toLowerCase();
        listModel.clear();
        File dir = new File(BASE_PATH);
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".desktop"));
        if (files != null) {
            for (File f : files) {
                if (f.getName().toLowerCase().contains(term)) listModel.addElement(f.getName());
            }
        }
    }

    private void readFileContent(String filename) {
        if (filename == null) return;
        currentSelectedFile = filename;
        detailTitle.setText(filename);
        try {
            contentDisplay.setText(Files.readString(Paths.get(BASE_PATH, filename)));
            contentDisplay.setCaretPosition(0);
        } catch (IOException e) {
            contentDisplay.setText("Error reading file");
        }
    }

    // --- Functional Dialogs ---

    private void showAddDialog() {
        JTextField appId = new JTextField(20);
        JTextField appName = new JTextField(20);
        JTextField fileName = new JTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("APP_ID:")); panel.add(appId);
        panel.add(new JLabel("APP_NAME:")); panel.add(appName);
        panel.add(new JLabel("FILE_NAME:")); panel.add(fileName);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Shortcut", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            runCommand("create", appId.getText(), appName.getText(), fileName.getText());
        }
    }

    private void showIconDialog() {
        JTextField pngPath = new JTextField(20);
        JTextField appId = new JTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("PNG_PATH:")); panel.add(pngPath);
        panel.add(new JLabel("APP_ID:")); panel.add(appId);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Icon", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            runCommand("icon", pngPath.getText(), appId.getText());
        }
    }

    private void deleteShortcut() {
        if (currentSelectedFile == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + currentSelectedFile + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Files.deleteIfExists(Paths.get(BASE_PATH, currentSelectedFile));
                refreshAll();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
            }
        }
    }

    // --- Subprocess execution ---
    private void runCommand(String... args) {
        new Thread(() -> {
            try {
                String pwaBin = System.getProperty("user.dir") + File.separator + "pwa";
                String[] cmd = new String[args.length + 1];
                cmd[0] = pwaBin;
                System.arraycopy(args, 0, cmd, 1, args.length);

                Process process = new ProcessBuilder(cmd).start();
                int exitCode = process.waitFor();

                SwingUtilities.invokeLater(() -> {
                    if (exitCode == 0) refreshAll();
                    else JOptionPane.showMessageDialog(this, "Operation failed (Exit Code: " + exitCode + ")");
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "System Error: " + ex.getMessage()));
            }
        }).start();
    }

    public static void setGlobalFont(Font font) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            setGlobalFont(new Font("SansSerif", Font.PLAIN, 18)); // Large font for all UI elements
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> new PwaManager().setVisible(true));
    }
}

