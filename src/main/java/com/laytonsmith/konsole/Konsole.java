package com.laytonsmith.konsole;

import com.laytonsmith.exObjects.exObject;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;


/**
 * The konsole is a much simpler solution to viewing System.err messages, instead
 * of having to launch the program from a konsole. This makes it possible to easily
 * help a remote user debug a problem. It is also possible to programmatically pump
 * messages to the konsole, even if we are not redirecting System.err messages.
 * The window itself is not directly accessable by outside files, use the static
 * methods provided. Note that only one konsole window may exist at any given time.
 * @author Layton
 */
public class Konsole extends javax.swing.JFrame {

    private static Konsole konsole;
    private static KonsoleEnv env;
    private static int historyIndex = 0;
    private static ArrayList<Kommand> cmdHistory = new ArrayList<Kommand>();
    private static String curCmd = "";
    private static boolean largeText = false;
    private static File cwd;
    private static boolean tabNext = false;
    private static boolean cmdRunning = false;
    private static String OS;
    private static boolean isUnix = false;
    private static String coutBuffer;
    private static String remoteUser;
    private static String allowRemoteFrom;

    private static long lastCommand = 0;

    private static final Object cmdRunningLock = new Object();

    public static Konsole get_console() {
        if (konsole == null) {
            if (EventQueue.isDispatchThread()) {
                konsole = new Konsole();
            } else {
                try {
                    EventQueue.invokeAndWait(new Runnable() {

                        public void run() {
                            konsole = new Konsole();
                        }
                    });
                } catch (Exception ex) {
                    throw new RuntimeException("Could not create konsole");
                }
            }
        }
        return konsole;
    }

    /** Creates new form Konsole */
    private Konsole() {
        System.out.println("Konsole initializing");
        initComponents();
        MessageConsole x = new MessageConsole(output);
        x.redirectErr(Color.RED, System.err);
        x.redirectOut(Color.GREEN, System.out);
        output.setForeground(Color.WHITE);
        try {
            cwd = new File(".").getCanonicalFile();
        } catch (IOException ex) {
        }
        env = new KonsoleEnv();
        //set the initial env vars
        env.addEnv("base", new exObject(new File(".").getAbsolutePath()));
        env.addEnv("clines", new exObject("5"));
        String name = System.getProperty("os.name");
        //Import some of the environmental variables
        if (name.matches("Windows.*")) {
            env.addEnv("path", new exObject(System.getenv("Path")));
            env.addEnv("home", new exObject(System.getenv("HOMEPATH")));
            OS = "windows";
            isUnix = false;
        } else if (name.matches("Linux.*")) {
            env.addEnv("path", new exObject(System.getenv("Path")));
            env.addEnv("home", new exObject(System.getenv("HOME")));
            OS = "linux";
            isUnix = true;
        } else if (name.matches("Mac.*")) {
            env.addEnv("path", new exObject(System.getenv("Path")));
            env.addEnv("home", new exObject(System.getenv("HOME")));
            OS = "mac";
            isUnix = true;
        } else {
            if (System.getenv("path") != null) {
                env.addEnv("path", new exObject(System.getenv("path")));
            }
            if (System.getenv("home") != null) {
                env.addEnv("home", new exObject(System.getenv("home")));
            }
            OS = "unknown";
            isUnix = false;
        }

        menu_mode_normal.setSelected(true);
        
    }

    /**
     * Writes a line to the konsole window
     * @param s
     */
    public static void println(Object s) {
        print(s + "\n");
    }

    /****************************************
     * Overloaded print statements
     ***************************************/
    // <editor-fold defaultstate="collapsed" desc="Overloaded prints">
    public static void println(int i) {
        println(new exObject(i).toString());
    }

    public static void println(short i) {
        println(new exObject(i).toString());
    }

    public static void println(long i) {
        println(new exObject(i).toString());
    }

    public static void println(float i) {
        println(new exObject(i).toString());
    }

    public static void println(double i) {
        println(new exObject(i).toString());
    }

    public static void println(boolean i) {
        println(new exObject(i).toString());
    }

    public static void println(char i) {
        println(new exObject(i).toString());
    }

    public static void println(byte i) {
        println(new exObject(i).toString());
    }

    public static void printerr(int i) {
        printerr(new exObject(i).toString());
    }

    public static void printerr(short i) {
        printerr(new exObject(i).toString());
    }

    public static void printerr(long i) {
        printerr(new exObject(i).toString());
    }

    public static void printerr(float i) {
        printerr(new exObject(i).toString());
    }

    public static void printerr(double i) {
        printerr(new exObject(i).toString());
    }

    public static void printerr(boolean i) {
        printerr(new exObject(i).toString());
    }

    public static void printerr(char i) {
        printerr(new exObject(i).toString());
    }

    public static void printerr(byte i) {
        printerr(new exObject(i).toString());
    }
    //</editor-fold>

    /**
     * Deletes lines from the console output so we don't eat up too much memory
     */
    private void deleteLines() {
        int maxChars = 10000;
        if (Konsole.get_console().output.getText().length() > maxChars) {
            Konsole.get_console().output.setText(
                    Konsole.get_console().output.getText().substring(
                    Konsole.get_console().output.getText().length() - maxChars, Konsole.get_console().output.getText().length()));
        }
    }

    public static void printerr(Object s) {
        print(s + "\n", Color.ORANGE);
    }

    public static void print(Object s) {
        print(s, null);
    }

    /**
     * Writes a line to the konsole without adding a newline at the end
     * @param s
     */
    public static void print(Object s, Color w) {
        if (w == null) {
            w = Color.WHITE;
        }
        Konsole.open();
        try {
            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, w);
            javax.swing.text.Document document = Konsole.get_console().output.getDocument();
            int offset = document.getLength();
            document.insertString(offset, s.toString(), attributes);
            Konsole.get_console().output.setCaretPosition(document.getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(Konsole.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Opens the konsole window if it is closed. If it is already open, nothing
     * happens.
     */
    public static void open() {
        if (!Konsole.get_console().isVisible()) {
            Konsole.get_console().setVisible(true);
        }
        Konsole.get_console().cmdBox.requestFocusInWindow();
    }

    /**
     * Closes the konsole window if it is open. If it already closed, nothing
     * happens.
     */
    public static void close() {
        if (Konsole.get_console().isVisible()) {
            Konsole.get_console().setVisible(false);
        }
    }

    /**
     * Clears the konsole's screen.
     */
    public static void cls() {
        Konsole.get_console().output.setText("");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        menu_modes = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        output = new javax.swing.JTextPane();
        inputPanel = new javax.swing.JPanel();
        commandType = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        cmdBox = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        menu_mode_normal = new javax.swing.JRadioButtonMenuItem();
        menu_mode_java = new javax.swing.JRadioButtonMenuItem();
        menu_mode_groovy = new javax.swing.JRadioButtonMenuItem();
        menu_history = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        menu_cmds_cls = new javax.swing.JMenuItem();
        menu_cmds_echo = new javax.swing.JMenuItem();
        menu_cmds_google = new javax.swing.JMenuItem();
        menu_cmds_genv = new javax.swing.JMenuItem();
        menu_cmds_gscript = new javax.swing.JMenuItem();
        menu_cmds_ksh = new javax.swing.JMenuItem();
        menu_cmds_set = new javax.swing.JMenuItem();
        menu_cmds_cmd = new javax.swing.JMenuItem();
        menu_cmds_pwd = new javax.swing.JMenuItem();
        menu_cmds_ls = new javax.swing.JMenuItem();
        menu_cmds_cd = new javax.swing.JMenuItem();
        menu_cmds_exit = new javax.swing.JMenuItem();
        menu_cmds_help = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        menu_cmds_common_ipconfig = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        menu_help_help = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Konsole");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        output.setBackground(new java.awt.Color(0, 0, 0));
        output.setForeground(new java.awt.Color(255, 255, 255));
        output.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                outputCaretUpdate(evt);
            }
        });
        jScrollPane1.setViewportView(output);

        commandType.setText(">");

        cmdBox.setColumns(20);
        cmdBox.setRows(1);
        cmdBox.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                cmdBoxKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                cmdBoxKeyTyped(evt);
            }
        });
        jScrollPane2.setViewportView(cmdBox);

        javax.swing.GroupLayout inputPanelLayout = new javax.swing.GroupLayout(inputPanel);
        inputPanel.setLayout(inputPanelLayout);
        inputPanelLayout.setHorizontalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(commandType)
                .addGap(10, 10, 10)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
                .addContainerGap())
        );
        inputPanelLayout.setVerticalGroup(
            inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(inputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandType))
                .addContainerGap())
        );

        jMenu1.setText("File");

        jMenu2.setText("Mode");

        menu_modes.add(menu_mode_normal);
        menu_mode_normal.setSelected(true);
        menu_mode_normal.setText("Normal");
        menu_mode_normal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_mode_normalActionPerformed(evt);
            }
        });
        jMenu2.add(menu_mode_normal);

        menu_modes.add(menu_mode_java);
        menu_mode_java.setText("Java");
        menu_mode_java.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_mode_javaActionPerformed(evt);
            }
        });
        jMenu2.add(menu_mode_java);

        menu_modes.add(menu_mode_groovy);
        menu_mode_groovy.setText("Groovy");
        menu_mode_groovy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_mode_groovyActionPerformed(evt);
            }
        });
        jMenu2.add(menu_mode_groovy);

        jMenu1.add(jMenu2);

        menu_history.setText("History");
        jMenu1.add(menu_history);

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
        jMenuItem1.setText("Close Konsole");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("Cmds");

        jMenu4.setText("Builtin");

        menu_cmds_cls.setText("cls");
        menu_cmds_cls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_clsActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_cls);

        menu_cmds_echo.setText("echo...");
        menu_cmds_echo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_echoActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_echo);

        menu_cmds_google.setText("google...");
        menu_cmds_google.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_googleActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_google);

        menu_cmds_genv.setText("genv");
        menu_cmds_genv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_genvActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_genv);

        menu_cmds_gscript.setText("gscript...");
        menu_cmds_gscript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_gscriptActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_gscript);

        menu_cmds_ksh.setText("ksh...");
        menu_cmds_ksh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_kshActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_ksh);

        menu_cmds_set.setText("set...");
        menu_cmds_set.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_setActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_set);

        menu_cmds_cmd.setText("cmd");
        menu_cmds_cmd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_cmdActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_cmd);

        menu_cmds_pwd.setText("pwd");
        menu_cmds_pwd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_pwdActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_pwd);

        menu_cmds_ls.setText("ls...");
        menu_cmds_ls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_lsActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_ls);

        menu_cmds_cd.setText("cd...");
        menu_cmds_cd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_cdActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_cd);

        menu_cmds_exit.setText("exit...");
        menu_cmds_exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_exitActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_exit);

        menu_cmds_help.setText("help...");
        menu_cmds_help.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_helpActionPerformed(evt);
            }
        });
        jMenu4.add(menu_cmds_help);

        jMenu3.add(jMenu4);

        jMenu5.setText("Common");
        jMenu5.setToolTipText("<html>These are common OS commands that are invoked<br />in an OS independant way. These commands are not inherently available to scripts.");

        menu_cmds_common_ipconfig.setText("ipconfig/ifconfig");
        menu_cmds_common_ipconfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_cmds_common_ipconfigActionPerformed(evt);
            }
        });
        jMenu5.add(menu_cmds_common_ipconfig);

        jMenu3.add(jMenu5);

        jMenuBar1.add(jMenu3);

        jMenu6.setText("Help");

        menu_help_help.setText("Help");
        menu_help_help.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_help_helpActionPerformed(evt);
            }
        });
        jMenu6.add(menu_help_help);

        jMenuBar1.add(jMenu6);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(inputPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void outputCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_outputCaretUpdate
        if (!this.isVisible()) {
            this.setVisible(true);
            this.toFront();
        }
        deleteLines();
    }//GEN-LAST:event_outputCaretUpdate

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.setVisible(false);
    }//GEN-LAST:event_formWindowClosing

    private void cmdBoxKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cmdBoxKeyPressed
        if (evt.getKeyCode() != KeyEvent.VK_TAB) {
            tabNext = false;
        }
        if (evt.getKeyCode() == KeyEvent.VK_SCROLL_LOCK) {
            Integer clines = 5;
            if (env.getEnv("clines") != null) {
                try {
                    clines = Integer.parseInt(env.getEnv("clines").toString());
                } catch (NumberFormatException e) {
                }
                if (clines < 2) {
                    clines = 2;
                }
                if (clines > 15) {
                    clines = 15;
                }
            }
            if (largeText) {
                runCmd();
                cmdBox.setRows(1);
                largeText = false;
            } else {
                cmdBox.setRows(clines);
                largeText = true;
            }
            inputPanel.validate();
            this.pack();
        }
        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            cmdBox.setText("");
            Konsole.close();
        }
        if (evt.getKeyCode() == KeyEvent.VK_UP) {
            if (!largeText) {
                if (historyIndex == 0) {
                    curCmd = cmdBox.getText();
                }
                if (historyIndex < cmdHistory.size()) {
                    cmdBox.setText(cmdHistory.get(historyIndex).cmd);
                    historyIndex++;
                }
                if (cmdHistory.size() == historyIndex) {
                    historyIndex = cmdHistory.size() - 1;
                }
            }
        } else if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
            if (!largeText) {
                if (historyIndex > 1) {
                    cmdBox.setText(cmdHistory.get(historyIndex).cmd);
                    historyIndex--;
                } else {
                    cmdBox.setText(curCmd);
                    historyIndex = 0;
                }
            }
        } else if (evt.getKeyCode() == KeyEvent.VK_TAB) {
            if (!cmdBox.getText().trim().equals("")) {
                if (tabNext) {
                    //raw regex: [^\s"]+|"(?:[^"\\]|\\.)*(?:\\|"|$)
                    Pattern p = Pattern.compile("[^\\s\"]+|\"(?:[^\"\\\\]|\\\\.)*(?:\\\\|\"|$)");
                    Matcher m = p.matcher(cmdBox.getText());
                    String lastParam = "";
                    boolean subLast = false;
                    ArrayList<String> params = new ArrayList<String>();
                    int paramIndex = -1;
                    while (m.find()) {
                        params.add(m.group(0).trim());
                        lastParam = m.group(0).trim();
                        paramIndex++;
                    }

                    if (lastParam.length() > 0) {
                        if (lastParam.charAt(0) == '"') {
                            lastParam = lastParam.substring(1);
                        }
                        if (lastParam.charAt(lastParam.length() - 1) == '"' && lastParam.charAt(lastParam.length() - 2) != '\\') {
                            lastParam = lastParam.substring(0, lastParam.length() - 1);
                        }
                        lastParam = lastParam.replace("\\\"", "\"");
                        String orig = lastParam;
                        try {
                            if (lastParam.charAt(lastParam.length() - 1) == System.getProperty("file.separator").charAt(0)
                                    && lastParam.length() > 3) {
                                lastParam = lastParam.substring(0, lastParam.length() - 1);
                            }
                        } catch (Exception e) {
                        }
                        File possible = new File(lastParam);
                        if (!possible.isAbsolute()) {
                            File r = null;
                            if (paramIndex > 1 && (new File(params.get(paramIndex - 1)).isAbsolute()
                                    || new File(cwd.getAbsolutePath(), params.get(paramIndex - 1)).isAbsolute())) {
                                //we need to concatenate to the previous argument instead of the current one
                                possible = new File(params.get(paramIndex - 1).substring(1).substring(0, params.get(paramIndex - 1).length() - 2), lastParam);
                                subLast = true;
                            } else {
                                possible = new File(cwd.getAbsolutePath(), lastParam);
                            }
                        }
                        File dir;
                        String filename;
                        if (possible.isDirectory() || orig.substring(orig.length() - 1).equals(System.getProperty("file.separator"))) {
                            dir = possible;
                            filename = "";
                        } else {
                            String file = possible.getAbsolutePath();
                            int last = file.lastIndexOf(System.getProperty("file.separator"));
                            dir = new File(file.substring(0, last));
                            filename = file.substring(last + 1);
                        }

                        if (dir.toString().length() == 2) {
                            dir = new File(dir.toString() + "\\");
                        }

                        String[] names = dir.list();

                        ArrayList<String> matches = new ArrayList<String>();
                        if (names != null) {
                            for (int i = 0; i < names.length; i++) {
                                if (names[i].matches(filename + ".*")) {
                                    matches.add(names[i]);
                                }
                            }
                        }
                        if (matches.isEmpty()) {
                            Toolkit.getDefaultToolkit().beep();
                        } else if (matches.size() == 1) {
                            filename = matches.get(0);
                            File f = new File(dir, filename);
                            String newCmd = "";
                            for (int i = 0; i < params.size(); i++) {
                                if (i == params.size() - 1 || (subLast && i == params.size() - 2)) {
                                    String fi = f.getAbsolutePath();
                                    if (fi.contains(" ") || fi.contains("\"")) {
                                        fi = fi.replace("\"", "\\\"");
                                        fi = "\"" + fi + "\"";
                                        newCmd += fi;
                                        break;
                                    }
                                } else {
                                    String param = params.get(i);
                                    if (param.contains(" ")) {
                                        param = "\"" + param + "\"";
                                    }
                                    newCmd += param + " ";
                                }
                            }
                            cmdBox.setText(newCmd);
                        } else {
                            for (int i = 0; i < matches.size(); i++) {
                                Konsole.print(matches.get(i) + "; ");
                            }
                            Konsole.println("");
                        }
                    }
                } else {
                    tabNext = true;
                }
            }
            evt.consume();
        } else {
            historyIndex = 0;
        }
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!largeText && !cmdRunning) {
                runCmd();
                evt.consume();
            }
            if (cmdRunning) {
                cmdBox.setText("");
                evt.consume();
            }
        }
    }//GEN-LAST:event_cmdBoxKeyPressed

    private void menu_mode_normalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_mode_normalActionPerformed
        setMode(MODE.NORMAL);
    }//GEN-LAST:event_menu_mode_normalActionPerformed

    private void menu_mode_javaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_mode_javaActionPerformed
        setMode(MODE.JAVA);
    }//GEN-LAST:event_menu_mode_javaActionPerformed

    private void menu_mode_groovyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_mode_groovyActionPerformed
        setMode(MODE.GROOVY);
    }//GEN-LAST:event_menu_mode_groovyActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        cmdBox.setText("");
        Konsole.close();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void menu_help_helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_help_helpActionPerformed
        String inputValue = JOptionPane.showInputDialog(Konsole.get_console(), "What command would you like help with?");
        Konsole.runCmd("help " + inputValue, MODE.NORMAL);
    }//GEN-LAST:event_menu_help_helpActionPerformed

    private void menu_cmds_clsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_clsActionPerformed
        Konsole.runCmd("cls", MODE.NORMAL);
    }//GEN-LAST:event_menu_cmds_clsActionPerformed

    private void menu_cmds_echoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_echoActionPerformed
        cmdBox.setText("echo ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_echoActionPerformed

    private void menu_cmds_googleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_googleActionPerformed
        cmdBox.setText("google ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_googleActionPerformed

    private void menu_cmds_genvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_genvActionPerformed
        Konsole.runCmd("genv", MODE.NORMAL);
    }//GEN-LAST:event_menu_cmds_genvActionPerformed

    private void menu_cmds_gscriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_gscriptActionPerformed
        cmdBox.setText("gscript ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_gscriptActionPerformed

    private void menu_cmds_kshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_kshActionPerformed
        cmdBox.setText("ksh ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_kshActionPerformed

    private void menu_cmds_setActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_setActionPerformed
        cmdBox.setText("set ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_setActionPerformed

    private void menu_cmds_cmdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_cmdActionPerformed
        Konsole.runCmd("cmd", MODE.NORMAL);
    }//GEN-LAST:event_menu_cmds_cmdActionPerformed

    private void menu_cmds_pwdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_pwdActionPerformed
        Konsole.runCmd("pwd", MODE.NORMAL);
    }//GEN-LAST:event_menu_cmds_pwdActionPerformed

    private void menu_cmds_lsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_lsActionPerformed
        cmdBox.setText("ls ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_lsActionPerformed

    private void menu_cmds_cdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_cdActionPerformed
        cmdBox.setText("cd ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_cdActionPerformed

    private void menu_cmds_exitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_exitActionPerformed
        cmdBox.setText("exit ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_exitActionPerformed

    private void menu_cmds_helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_helpActionPerformed
        cmdBox.setText("help ");
        cmdBox.setCaretPosition(cmdBox.getText().length());
        cmdBox.requestFocus();
    }//GEN-LAST:event_menu_cmds_helpActionPerformed

    private void menu_cmds_common_ipconfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_cmds_common_ipconfigActionPerformed
        if (isUnix) {
            Konsole.runCmd("ifconfig -a", MODE.NORMAL);
        } else {
            Konsole.runCmd("ipconfig /all", MODE.NORMAL);
        }
    }//GEN-LAST:event_menu_cmds_common_ipconfigActionPerformed

    private void cmdBoxKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cmdBoxKeyTyped

    }//GEN-LAST:event_cmdBoxKeyTyped

    private void runCmd() {
        runCmd(cmdBox.getText(), false);
    }

    /**
     * This function allows for programmatically running a command, in the mode
     * specified, instead of in whatever mode the konsole is currently in.
     * @param cmdString
     * @param mode
     */
    public static void runCmd(final String cmdString, MODE mode) {
        //ensure references to konsole are valid
        get_console();
        final MODE oldMode = Konsole.mode;
        synchronized (cmdRunningLock) {
            konsole.setMode(mode);
            Konsole.get_console().runCmd(cmdString, true);
            konsole.setMode(oldMode);
        }
    }

    /**
     * This private function is not static, because we want to be able
     * to access the non-static swing elements from within this function
     * without having to qualify each one everytime.
     * @param cmdString The string to run
     * @param programmatic True if ANY command is run from any thread other than
     * the Konsole thread.
     */
    private void runCmd(final String cmdString, final boolean programmatic) {
        new Thread(new Runnable() {

            public void run() {
                //We only want one command to be running at a time, so if multiple threads
                //are trying to run commands, lock them out of running a command, or changing
                //the mode.
                synchronized (cmdRunningLock) {
                    if (mode == MODE.REMOTE) {
                        //We are sending all commands to the remote machine. back tick escaped commands
                        //are sent through, except "dc" is handled specially.
                        if(cmdString.toLowerCase().equals("dc")){
                            setMode(MODE.NORMAL);
                            Konsole.println("Remote connection closed");
                        }
                        
                        cmdBox.setText("");
                        return;
                    }

                    coutBuffer = "";

                    boolean addToHistory = true;
                    cmdRunning = true;
                    if (cmdString.equals("")) {
                        return;
                    }
                    ArrayList<String> args = parseArgs(cmdString);
                    ArrayList<String> params = new ArrayList<String>();
                    String cmd = args.get(0);
                    boolean tempNormalConsole = false;
                    if (cmd.matches("`.*?`")) {
                        tempNormalConsole = true;
                        Pattern p = Pattern.compile("`(.*?)`");
                        Matcher m = p.matcher(cmd);
                        if (m.find()) {
                            cmd = m.group(1);
                        }
                    }

                    //override
                    if (tempNormalConsole) {
                        setMode(MODE.NORMAL);
                    }

                    if (mode == MODE.NORMAL) {
                        //expand env variables
                        for (int i = 0; i < args.size(); i++) {
                            if (i == 0) {
                                continue;
                            }
                            Pattern p = Pattern.compile(".*\\{(.*?)\\}.*");
                            Matcher m = p.matcher(args.get(i));
                            while (m.find()) {
                                String var = m.group(1);
                                args.set(i, args.get(i).replace("{" + var + "}", env.getEnv(var).toString()));
                            }
                            params.add(args.get(i));
                        }
                        //since we are checking for params.get(argNum) != null in several
                        //places, we are going to insert 25 nulls to get around throwing
                        //an IndexOutOfBoundsException. This way, if you are expecting
                        //a value at index 0, if the argument is there, it will exist in 0,
                        //if not, it will be null. (Since the whitespace is trimmed, and only
                        //strings should be in the array, the first null in the array also
                        //means that there are no arguments after this either. 25 should
                        //be a good number, but if there are more than 25 possible arguments,
                        //this hack won't work.
                        for (int i = 0; i < 25; i++) {
                            params.add(null);
                        }
                    }
                    if (mode == MODE.NORMAL) {
                        Konsole.print(">>");
                    }
                    /*******************************************************************
                     * NORMAL CONSOLE MODE
                     *******************************************************************/
                    if (mode == MODE.NORMAL) {
                        cmd = cmd.toLowerCase();
                        /*******************************************************************
                         * cls, clear - clears the konsole screen
                         *******************************************************************/
                        if (cmd.equals("clear") || cmd.equals("cls")) {
                            Konsole.cls();

                            /*******************************************************************
                             * echo, print - writes to the konsole screen
                             *******************************************************************/
                        } else if (cmd.equals("echo") || cmd.equals("print")) {
                            String echo = "";
                            for (int i = 0; i < params.size(); i++) {
                                if (params.get(i) != null) {
                                    echo += params.get(i) + " ";
                                }
                            }
                            echo = echo.trim();
                            Konsole.println(echo);

                            /*******************************************************************
                             * err - writes to the konsole screen with printerr, instead of println
                             *******************************************************************/
                        } else if (cmd.equals("err")) {
                            String echo = "";
                            for (int i = 0; i < params.size(); i++) {
                                if (params.get(i) != null) {
                                    echo += params.get(i) + " ";
                                }
                            }
                            Konsole.printerr(echo);

                            /*******************************************************************
                             * info - prints useful information about this computer
                             *******************************************************************/
                        } else if (cmd.equals("info")) {
                            Konsole.println("Computer Information:");
                            try {
                                InetAddress addr = InetAddress.getLocalHost();
                                Konsole.println("\tLocal IP Address:  " + addr.getHostAddress());
                                Konsole.println("\tLocal Host Name:   " + addr.getHostName());
                            } catch (UnknownHostException e) {
                            }
                            try {
                                Konsole.println("\tGlobal IP Address: " + NetworkHandler.getWebPage("http://www.whatismyip.com/automation/n09230945.asp"));
                            } catch (NetworkHandlerException ex) {
                            }

                            /*******************************************************************
                             * google, search - searches for a given string on google
                             *******************************************************************/
                        } else if (cmd.equals("google") || cmd.equals("search")) {
                            String echo = "";
                            for (int i = 0; i < params.size(); i++) {
                                if (params.get(i) == null) {
                                    break;
                                }
                                echo += params.get(i) + " ";
                            }
                            echo = echo.trim();
                            Konsole.println("Searching google...");
                            utility.LaunchBrowser("http://www.google.com/#hl=en&q=" + echo);


                            /*******************************************************************
                             * > - Put the console in normal mode
                             *******************************************************************/
                        } else if (cmd.equals(">")) {
                            setMode(MODE.NORMAL);
                            addToHistory = false;
                            /*******************************************************************
                             * groovy - turns groovy mode on or off
                             *******************************************************************/
                        } else if (cmd.equals("groovy")) {
                            setMode(MODE.GROOVY);
                            addToHistory = false;
                            /*******************************************************************
                             * java - turns reflection mode on or off
                             *******************************************************************/
                        } else if (cmd.equals("java")) {
                            setMode(MODE.JAVA);
                            addToHistory = false;
                            /*******************************************************************
                             * remote - turns remote connection mode on or off
                             *******************************************************************/
                        } else if (cmd.equals("remote")) {
                            if (validateArgs(1, params)) {
                                remoteUser = params.get(0);
                                setMode(MODE.REMOTE);
                                addToHistory = false;
                            }
                            /***********************************************************
                             * genv - deletes the current env, and imports the env from
                             * groovy
                             ***********************************************************/
                        } else if (cmd.equals("genv")) {
                            env.clearEnv();
                            Map<String, Object> genv = groovyBinding.getVariables();
                            for (Map.Entry<String, Object> e : genv.entrySet()) {
                                env.addEnv(e.getKey(), new exObject(e.getValue()));
                            }
                            Konsole.println("Groovy Env exported");
                            /*******************************************************************
                             * gscript - runs a groovy script specified
                             *******************************************************************/
                        } else if (cmd.equals("gscript")) {
                            if (validateArgs(1, params)) {
                                groovyBinding = new Binding();
                                //bind all the env variables to this shell
                                Map<String, exObject> myEnv = env.getFullEnv();
                                for (Map.Entry<String, exObject> e : myEnv.entrySet()) {
                                    groovyBinding.setVariable(e.getKey(), e.getValue().toObject());
                                }
                                File filename = new File(params.get(0));
                                Konsole.println("Running groovy script at " + filename.getAbsolutePath());
                                try {
                                    String script = utility.file_get_contents(filename.getAbsolutePath());
                                    GroovyShell shell = new GroovyShell(groovyBinding);
                                    Object groovyReturn = shell.evaluate(script);
                                    if (groovyReturn != null) {
                                        Konsole.println(groovyReturn);
                                    }
                                } catch (Exception ex) {
                                    Konsole.println("That file was not found");
                                }
                            } else {
                                Konsole.println("usage: gscript filename");
                            }
                            /*******************************************************************
                             * ksh - runs an external Konsole script
                             *******************************************************************/
                        } else if (cmd.equals("ksh")) {
                            if (validateArgs(1, params)) {
                                Konsole.println("Running Konsole script at " + params.get(0));


                            } else {
                                Konsole.println("usage: ksh filename");
                            }
                            /*******************************************************************
                             * set - allows for the environmental variables to be manipulated
                             *******************************************************************/
                        } else if (cmd.equals("set")) {
                            if (validateArgs(0, params)) {
                                //echo the environment to the screen
                                env.printAllToKonsole();
                            } else if (validateArgs(1, params)) {
                                env.removeEnv(params.get(0));
                                Konsole.println(params.get(0) + " unset.");
                            } else if (validateArgs(2, params)) {
                                //set arg1 to arg2
                                env.addEnv(params.get(0), new exObject(params.get(1)));
                                Konsole.println(params.get(0) + "=" + env.getEnv(params.get(0)));
                            } else {
                                Konsole.println("Usage: set [key [value]]");
                            }
                            /*******************************************************************
                             * cmd, cmds - lists supported commands
                             *******************************************************************/
                        } else if (cmd.equals("cmd") || cmd.equals("cmds")) {
                            Konsole.println("Here is a list of all supported commands. For more help on a\n specific command,"
                                    + "type help <cmd>");
                            Konsole.println("cls\necho\ngoogle\ngenv\ngscript\nksh\nset\ncmd\npwd\nls\ncd\nexit\nhelp\ninfo");
                            /*******************************************************************
                             * pwd - Prints working directory
                             *******************************************************************/
                        } else if (cmd.equals("pwd")) {
                            Konsole.println(cwd.getAbsolutePath().substring(0, cwd.getAbsolutePath().length() - 1));
                            /*******************************************************************
                             * ls, dir - Lists all the files in the current working directory
                             *******************************************************************/
                        } else if (cmd.equals("ls") || cmd.equals("dir")) {
                            boolean list = false;
                            boolean all = false;
                            for (int i = 0; i < params.size(); i++) {
                                String param = params.get(i);
                                if (param == null) {
                                    break;
                                }
                                if (param.charAt(0) == '-') {
                                    for (int j = 1; j < param.length(); j++) {
                                        if (param.charAt(j) == 'l') {
                                            list = true;
                                        }
                                        if (param.charAt(j) == 'a') {
                                            all = true;
                                        }
                                    }
                                }
                            }
                            File[] files = cwd.listFiles();
                            String out = "";
                            if (files != null) {
                                for (int i = 0; i < files.length; i++) {
                                    File f = files[i];
                                    if (!all) {
                                        if (f.isHidden()) {
                                            continue;
                                        }
                                    }
                                    if (list) {
                                        Konsole.print("\n" + (f.canExecute() ? "X" : "-") + (f.canRead() ? "R" : "-") + (f.canWrite() ? "W" : "-") + " ");
                                        Konsole.print((!f.isDirectory() ? f.length() + "bytes" : "dir") + " ", Color.CYAN);
                                        Konsole.print(f.getName(), (f.isHidden() ? Color.RED : null));
                                    } else {
                                        out += f.getName() + "; ";
                                    }
                                }
                                Konsole.println(out.trim());
                            } else {
                                Konsole.println("This directory is empty");
                            }
                            /*******************************************************************
                             * cd - Changes the directory. If no arguments, works like a Windows style cd
                             *******************************************************************/
                        } else if (cmd.equals("cd")) {
                            if (params.get(0) == null) {
                                Konsole.println(cwd.getAbsolutePath());
                            } else {
                                //first, see if the path is an absolute path
                                File poss = new File(params.get(0));
                                if (poss.isAbsolute()) {
                                    try {
                                        cwd = poss.getCanonicalFile();
                                    } catch (IOException ex) {
                                    }
                                    Konsole.println(cwd.getAbsolutePath());
                                } else {
                                    poss = new File(cwd, params.get(0));
                                    if (poss.exists()) {
                                        if (poss.isDirectory()) {
                                            try {
                                                cwd = poss.getCanonicalFile();
                                            } catch (IOException ex) {
                                            }
                                            Konsole.println(cwd.getAbsolutePath());
                                        } else {
                                            Konsole.println("The specified path is not a directory");
                                        }
                                    } else {
                                        Konsole.println("No such directory");
                                    }
                                }
                            }
                            /*******************************************************************
                             * exit, close, quit - Closes the Konsole
                             *******************************************************************/
                        } else if (cmd.equals("exit") || cmd.equals("quit") || cmd.equals("close")) {
                            if (params.get(0) != null && params.get(0).equals("-st")) {
                                //close the whole program
                                frame.quit(true);
                            }
                            Konsole.close();
                            /*******************************************************************
                             * help, *? - displays a help message for the specified command
                             *******************************************************************/
                        } else if (cmd.equals("help") || cmd.matches(".*\\?")) {
                            if (validateArgs(0, params)) {
                                params.add(cmd.replaceAll("\\?", ""));
                            }
                            if (validateArgs(1, params)) {
                                //Please add a blurb about your command here when you create the command
                                String helpcmd = params.get(0);
                                if (helpcmd.equals("clear") || helpcmd.equals("cls")) {
                                    Konsole.println("Clears the console's screen");
                                } else if (helpcmd.equals("echo") || helpcmd.equals("print")) {
                                    Konsole.println("Echoes text to the console");
                                } else if (helpcmd.equals("help")) {
                                    Konsole.println("Shows a help message for the specified command");
                                } else if (helpcmd.equals("set")) {
                                    Konsole.println("Shows all the environmental variables for this Konsole.\n"
                                            + "Sets 'key' to 'value', or deletes 'key' from the map if no 'value' specified.\n"
                                            + "usage: set [key [value]]");
                                } else if (helpcmd.equals("java")) {
                                    Konsole.println("Turns reflection mode on in the konsole. This allows arbitrary functions to "
                                            + "be run, which allows for easier debugging, and also adds most of the existing functionality"
                                            + " to the Konsole that already exists in the codebase.");
                                } else if (helpcmd.equals("groovy")) {
                                    Konsole.println("Turns groovy mode on in the konsole. This allows for groovy code to be run"
                                            + " in the console. groovy -noenv prevents the environment from being transferred.");
                                } else if (helpcmd.equals("genv")) {
                                    Konsole.println("Deletes the current env, and imports the groovy env.");
                                } else if (helpcmd.equals("gscript")) {
                                    Konsole.println("Runs the specified groovy script. It sets up a new groovy environment.");
                                } else if (helpcmd.equals("ksh")) {
                                    Konsole.println("Runs the specified Konsole script. Currently not implemented.");
                                } else if (helpcmd.equals("class")) {
                                    Konsole.println("Prints information about all the functions in this class. The classname "
                                            + "must be fully qualified. Currently not implemented.");
                                } else if (helpcmd.equals("package")) {
                                    Konsole.println("Prints information about all the classes inside the given package. Currently not "
                                            + "implemented.");
                                } else if (helpcmd.equals("pwd")) {
                                    Konsole.println("Prints the current working directory");
                                } else if (helpcmd.equals("cd")) {
                                    Konsole.println("Changes the working directory to the specified directory");
                                } else if (helpcmd.equals("ls") || helpcmd.equals("dir")) {
                                    Konsole.println("Lists the files in the current working directory.\n"
                                            + "\t-l Lists more information about the files\n"
                                            + "\t-a Shows all the files in the directory, including . and .. and"
                                            + "other hidden files.");
                                } else if (helpcmd.equals("info")) {
                                    Konsole.println("Shows useful information about this computer");
                                } else {
                                    Konsole.println("That command is not one of the builtin commands.");
                                }
                            } else {
                                Konsole.println("usage: help <cmd>");
                            }

                            /*******************************************************************
                             * Run an arbitrary command
                             *******************************************************************/
                        } else {
                            try {
                                cmdBox.setText("");
                                exProcess p = new exProcess(parseArgs(cmdString), null, new exRunnable() {

                                    public void run(exObject line) {
                                        coutBuffer += line.getString() + "\n";
                                        Konsole.println(line.getString());
                                    }
                                }, null);
                                Konsole.get_console().setTitle("Konsole - " + cmd);
                                p.directory(cwd);
                                p.start();
                                currentProcess = p;
                                try {
                                    env.addEnv("ret", new exObject(p.waitFor()));
                                } catch (InterruptedException ex) {
                                } finally {
                                    currentProcess = null;
                                }
                            } catch (IOException e) {
                                Konsole.println("The specified command \"" + cmd + "\" was not found");
                            } finally {
                                Konsole.get_console().setTitle("Konsole");
                            }
                        }
                        /*******************************************************************
                         * GROOVY MODE
                         *******************************************************************/
                    } else if (mode == MODE.GROOVY) {

                        GroovyShell shell = new GroovyShell(groovyBinding);

                        //run the code. Take the raw input from the Konsole shell, instead of
                        //the parsed stuff, it'll be more accurate.
                        Konsole.print("\n");
                        try {
                            Object groovyReturn = shell.evaluate(cmdString);
                            if (groovyReturn != null) {
                                Konsole.println(groovyReturn);
                            }
                        } catch (Exception e) {
                            Konsole.printerr(e.getMessage());
                        }
                        Konsole.print("\n");

                        /*******************************************************************
                         * REFLECTION MODE
                         *******************************************************************/
                    } else if (mode == MODE.JAVA) {
                        try {
                            //import all the modules and the core by default
                            String pre_eval = "import SparkTabCore.*;\n";
                            for (int i = 0; i < frame.getMod_Set().size(); i++) {
                                pre_eval += "import " + frame.getMod_Set().get(i).getModuleName() + ".*;\n";
                            }
                            System.out.println(pre_eval);
                            ScriptEvaluator se = new ScriptEvaluator(pre_eval + cmdString);
                            se.evaluate(new Object[]{});
                        } catch (InvocationTargetException ex) {
                            Logger.getLogger(Konsole.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (CompileException ex) {
                            Konsole.printerr(ex.getMessage());
                        }
                    }
                    if (!programmatic && addToHistory) {
                        cmdHistory.add(0, new Kommand(cmdString, mode));
                        setHistory(cmdHistory);
                        historyIndex = 0;
                    }
                    cmdBox.setText("");
                    cmdRunning = false;
                } //synchronized
            }
        }).start();
    }

    private void setMode(final MODE m) {
        if (EventQueue.isDispatchThread()) {
            synchronized (cmdRunningLock) {
                switch (m) {
                    case GROOVY:
                        Konsole.cls();
                        commandType.setText(MODE.GROOVY.getCmdText());
                        menu_mode_groovy.setSelected(true);
                        mode = MODE.GROOVY;
                        groovyBinding = new Binding();
                        //bind all the env variables to this shell
                        Map<String, exObject> myEnv = env.getFullEnv();
                        for (Map.Entry<String, exObject> e : myEnv.entrySet()) {
                            groovyBinding.setVariable(e.getKey(), e.getValue());
                        }
                        break;
                    case JAVA:
                        Konsole.cls();
                        commandType.setText(MODE.JAVA.getCmdText());
                        menu_mode_java.setSelected(true);
                        mode = MODE.JAVA;
                        break;
                    case REMOTE:
                        Konsole.cls();
                        Konsole.println("Attempting to connect to user: " + remoteUser);
                        commandType.setText("<connecting>");
                        mode = MODE.REMOTE;
                        ServerFeed.registerFeedListener(new ServerFeedListener() {

                            public API_Command serverFeed() {
                                return new API_Command(0, "remote_connect", new String[]{remoteUser}, null);
                            }

                            public void serverReturn(API_Return ret) {
                                ServerFeed.removeFeedListener(this);
                            }
                        });
                        break;
                    default:
                        commandType.setText(MODE.NORMAL.getCmdText());
                        menu_mode_normal.setSelected(true);
                        mode = MODE.NORMAL;
                        break;
                }
            }
        } else {
            EventQueue.invokeLater(new Runnable() {

                public void run() {
                    setMode(m);
                }
            });
        }
    }

    private void setHistory(final ArrayList<Kommand> cmds) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                menu_history.removeAll();
                for (int i = 0; i < cmds.size() && i < 10; i++) {
                    final Kommand cmd = cmds.get(i);
                    JMenuItem menu = new JMenuItem();
                    String cmdAbbr = cmd.cmd;
                    if (cmd.cmd.length() > 30) {
                        cmdAbbr = cmd.cmd.substring(0, 28) + "...";
                    }
                    menu.setText(cmd.kmode.getCmdText() + cmdAbbr);
                    menu.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            cmd.invoke();
                        }
                    });
                    menu_history.add(menu);
                }
            }
        });
    }

    private boolean validateArgs(int number, ArrayList<String> params) {
        for (int i = 0; i < number; i++) {
            try {
                if (params.get(i) == null) {
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        }
        return true;
    }

    public Object get_env(String key) {
        return env.getEnv(key);
    }

    private ArrayList<String> parseArgs(String cmd) {
        //raw regex: [^\s"]+|"(?:[^"\\]|\\.)*"
        Matcher m = Pattern.compile("[^\\s\"]+|\"(?:[^\"\\\\]|\\\\.)*\"").matcher(cmd);
        ArrayList<String> r = new ArrayList<String>();
        while (m.find()) {
            StringBuilder arg = new StringBuilder(m.group(0));
            //if this is a quoted string, take out the left and right quotes and
            //unescape backslash escaped quotes.
            //raw regex: /"(?:[^"\\]|\\.)*"/
            if (arg.toString().matches("\"(?:[^\"\\\\]|\\\\.)*\"")) {
                arg.delete(0, 1);
                arg.delete(arg.length() - 1, arg.length());
                r.add(arg.toString().replace("\\\"", "\""));
            } else {
                r.add(arg.toString());
            }
        }
        return r;
    }

    private class Kommand {

        private String cmd;
        private MODE kmode;

        public Kommand(String cmd, MODE mode) {
            this.cmd = cmd;
            this.kmode = mode;
        }

        @Override
        public String toString() {
            return this.cmd;
        }

        public void invoke() {
            Konsole.get_console();
            synchronized (cmdRunningLock) {
                MODE oldMode = mode;
                setMode(kmode);
                konsole.runCmd(cmd, false);
                setMode(oldMode);
            }
        }
    }

    private String stripLast(String s) {
        return s.substring(0, s.length() - 1);
    }

    public class KonsoleEnv {

        Map<String, exObject> env;

        public KonsoleEnv() {
            env = new HashMap<String, exObject>();
        }

        public void addEnv(String key, exObject value) {
            env.put(key, value);
        }

        public exObject getEnv(String key) {
            if (env.get(key) == null) {
                return null;
            }
            return new exObject(env.get(key));
        }

        public void removeEnv(String key) {
            env.remove(key);
        }

        public void printAllToKonsole() {
            for (Map.Entry<String, exObject> e : env.entrySet()) {
                if (e.getValue().isStringType()) {
                    Konsole.println(e.getKey() + "=" + e.getValue());
                } else if (e.getValue().isPrimitive()) {
                    Konsole.println(e.getKey() + "=("
                            + e.getValue().getClassification().name().toLowerCase() + ")" + e.getValue().toString());
                } else {
                    Konsole.println(e.getKey() + "->" + e.getValue().toString() + "\n\t(" + e.getValue().getClass().getCanonicalName() + ")");
                }
            }
        }

        public Map<String, exObject> getFullEnv() {
            return env;
        }

        public void clearEnv() {
            env.clear();
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea cmdBox;
    private javax.swing.JLabel commandType;
    private javax.swing.JPanel inputPanel;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JMenuItem menu_cmds_cd;
    private javax.swing.JMenuItem menu_cmds_cls;
    private javax.swing.JMenuItem menu_cmds_cmd;
    private javax.swing.JMenuItem menu_cmds_common_ipconfig;
    private javax.swing.JMenuItem menu_cmds_echo;
    private javax.swing.JMenuItem menu_cmds_exit;
    private javax.swing.JMenuItem menu_cmds_genv;
    private javax.swing.JMenuItem menu_cmds_google;
    private javax.swing.JMenuItem menu_cmds_gscript;
    private javax.swing.JMenuItem menu_cmds_help;
    private javax.swing.JMenuItem menu_cmds_ksh;
    private javax.swing.JMenuItem menu_cmds_ls;
    private javax.swing.JMenuItem menu_cmds_pwd;
    private javax.swing.JMenuItem menu_cmds_set;
    private javax.swing.JMenuItem menu_help_help;
    private javax.swing.JMenu menu_history;
    private javax.swing.JRadioButtonMenuItem menu_mode_groovy;
    private javax.swing.JRadioButtonMenuItem menu_mode_java;
    private javax.swing.JRadioButtonMenuItem menu_mode_normal;
    private javax.swing.ButtonGroup menu_modes;
    javax.swing.JTextPane output;
    // End of variables declaration//GEN-END:variables
}
