
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.*;
import javax.swing.tree.*;

public class FileExplorer {

    public static final String APP_TITLE = "FileExplorer";
    private Desktop desktop;
    private FileSystemView fileSystemView;
    private File currentFile;
    private JPanel mainFrame;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTable table;
    private JProgressBar progressBar;
    private FileTableModel fileTableModel;
    private ListSelectionListener listSelectionListener;

    private JButton openFile;
    private JButton deleteFile;
    private JButton newFile;
    private JButton copyFile;
    private JButton moveFile;
    private JButton pasteFile;
    private JLabel fileDir;
    private JTextField path;
    private JPanel newFileDirectoryPanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    Boolean canPaste = false, canMove = false;

    public Container UI() {
        if (mainFrame == null) {
            mainFrame = new JPanel(new BorderLayout(3, 3));
            mainFrame.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            // Topmost view : details of the selected file/directory

            JPanel fileDetails = new JPanel(new BorderLayout(2, 2));
            fileDetails.setBorder(new EmptyBorder(0, 6, 20, 6));

            JPanel fileLabels = new JPanel(new GridLayout(0, 1, 2, 2));
            fileDetails.add(fileLabels, BorderLayout.WEST);

            JPanel fileValues = new JPanel(new GridLayout(0, 1, 2, 2));
            fileDetails.add(fileValues, BorderLayout.CENTER);

            fileLabels.add(new JLabel("File/Directory", JLabel.LEADING));
            fileDir = new JLabel();
            fileValues.add(fileDir);

            fileLabels.add(new JLabel("Path/name", JLabel.LEADING));
            path = new JTextField(5);
            path.setEditable(false);
            fileValues.add(path);

            // toolbar

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);

            newFile = new JButton(new ImageIcon(
                    "images/file.png"));
            newFile.setToolTipText("<html><div style='padding: -10; background:white; '>New</div></html>");
            newFile.setMnemonic('n');
            newFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            newFile();
                        }
                    });
            toolBar.add(newFile);

            toolBar.addSeparator();

            openFile = new JButton(new ImageIcon(
                    "images/open.png"));
            openFile.setToolTipText("<html><div style='padding: -10; background:white; '>Open</div></html>");
            openFile.setMnemonic('o');

            openFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            try {
                                desktop.open(currentFile);
                            } catch (Throwable t) {
                                showThrowable(t);
                            }
                            mainFrame.repaint();
                        }
                    });
            toolBar.add(openFile);

            copyFile = new JButton(new ImageIcon(
                    "images/copy.png"));
            copyFile.setToolTipText("<html><div style='padding: -10; background:white; '>Copy</div></html>");
            copyFile.setMnemonic('c');
            copyFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            copyFile();
                        }
                    });
            toolBar.add(copyFile);

            moveFile = new JButton(new ImageIcon(
                    "images/move.png"));
            moveFile.setToolTipText("<html><div style='padding: -10; background:white; '>Move</div></html>");
            moveFile.setMnemonic('m');
            moveFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            canMove = true;
                            moveFile();
                        }
                    });
            toolBar.add(moveFile);

            pasteFile = new JButton(new ImageIcon(
                    "images/paste.png"));
            pasteFile.setToolTipText("<html><div style='padding: -10; background:white; '>Paste</div></html>");
            pasteFile.setMnemonic('p');
            pasteFile.setEnabled(false);
            pasteFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            pasteFile();
                        }
                    });
            toolBar.add(pasteFile);

            JButton renameFile = new JButton(new ImageIcon(
                    "images/rename.png"));
            renameFile.setToolTipText("<html><div style='padding: -10; background:white; '>Rename</div></html>");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            renameFile();
                        }
                    });
            toolBar.add(renameFile);

            deleteFile = new JButton(new ImageIcon(
                    "images/delete.png"));
            deleteFile.setToolTipText("<html><div style='padding: -10; background:white; '>Delete</div></html>");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            deleteFile();
                        }
                    });
            toolBar.add(deleteFile);

            toolBar.addSeparator();

            // View that holds the toolbar and file details

            JPanel TopView = new JPanel(new BorderLayout(3, 3));

            TopView.add(toolBar, BorderLayout.NORTH);
            TopView.add(fileDetails, BorderLayout.CENTER);

            mainFrame.add(TopView, BorderLayout.NORTH);

            // Lower view containing table and hierarchy

            JPanel LowerView = new JPanel(new BorderLayout(3, 3));

            table = new JTable();
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            table.getTableHeader().setOpaque(true);
            table.getTableHeader().setBackground(Color.BLUE);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);

            listSelectionListener = new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    setFileDetails(((FileTableModel) table.getModel()).getFile(row));
                }
            };

            table.getSelectionModel().addListSelectionListener(listSelectionListener);

            JScrollPane fileDirTable = new JScrollPane(table);
            Dimension dimension = fileDirTable.getPreferredSize();
            fileDirTable.setPreferredSize(
                    new Dimension(800, (int) dimension.getHeight() / 2));
            LowerView.add(fileDirTable, BorderLayout.CENTER);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent tse) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
                    showChildren(node);
                    setFileDetails((File) node.getUserObject());
                }
            };

            // show the file system roots.
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add(node);

                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                for (File file : files) {
                    if (file.isDirectory()) {
                        node.add(new DefaultMutableTreeNode(file));
                    }
                }

            }

            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            tree.setVisibleRowCount(20);

            JScrollPane treeHierarchy = new JScrollPane(tree);
            Dimension preferredSize = treeHierarchy.getPreferredSize();
            Dimension d = new Dimension(200, (int) preferredSize.getHeight());
            treeHierarchy.setPreferredSize(d);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeHierarchy, LowerView);
            mainFrame.add(splitPane, BorderLayout.CENTER);

            JPanel progressView = new JPanel(new BorderLayout(3, 3));
            progressBar = new JProgressBar();
            progressView.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            mainFrame.add(progressView, BorderLayout.SOUTH);
        }
        return mainFrame;
    }

    File copiedFile, movedFile;

    protected void pasteFile() {

        try {
            if (canPaste) {
                System.out.println("copy-paste");

                if (currentFile == null) {
                    JOptionPane.showMessageDialog(mainFrame, "No file is selected to paste", "Select File",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (currentFile.isDirectory()) {

                    System.out.println(copiedFile);
                    System.out.println(currentFile);

                    String path = currentFile.getAbsolutePath() + "/" + copiedFile.getName();

                    Path newPath = Paths.get(path);
                    System.out.println(newPath);

                    copyDir(copiedFile.toPath(), newPath);
                    TreePath filePath = findTreePath(currentFile);
                    DefaultMutableTreeNode fileNode = (DefaultMutableTreeNode) filePath.getLastPathComponent();

                    if (copiedFile.isDirectory()) {

                        TreePath currentPath = findTreePath(currentFile);

                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
                                .getLastPathComponent();

                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(copiedFile);

                        treeModel.insertNodeInto(newChild, (DefaultMutableTreeNode) currentNode, 0);

                    }

                    showChildren(fileNode);

                } else {

                    showErrorMessage("Can not copy a file/directory in another file.", "Copy-Paste Failed");
                }

            } else if (canMove) {
                System.out.println("move");

                if (currentFile == null) {
                    JOptionPane.showMessageDialog(mainFrame, "No file is selected to paste", "Select File",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (currentFile.isDirectory()) {

                    System.out.println(movedFile);
                    System.out.println(currentFile);

                    // new path for moving
                    String path = currentFile.getAbsolutePath() + "/" + movedFile.getName();
                    Path newPath = Paths.get(path);
                    System.out.println(newPath);

                    // inserting new node

                    copyDir(movedFile.toPath(), newPath);

                    // destination node
                    TreePath filePath = findTreePath(currentFile);
                    DefaultMutableTreeNode fileNode = (DefaultMutableTreeNode) filePath.getLastPathComponent();

                    // parent node of source
                    TreePath parentPath = findTreePath(movedFile.getParentFile());
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                    if (movedFile.isDirectory()) {

                        // creating new node for source
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(movedFile);

                        treeModel.insertNodeInto(newChild, (DefaultMutableTreeNode) fileNode, 0);

                    }

                    // Deleting previous node

                    if (movedFile.isFile()) {
                        movedFile.delete();
                    } else {

                        // source node
                        TreePath currentPath = findTreePath(movedFile);
                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
                                .getLastPathComponent();
                        deleteFolder(movedFile);
                        treeModel.removeNodeFromParent(currentNode);

                    }

                    showChildren(parentNode);
                    showChildren(fileNode);

                } else {

                    showErrorMessage("Can not copy a file/directory in another file.", "Copy-Paste Failed");
                }
            }

        } catch (Throwable t) {
            showThrowable(t);
        }

        mainFrame.repaint();
        pasteFile.setEnabled(false);
        canPaste = false;
        canMove = false;

    }

    public static void copyDir(Path src, Path dest) throws IOException {

        Files.walk(src)
                .forEach(source -> {
                    try {
                        Files.copy(source, dest.resolve(src.relativize(source)),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    protected void moveFile() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(mainFrame, "No file is selected to move", "Select File",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {

            movedFile = new File(currentFile.getParentFile(), currentFile.getName());
            pasteFile.setEnabled(true);
            canMove = true;

        } catch (Throwable t) {
            showThrowable(t);
        }
    }

    protected void copyFile() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(mainFrame, "No file is selected to copy", "Select File",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {

            copiedFile = new File(currentFile.getParentFile(), currentFile.getName());
            pasteFile.setEnabled(true);
            canPaste = true;

        } catch (Throwable t) {
            showThrowable(t);
        }

    }

    public void showRootFile() {
        // ensure the main files are displayed
        tree.setSelectionInterval(0, 0);
    }

    private void renameFile() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(mainFrame, "No file is selected to rename", "Select File",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newName = JOptionPane.showInputDialog(mainFrame, "New Name");
        if (newName != null) {
            try {
                boolean directory = currentFile.isDirectory();
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                File newFIle = new File(currentFile.getParentFile(), newName);
                boolean renamed = currentFile.renameTo(newFIle);
                if (renamed) {
                    if (directory) {

                        TreePath currentPath = findTreePath(currentFile);
                        System.out.println(currentPath);
                        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
                                .getLastPathComponent();

                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(newFIle);

                        treeModel.insertNodeInto(newChild, (DefaultMutableTreeNode) currentNode.getParent(), 0);
                        treeModel.removeNodeFromParent(currentNode);

                    }

                    showChildren(parentNode);
                } else {
                    String msg = "The file '" + currentFile + "' could not be renamed.";
                    showErrorMessage(msg, "Rename Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        mainFrame.repaint();
    }

    private void deleteFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected for deletion.", "Select File");
            return;
        }

        System.out.println("On delete");
        System.out.println(currentFile);

        int result = JOptionPane.showConfirmDialog(
                mainFrame,
                "Are you sure you want to delete this file?",
                "Delete File",
                JOptionPane.ERROR_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                TreePath parentPath = findTreePath(currentFile.getParentFile());
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                System.out.println(currentFile.isFile());

                if (currentFile.isFile()) {
                    System.out.println("Fileeeeee");
                    System.out.println(currentFile);
                    currentFile.delete();
                } else {

                    TreePath currentPath = findTreePath(currentFile);

                    DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath.getLastPathComponent();
                    deleteFolder(currentFile);
                    treeModel.removeNodeFromParent(currentNode);

                }

                showChildren(parentNode);
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        mainFrame.repaint();
    }

    static void deleteFolder(File file) {
        for (File subFile : file.listFiles()) {
            if (subFile.isDirectory()) {
                deleteFolder(subFile);
            } else {
                subFile.delete();
            }
        }
        file.delete();
    }

    private void newFile() {
        if (currentFile == null) {
            showErrorMessage("No location selected for new file.", "Select Location");
            return;
        }

        if (newFileDirectoryPanel == null) {
            newFileDirectoryPanel = new JPanel(new BorderLayout(3, 3));

            JPanel southRadio = new JPanel(new GridLayout(1, 0, 2, 2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup buttonGrp = new ButtonGroup();
            buttonGrp.add(newTypeFile);
            buttonGrp.add(newTypeDirectory);
            southRadio.add(newTypeFile);
            southRadio.add(newTypeDirectory);

            name = new JTextField(15);

            newFileDirectoryPanel.add(new JLabel("Name"), BorderLayout.WEST);
            newFileDirectoryPanel.add(name);
            newFileDirectoryPanel.add(southRadio, BorderLayout.SOUTH);
        }

        int result = JOptionPane.showConfirmDialog(
                mainFrame, newFileDirectoryPanel, "Create File", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File(parentFile, name.getText());
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {

                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                    if (file.isDirectory()) {

                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);
                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }

                    showChildren(parentNode);
                } else {
                    String msg = file + " could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        mainFrame.repaint();
    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(mainFrame, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(mainFrame, t.toString(), t.getMessage(), JOptionPane.ERROR_MESSAGE);
        mainFrame.repaint();
    }

    private TreePath findTreePath(File find) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            TreePath treePath = tree.getPathForRow(i);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            File nodeFile = (File) node.getUserObject();

            if (nodeFile.equals(find)) {
                return treePath;
            }
        }
        return null;
    }

    /** Update the table on the EDT */
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        if (fileTableModel == null) {
                            fileTableModel = new FileTableModel();
                            table.setModel(fileTableModel);
                        }
                        table.getSelectionModel()
                                .removeListSelectionListener(listSelectionListener);
                        fileTableModel.setFiles(files);
                        table.getSelectionModel().addListSelectionListener(listSelectionListener);
                        resizeColumnWidth(table);

                    }
                });
    }

    // resizes column with window size

    public void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 15; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            if (width > 300)
                width = 300;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    /**
     * Add the files that are contained within the directory of this node. Thanks to
     * Hovercraft Full
     * Of Eels.
     */
    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true); // !!
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    setTableData(files);
                }
                return null;
            }

            @Override
            protected void process(List<File> chunks) {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                tree.setEnabled(true);
            }
        };
        worker.execute();
    }

    /** Update the File details view with the details of this File. */
    private void setFileDetails(File file) {
        currentFile = file;
        System.out.println(currentFile);
        Icon icon = fileSystemView.getSystemIcon(file);
        fileDir.setIcon(icon);
        fileDir.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());

        JFrame jframe = (JFrame) mainFrame.getTopLevelAncestor();
        if (jframe != null) {
            jframe.setTitle(APP_TITLE + " :: " + fileSystemView.getSystemDisplayName(file));
        }

        mainFrame.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } catch (Exception weTried) {
                        }
                        JFrame frame = new JFrame(APP_TITLE);
                        frame.setSize(1000, 500);
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                        FileExplorer fileExplorer = new FileExplorer();
                        frame.setContentPane(fileExplorer.UI());

                        frame.pack();
                        frame.setLocationByPlatform(true);
                        frame.setMinimumSize(frame.getSize());
                        frame.setVisible(true);

                        fileExplorer.showRootFile();
                    }
                });
    }
}

/** A TableModel to hold File[]. */
class FileTableModel extends AbstractTableModel {

    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {
            "Type", "Name", "Path", "Size", "Last Modified",
    };

    FileTableModel() {
        this(new File[0]);
    }

    FileTableModel(File[] files) {
        this.files = files;
    }

    public Object getValueAt(int row, int column) {
        File file = files[row];
        switch (column) {
            case 0:
                return fileSystemView.getSystemIcon(file);
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return file.lastModified();

            default:
                System.err.println("Logic Error");
        }
        return "";
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 5:
                return Boolean.class;
        }
        return String.class;
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return files.length;
    }

    public File getFile(int row) {
        return files[row];
    }

    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }
}

// HEREEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEeeee

/** A TreeCellRenderer for a File. */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;

    private JLabel label;

    FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        File file = (File) node.getUserObject();
        label.setIcon(fileSystemView.getSystemIcon(file));
        label.setText(fileSystemView.getSystemDisplayName(file));
        label.setToolTipText(file.getPath());

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}