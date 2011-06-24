
package com.laytonsmith.exObjects;

/**
 * This class extends Thread to make it so that the run function takes a parameter.
 * This parameter is an exObject, which means that any value may be passed to the
 * Runnable. Note that the Runnable is actually an exRunnable, however, for the sake
 * of compatibility, there is are constructors that support a typical Runnable object.
 * @author Layton Smith
 */
public class exThread extends Thread{

    Thread w;

    /**
     * This constructor creates a new exThread with null being sent as the
     * parameter to the exRunnable
     * @param r The exRunnable to run in this Thread
     */
    public exThread(final exRunnable r){
        this(r, null);
    }

    /**
     * This constructor creates a new exThread from a Runnable. Obviously
     * no parameters can be passed to the Runnable object.
     * @param r
     */
    public exThread(final Runnable r){
        w = new Thread(r);
    }

    /**
     * This constructor creates a new exThread with a name from a Runnable.
     * Obviously no parameters can be passed to the Runnable object.
     * @param r
     * @param name
     */
    public exThread(final Runnable r, String name){
        w = new Thread(r, name);
    }

    /**
     * This constructor creates a new exThread with an exRunnable,
     * and the exObject e is passed to it when the thread is started.
     * @param r
     * @param e
     */
    public exThread(final exRunnable r, final exObject e){
        w = new Thread(new Runnable(){
            public void run(){
                r.run(e);
            }
        });
    }

    /**
     * This constructor creates a new named exThread with an exRunnable,
     * and the exObject e is passed to it when the thread is started.
     * @param r
     * @param e
     * @param name
     */
    public exThread(final exRunnable r, final exObject e, final String name){
        w = new Thread(new Runnable(){
            public void run(){
                r.run(e);
            }
        }, name);
    }

    @Override
    public synchronized void start() {
        w.start();
    }

    @Override
    @Deprecated
    public int countStackFrames() {
        return w.countStackFrames();
    }

    @Override
    @Deprecated
    public void destroy() {
        w.destroy();
    }

    @Override
    public ClassLoader getContextClassLoader() {
        return w.getContextClassLoader();
    }

    @Override
    public long getId() {
        return w.getId();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return w.getStackTrace();
    }

    @Override
    public State getState() {
        return w.getState();
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return w.getUncaughtExceptionHandler();
    }

    @Override
    public void interrupt() {
        w.interrupt();
    }

    @Override
    public boolean isInterrupted() {
        return w.isInterrupted();
    }

    @Override
    /**
     * Unlike Thread, run does the exact same thing as start.
     */
    public void run() {
        w.start();
    }

    @Override
    public void setContextClassLoader(ClassLoader cl) {
        w.setContextClassLoader(cl);
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        w.setUncaughtExceptionHandler(eh);
    }

    @Override
    public String toString() {
        return w.toString();
    }

}
