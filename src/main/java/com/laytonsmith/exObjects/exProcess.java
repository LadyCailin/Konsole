package com.laytonsmith.exObjects;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class exProcess extends Process {

    private Thread outWatcher;
    private Thread errWatcher;
    private Thread dieWatcher;
    private exRunnable _finished;
    private ProcessBuilder processb;
    private Process p;
    private boolean running = false;
    private int exitcode = Integer.MAX_VALUE;
    private boolean hasExited = false;

    public exProcess(ArrayList<String> cmd) {
        this(cmd, null, null, null);
    }

    public exProcess(ArrayList<String> cmd, final exRunnable finished, final exRunnable outLine,
            final exRunnable dieLine) {
        processb = new ProcessBuilder(cmd.toArray(new String[]{}));
        this._finished = finished;
        outWatcher = new Thread(new Runnable() {

            public void run() {
                try {
                    InputStream in = p.getInputStream();
                    int c;
                    String line = "";
                    while ((c = in.read()) != -1) {
                        if (((char) c) == '\n') {
                            System.out.println(line);
                            final String fLine = line;
                            new Thread(new Runnable() {

                                public void run() {
                                    if (outLine != null) {
                                        outLine.run(new exObject(fLine));
                                    }
                                }
                            }).start();
                            line = "";
                        } else {
                            line += Character.toString((char) c);
                        }
                    }
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(exProcess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        errWatcher = new Thread(new Runnable() {

            public void run() {
                try {
                    InputStream in = p.getErrorStream();
                    int c;
                    String line = "";
                    while ((c = in.read()) != -1) {
                        if (((char) c) == '\n') {
                            System.err.println(line);
                            final String fLine = line;
                            new Thread(new Runnable() {

                                public void run() {
                                    if (outLine != null) {
                                        outLine.run(new exObject(fLine));
                                    }
                                }
                            }).start();
                            line = "";
                        } else {
                            line += Character.toString((char) c);
                        }
                    }
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(exProcess.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        dieWatcher = new Thread(new Runnable() {

            public void run() {
                int failed = 10;
                while (failed > 0 && !hasExited) {
                    try {
                        exitcode = p.waitFor();
                        hasExited = true;
                        running = false;
                        if (_finished != null) {
                            new exThread(_finished, new exObject(exitcode), "Konsole_Process_Callback").start();
                        }
                    } catch (InterruptedException ex) {
                        failed--;
                    }
                }
            }
        });
    }

    public ProcessBuilder directory(File directory) {
        return processb.directory(directory);
    }

    public File directory() {
        return processb.directory();
    }

    public Map<String, String> environment() {
        return processb.environment();
    }

    public int exitValue() {
        if (p == null) {
            throw new IllegalThreadStateException("The process has not been started yet");
        }
        return p.exitValue();
    }

    public int waitFor() throws InterruptedException {
        if (p == null) {
            throw new IllegalThreadStateException("The process has not been started yet");
        }
        return p.waitFor();
    }

    public void in(String input) {
        try {
            if (p == null) {
                throw new IllegalThreadStateException("The process has not been started yet");
            }
            OutputStream o = p.getOutputStream();
            o.write(input.getBytes());
        } catch (IOException ex) {
            System.err.println("Could not write input to process");
        }
    }

    public void start() throws IOException {
        if (!running) {
            p = processb.start();
            new Thread(outWatcher, "Konsole_outwatcher").start();
            new Thread(errWatcher, "Konsole_errwatcher").start();
            new Thread(dieWatcher, "Konsole_diewatcher").start();
            running = true;
        }
    }

    @Override
    public OutputStream getOutputStream() {
        if (p != null) {
            return p.getOutputStream();
        } else {
            throw new IllegalThreadStateException("The process has not been started yet");
        }
    }

    @Override
    public InputStream getInputStream() {
        if (p != null) {
            return p.getInputStream();
        } else {
            throw new IllegalThreadStateException("The process has not been started yet");
        }
    }

    @Override
    public InputStream getErrorStream() {
        if (p != null) {
            return p.getErrorStream();
        } else {
            throw new IllegalThreadStateException("The process has not been started yet");
        }
    }

    @Override
    public void destroy() {
        if(p != null){
            p.destroy();
        } else{
            throw new IllegalThreadStateException("The process has not been started yet");
        }
    }
}
