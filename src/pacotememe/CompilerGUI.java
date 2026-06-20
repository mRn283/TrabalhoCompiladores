package pacotememe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.List;

public class CompilerGUI extends JFrame {

    private JTextArea txtEditor;
    private JTextArea txtErrors;
    private JTable tblTokens;
    private JTree treeSyntax;
    private JLabel lblStatus;
    private JLabel lblLineCol;
    private File currentFile = null;
    private static gramaticameme parserInstance = null;

    // Dark Mode Theme Colors
    private static final Color COLOR_BG_DARK = new Color(30, 30, 30);
    private static final Color COLOR_BG_LIGHTER = new Color(45, 45, 45);
    private static final Color COLOR_BG_EDITOR = new Color(20, 20, 20);
    private static final Color COLOR_FG = new Color(220, 220, 220);
    private static final Color COLOR_FG_MUTED = new Color(130, 130, 130);
    private static final Color COLOR_ACCENT = new Color(14, 99, 156);
    private static final Color COLOR_SUCCESS = new Color(76, 175, 80);
    private static final Color COLOR_ERROR = new Color(244, 67, 54);

    public CompilerGUI() {
        super("Compilador CompilaMeme - IDE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        // Customize UI Theme globally
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // Keep default if look and feel settings fail
        }

        initComponents();
        setupMenus();
        updateLineColLabel();
    }

    private void initComponents() {
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG_DARK);
        setContentPane(mainPanel);

        // Create Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBackground(COLOR_BG_LIGHTER);
        toolbar.setBorder(new LineBorder(COLOR_BG_DARK, 1));

        JButton btnCompile = createStyledButton("Compilar (F5)", COLOR_ACCENT, Color.WHITE);
        btnCompile.addActionListener(e -> runCompiler());

        JButton btnClear = createStyledButton("Limpar Tudo", COLOR_BG_DARK, COLOR_FG);
        btnClear.addActionListener(e -> clearAll());

        toolbar.add(btnCompile);
        toolbar.add(btnClear);
        mainPanel.add(toolbar, BorderLayout.NORTH);

        // Left: Editor Panel with line numbering
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBackground(COLOR_BG_DARK);
        editorPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(COLOR_BG_LIGHTER), "Editor de Código", 0, 0,
                new Font("Segoe UI", Font.BOLD, 12), COLOR_FG));

        txtEditor = new JTextArea();
        txtEditor.setBackground(COLOR_BG_EDITOR);
        txtEditor.setForeground(COLOR_FG);
        txtEditor.setCaretColor(Color.WHITE);
        txtEditor.setFont(new Font("Consolas", Font.PLAIN, 15));
        txtEditor.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Add tab size adjustment
        txtEditor.setTabSize(4);

        JScrollPane scrollEditor = new JScrollPane(txtEditor);
        scrollEditor.setBorder(null);

        // Custom Gutter for Line Numbers
        LineNumberGutter gutter = new LineNumberGutter(txtEditor);
        scrollEditor.setRowHeaderView(gutter);

        editorPanel.add(scrollEditor, BorderLayout.CENTER);

        // Editor Footer (line / column display)
        JPanel editorFooter = new JPanel(new BorderLayout());
        editorFooter.setBackground(COLOR_BG_LIGHTER);
        lblLineCol = new JLabel(" Linha: 1, Coluna: 1 ");
        lblLineCol.setForeground(COLOR_FG_MUTED);
        lblLineCol.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        editorFooter.add(lblLineCol, BorderLayout.EAST);
        
        txtEditor.addCaretListener(e -> updateLineColLabel());
        editorPanel.add(editorFooter, BorderLayout.SOUTH);

        // Right Panel: Tabbed pane for JTree (Syntax Tree) and JTable (Tokens List)
        JTabbedPane rightTabbedPane = new JTabbedPane();
        rightTabbedPane.setBackground(COLOR_BG_LIGHTER);
        rightTabbedPane.setForeground(COLOR_FG);
        rightTabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Custom tree
        treeSyntax = new JTree(new DefaultMutableTreeNode("Árvore Sintática (Compile para carregar)"));
        treeSyntax.setBackground(COLOR_BG_EDITOR);
        treeSyntax.setForeground(COLOR_FG);
        // Make the JTree text contrast with background
        treeSyntax.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                setBackgroundNonSelectionColor(COLOR_BG_EDITOR);
                setBackgroundSelectionColor(COLOR_ACCENT);
                setTextNonSelectionColor(COLOR_FG);
                setTextSelectionColor(Color.WHITE);
                return this;
            }
        });
        JScrollPane scrollTree = new JScrollPane(treeSyntax);
        scrollTree.setBorder(null);
        rightTabbedPane.addTab("Árvore Sintática", scrollTree);

        // Custom Tokens table
        String[] columnNames = {"Linha", "Coluna", "Token / Tipo", "Lexema"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblTokens = new JTable(model);
        tblTokens.setBackground(COLOR_BG_EDITOR);
        tblTokens.setForeground(COLOR_FG);
        tblTokens.setGridColor(COLOR_BG_LIGHTER);
        tblTokens.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblTokens.getTableHeader().setBackground(COLOR_BG_LIGHTER);
        tblTokens.getTableHeader().setForeground(COLOR_FG);
        tblTokens.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JScrollPane scrollTable = new JScrollPane(tblTokens);
        scrollTable.setBorder(null);
        rightTabbedPane.addTab("Tokens Reconhecidos", scrollTable);

        // Split Editor and Right Tabbed Pane
        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, rightTabbedPane);
        horizontalSplit.setDividerLocation(550);
        horizontalSplit.setDividerSize(5);
        horizontalSplit.setBorder(null);
        horizontalSplit.setBackground(COLOR_BG_DARK);

        // Bottom Panel: Acceptance Status & Error Output
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(COLOR_BG_DARK);
        bottomPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(COLOR_BG_LIGHTER), "Console de Compilação", 0, 0,
                new Font("Segoe UI", Font.BOLD, 12), COLOR_FG));

        // Acceptance text
        lblStatus = new JLabel("Status: Aguardando compilação...");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(COLOR_FG);
        lblStatus.setBorder(new EmptyBorder(5, 5, 5, 5));
        bottomPanel.add(lblStatus, BorderLayout.NORTH);

        // Error Messages textarea
        txtErrors = new JTextArea(6, 50);
        txtErrors.setEditable(false);
        txtErrors.setBackground(COLOR_BG_EDITOR);
        txtErrors.setForeground(COLOR_FG);
        txtErrors.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtErrors.setBorder(new EmptyBorder(5, 5, 5, 5));
        JScrollPane scrollErrors = new JScrollPane(txtErrors);
        scrollErrors.setBorder(null);
        bottomPanel.add(scrollErrors, BorderLayout.CENTER);

        // Split Top Panel and Bottom Panel
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontalSplit, bottomPanel);
        verticalSplit.setDividerLocation(460);
        verticalSplit.setDividerSize(5);
        verticalSplit.setBorder(null);
        verticalSplit.setBackground(COLOR_BG_DARK);

        mainPanel.add(verticalSplit, BorderLayout.CENTER);

        // Register F5 key binding for compilation
        btnCompile.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "compile");
        btnCompile.getActionMap().put("compile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runCompiler();
            }
        });
    }

    private void setupMenus() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_BG_LIGHTER);
        menuBar.setBorder(new LineBorder(COLOR_BG_DARK, 1));

        JMenu menuFile = new JMenu("Arquivo");
        menuFile.setForeground(COLOR_FG);
        menuFile.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JMenuItem itemOpen = new JMenuItem("Abrir Código");
        itemOpen.setBackground(COLOR_BG_LIGHTER);
        itemOpen.setForeground(COLOR_FG);
        itemOpen.addActionListener(e -> openFile());

        JMenuItem itemSave = new JMenuItem("Salvar Código");
        itemSave.setBackground(COLOR_BG_LIGHTER);
        itemSave.setForeground(COLOR_FG);
        itemSave.addActionListener(e -> saveFile());

        JMenuItem itemExit = new JMenuItem("Sair");
        itemExit.setBackground(COLOR_BG_LIGHTER);
        itemExit.setForeground(COLOR_FG);
        itemExit.addActionListener(e -> System.exit(0));

        menuFile.add(itemOpen);
        menuFile.add(itemSave);
        menuFile.addSeparator();
        menuFile.add(itemExit);
        menuBar.add(menuFile);

        setJMenuBar(menuBar);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bg.darker(), 1),
                new EmptyBorder(5, 12, 5, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                txtEditor.read(reader, null);
                setTitle("Compilador CompilaMeme - " + currentFile.getName());
                lblStatus.setText("Status: Arquivo " + currentFile.getName() + " carregado.");
                lblStatus.setForeground(COLOR_FG);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            } else {
                return;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
            txtEditor.write(writer);
            setTitle("Compilador CompilaMeme - " + currentFile.getName());
            lblStatus.setText("Status: Arquivo " + currentFile.getName() + " salvo com sucesso.");
            lblStatus.setForeground(COLOR_SUCCESS);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateLineColLabel() {
        try {
            int caretPos = txtEditor.getCaretPosition();
            int line = txtEditor.getLineOfOffset(caretPos);
            int column = caretPos - txtEditor.getLineStartOffset(line);
            lblLineCol.setText(String.format(" Linha: %d, Coluna: %d ", line + 1, column + 1));
        } catch (Exception e) {
            lblLineCol.setText(" Linha: 1, Coluna: 1 ");
        }
    }

    private void clearAll() {
        txtEditor.setText("");
        txtErrors.setText("");
        lblStatus.setText("Status: Aguardando compilação...");
        lblStatus.setForeground(COLOR_FG);
        currentFile = null;
        setTitle("Compilador CompilaMeme - IDE");

        DefaultTableModel model = (DefaultTableModel) tblTokens.getModel();
        model.setRowCount(0);

        treeSyntax.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Árvore Sintática (Compile para carregar)")));
    }

    private void runCompiler() {
        String code = txtEditor.getText();
        if (code.trim().isEmpty()) {
            lblStatus.setText("Status: Digite algum código para compilar.");
            lblStatus.setForeground(COLOR_ERROR);
            return;
        }

        txtErrors.setText("");

        // Initialize parser static state if needed, or ReInit
        try {
            if (parserInstance == null) {
                parserInstance = new gramaticameme(new StringReader(code));
            } else {
                gramaticameme.ReInit(new StringReader(code));
            }
        } catch (Throwable t) {
            // Ignore for now
        }

        // 1. Lexical pass using the static token source
        DefaultTableModel model = (DefaultTableModel) tblTokens.getModel();
        model.setRowCount(0);

        try {
            while (true) {
                Token t = gramaticameme.token_source.getNextToken();
                if (t == null || t.kind == gramaticamemeConstants.EOF) {
                    break;
                }
                
                String tokenKindName = gramaticamemeConstants.tokenImage[t.kind];
                if (t.kind == (gramaticamemeConstants.tokenImage.length - 1) && tokenKindName.equals("<INVALID>")) {
                    tokenKindName = "INVÁLIDO (Erro Léxico)";
                }

                model.addRow(new Object[]{t.beginLine, t.beginColumn, tokenKindName, t.image});
            }
        } catch (Throwable e) {
            // Catch lexical manager errors
        }

        // 2. Syntactic parsing and AST construction
        gramaticameme.listaErros.clear();
        ASTNode rootNode = null;
        boolean parserCrashed = false;
        String crashMsg = "";

        try {
            // Re-initialize for the parsing pass
            gramaticameme.ReInit(new StringReader(code));
            rootNode = gramaticameme.Programa();
        } catch (ParseException e) {
            gramaticameme.registrarErro(e);
        } catch (TokenMgrError e) {
            // Fallback for lexical crashes
            gramaticameme.listaErros.add("Erro Léxico: " + e.getMessage());
            parserCrashed = true;
            crashMsg = e.getMessage();
        } catch (Exception e) {
            gramaticameme.listaErros.add("Erro Inesperado: " + e.getMessage());
            parserCrashed = true;
            crashMsg = e.getMessage();
        }

        // Display results
        if (gramaticameme.listaErros.isEmpty() && !parserCrashed && rootNode != null) {
            lblStatus.setText("Status: Aceito! Código sintaticamente correto.");
            lblStatus.setForeground(COLOR_SUCCESS);
            txtErrors.setText("Nenhum erro léxico ou sintático encontrado.");
            
            // Build tree
            DefaultMutableTreeNode swingRoot = buildSwingTree(rootNode);
            treeSyntax.setModel(new DefaultTreeModel(swingRoot));
            expandAllNodes(treeSyntax, 0, treeSyntax.getRowCount());
        } else {
            lblStatus.setText("Status: Erros sintáticos/léxicos detectados.");
            lblStatus.setForeground(COLOR_ERROR);

            StringBuilder sb = new StringBuilder();
            for (String err : gramaticameme.listaErros) {
                sb.append(err).append("\n");
            }
            txtErrors.setText(sb.toString());

            // Build partial tree (including error nodes if any)
            if (rootNode != null) {
                DefaultMutableTreeNode swingRoot = buildSwingTree(rootNode);
                treeSyntax.setModel(new DefaultTreeModel(swingRoot));
                expandAllNodes(treeSyntax, 0, treeSyntax.getRowCount());
            } else {
                treeSyntax.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Erro ao gerar Árvore Sintática")));
            }
        }
    }

    private DefaultMutableTreeNode buildSwingTree(ASTNode node) {
        if (node == null) return null;
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(node.toString());
        for (ASTNode child : node.getChildren()) {
            DefaultMutableTreeNode childSwingNode = buildSwingTree(child);
            if (childSwingNode != null) {
                swingNode.add(childSwingNode);
            }
        }
        return swingNode;
    }

    private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }
        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    // Gutter component for Line Numbers
    private static class LineNumberGutter extends JTextArea {
        private final JTextArea textArea;

        public LineNumberGutter(JTextArea textArea) {
            this.textArea = textArea;
            setBackground(COLOR_BG_DARK);
            setForeground(COLOR_FG_MUTED);
            setFont(new Font("Consolas", Font.PLAIN, 15));
            setEditable(false);
            setFocusable(false);
            setBorder(new EmptyBorder(5, 8, 5, 8));
            
            textArea.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { updateLines(); }
                @Override
                public void removeUpdate(DocumentEvent e) { updateLines(); }
                @Override
                public void changedUpdate(DocumentEvent e) { updateLines(); }
            });
            updateLines();
        }

        private void updateLines() {
            int lineCount = textArea.getLineCount();
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                sb.append(i).append("\n");
            }
            setText(sb.toString());
        }
    }
}
