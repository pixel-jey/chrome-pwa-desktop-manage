import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PwaManager extends JFrame {
	private static final long serialVersionUID = 1L;
	private final String BASE_PATH = System.getProperty("user.home") + "/.local/share/applications/";
	private DefaultListModel<String> listModel = new DefaultListModel<>();
	private JList<String> fileList = new JList<>(listModel);
	private JTextArea contentDisplay = new JTextArea();
	private JTextField searchInput = new JTextField();
	private JLabel pwdId = new JLabel();
	private JButton b1;
	private JButton b2;
	private JButton b3;
	private JButton b4;
	private JSplitPane splitPane;
	private String currentSelectedFile = null;

	public PwaManager() {
		setTitle("PWA Manager");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		setLayout(new BorderLayout());
		headerExit();

		// Load Icon safely
		try {
			java.net.URL iconURL = getClass().getResource("/icons/chrome-pwa-desktop-manage.png");
			if (iconURL != null)
				setIconImage(new ImageIcon(iconURL).getImage());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Left Panel (Menu)
		JPanel left = new JPanel(new BorderLayout(10, 10));
		left.setBorder(new EmptyBorder(10, 10, 10, 10));
		left.setPreferredSize(new Dimension(450, 0));

		searchInput.setFont(new Font("SansSerif", Font.PLAIN, 22));
		searchInput.setBorder(BorderFactory.createTitledBorder("Search..."));
		searchInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				filterFiles();
			}
		});

		fileList.setFont(new Font("SansSerif", Font.PLAIN, 22));
		fileList.setFixedCellHeight(50);
		fileList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				String val = fileList.getSelectedValue();
				if (val != null)
					readFileAsync(val);
			}
		});

		left.add(searchInput, BorderLayout.NORTH);
		left.add(new JScrollPane(fileList), BorderLayout.CENTER);

		// Right Panel (Content)
		JPanel right = new JPanel(new BorderLayout(10, 10));
		right.setBorder(new EmptyBorder(10, 10, 10, 10));

		contentDisplay.setFont(new Font("Monospaced", Font.PLAIN, 24));
		contentDisplay.setEditable(false);
		contentDisplay.setLineWrap(true);

		JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
		Font f20 = new Font("SansSerif", Font.BOLD, 20);
		b1 = new JButton("Add");
		b1.setFont(f20);
		b2 = new JButton("Icon");
		b2.setFont(f20);
		b3 = new JButton("Rename");
		b3.setFont(f20);
		b4 = new JButton("Delete");
		b4.setFont(f20);

		b1.addActionListener(e -> showAdd());
		b2.addActionListener(e -> showIcon());
		b3.addActionListener(e -> doRename());
		b4.addActionListener(e -> doDelete());

		btns.add(b1);
		btns.add(b2);
		btns.add(b3);
		btns.add(b4);
		btns.add(pwdId);
		right.add(new JScrollPane(contentDisplay), BorderLayout.CENTER);
		right.add(btns, BorderLayout.SOUTH);

		// Split Pane Assembly
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		splitPane.setResizeWeight(0.2);
		add(splitPane, BorderLayout.CENTER);

		btnStatus(false);
		filterFiles();
	}

	private void headerExit() {
		setUndecorated(true);
		setExtendedState(JFrame.MAXIMIZED_BOTH);

		JButton	closeBtn = new JButton("✕");
		closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 32));
		closeBtn.setForeground(new Color(110, 110, 110));
		closeBtn.setBorderPainted(true);
		closeBtn.setFocusPainted(true);
		closeBtn.setContentAreaFilled(false);
		closeBtn.setOpaque(false);
		closeBtn.setMargin(new Insets(10, 10, 10, 10));

		closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				closeBtn.setBackground(new Color(232, 17, 35));
				closeBtn.setForeground(Color.WHITE);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				closeBtn.setBackground(new Color(255, 255, 255));
				closeBtn.setForeground(Color.BLACK);
			}

			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				closeBtn.setBackground(new Color(241, 112, 122));
			}
		});

		closeBtn.addActionListener(e -> {
			try {
				String userHome = System.getProperty("user.home");

				String cmdStr = String.format("rm -rf %1$s/Desktop/chrome-*.desktop; "
						+ "rm -rf %1$s/.local/share/applications/chrome-*.desktop; "
						+ "touch %1$s/.local/share/icons/hicolor; "
						+ "gtk-update-icon-cache -f -t %1$s/Light/icons/hicolor --ignore-theme-index; "
						+ "update-desktop-database %1$s/.local/share/applications", userHome);

				String[] cmd = { "/bin/bash", "-c", cmdStr };
				Runtime.getRuntime().exec(cmd).waitFor();

			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				System.exit(0);
			}
		});

		JLabel	titleLabel = new JLabel(getTitle(), JLabel.CENTER);
		JPanel	titlePanel = new JPanel(new BorderLayout());
		titlePanel.add(titleLabel, BorderLayout.CENTER);
		titlePanel.add(closeBtn, BorderLayout.EAST);

		add(titlePanel, BorderLayout.NORTH);
	}

	private void filterFiles() {
		String q = searchInput.getText().toLowerCase();
		listModel.clear();
		File d = new File(BASE_PATH);
		if (d.exists()) {
			File[] fs = d.listFiles((dir, n) -> n.endsWith(".desktop"));
			if (fs != null) {
				for (File f : fs) {
					if (f.getName().toLowerCase().contains(q)) {
						listModel.addElement(f.getName());
					}
				}
			}
		}
	}

	// Fixed: Uses Java 8 compatible stream reading inside a SwingWorker background
	// thread
	private void readFileAsync(String n) {
		currentSelectedFile = n;
		contentDisplay.setText("Loading...");

		SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
			private String extractedId = "";

			@Override
			protected String doInBackground() throws Exception {
				File file = new File(BASE_PATH, n);
				byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
				String content = new String(bytes, StandardCharsets.UTF_8);

				java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Icon=chrome-([a-z]{32})");
				java.util.regex.Matcher matcher = pattern.matcher(content);
				if (matcher.find()) {
					extractedId = matcher.group(1);
					btnStatus(true);
				} else {
					extractedId = "";
					btnStatus(false);
				}

				return content;
			}

			@Override
			protected void done() {
				try {
					contentDisplay.setText(get());
					pwdId.setText(extractedId);
				} catch (Exception e) {
					contentDisplay.setText("Error reading file.");
					pwdId.setText("Error");
					e.printStackTrace();
				}
			}
		};
		worker.execute();
	}

	private void btnStatus(boolean bool) {
		b1.setEnabled(bool);
		b2.setEnabled(bool);
		b3.setEnabled(bool);
		b4.setEnabled(bool);
	}

	private void showAdd() {
		JTextField id = new JTextField(pwdId.getText());
		id.setEnabled(false);
		JTextField nm = new JTextField();
		JTextField fl = new JTextField();
		
		Object[] m = { "APP_ID:", id, "APP_NAME:", nm, "FILE_NAME:", fl };
		if (JOptionPane.showConfirmDialog(this, m, "Add", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
			runCmd("create", id.getText(), nm.getText(), fl.getText());
		}
	}

	private void showIcon() {
		JTextField p = new JTextField(System.getProperty("user.home") + "/Downloads/");
		JTextField id = new JTextField(pwdId.getText());
		id.setEnabled(false);
		Object[] m = { "PNG_PATH:", p, "APP_ID:", id };
		
		if (JOptionPane.showConfirmDialog(this, m, "Icon", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
			String path = p.getText().trim();
			File file = new File(path);

			String lowerName = file.getName().toLowerCase();
			boolean isImageExt = lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
					|| lowerName.endsWith(".svg");

			if (!file.isFile() || !isImageExt) {
				showError(path + " is not a valid image file! (.png, .jpg, .svg)");
				return;
			}
			runCmd("icon", p.getText(), id.getText());
		}
	}

	private void doRename() {
		JTextField nm = new JTextField();

		String oldContent = contentDisplay.getText();
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?m)^Name=(.*)").matcher(oldContent);
		if (matcher.find()) {
			String existingName = matcher.group(1).trim();
			nm.setText(existingName);
		}

		Object[] m = { "APP_NAME:", nm };

		if (JOptionPane.showConfirmDialog(this, m, "ReName", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
			String newName = nm.getText().trim();
			if (newName.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			String updatedContent = oldContent.replaceAll("(?m)^Name=.*", "Name=" + newName);

			if (currentSelectedFile != null) {
				try {
					java.io.File targetFile = new java.io.File(BASE_PATH, currentSelectedFile);

					java.nio.file.Files.write(targetFile.toPath(),
							updatedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

					contentDisplay.setText(updatedContent);
					filterFiles();

					JOptionPane.showMessageDialog(this,
							"Successfully saved to disk!\nPath: " + targetFile.getAbsolutePath(), "Success",
							JOptionPane.INFORMATION_MESSAGE);

				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, "Failed to write file:\n" + e.toString(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			} else {
				JOptionPane.showMessageDialog(this, "No file selected!", "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}

	}

	private void doDelete() {
		if (currentSelectedFile != null && JOptionPane.showConfirmDialog(this, "Delete?", "Confirm",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
			new File(BASE_PATH, currentSelectedFile).delete();
			filterFiles();
			contentDisplay.setText("");
		}
	}

	private void showError(String finalErrorMsg) {
		SwingUtilities.invokeLater(() -> {
			javax.swing.JOptionPane.showMessageDialog(this, finalErrorMsg, "PWA",
					javax.swing.JOptionPane.ERROR_MESSAGE);
		});
	}

	private void runCmd(String... a) {
		new Thread(() -> {
			try {
				String p = System.getProperty("user.dir") + File.separator + "pwa";
				ArrayList<String> c = new ArrayList<>();

				c.add(p);
				Collections.addAll(c, a);

				ProcessBuilder pb = new ProcessBuilder(c);
				pb.redirectErrorStream(true);
				Process process = pb.start();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						showError(line);
					}
				}

				int code = process.waitFor();
				if (code == 0) {
					SwingUtilities.invokeLater(this::filterFiles);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(() -> {
			PwaManager m = new PwaManager();
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			m.setSize(screenSize.width, screenSize.height);
			m.setLocationRelativeTo(null);
			m.setVisible(true);

			SwingUtilities.invokeLater(() -> {
				m.setExtendedState(MAXIMIZED_BOTH);
				m.splitPane.setDividerLocation(450);
			});
		});
	}
}
