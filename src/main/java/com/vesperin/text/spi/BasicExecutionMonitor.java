package com.vesperin.text.spi;

import java.io.PrintStream;

/**
 * @author Huascar Sanchez
 */
public class BasicExecutionMonitor implements ExecutionMonitor {
  private PrintStream out;
  private boolean     active;

  /**
   * Constructs a new BasicExecutionMonitor object.
   */
  private BasicExecutionMonitor(){
    this(System.out);
  }

  /**
   * Constructs a new BasicExecutionMonitor object.
   * @param outStream print stream
   */
  private BasicExecutionMonitor(PrintStream outStream){
    this.out    = outStream;
    this.active = true;
  }

  /**
   * @return Sole instance of ExecutionMonitor
   */
  public static ExecutionMonitor get(){
    return Installer.LAZY_INSTANCE;
  }

  @Override public void enable() {
    active = true;
  }

  @Override public void disable() {
    if(active){
      active = false;
    }
  }

  @Override public PrintStream outStream() {
    return out;
  }

  @Override public String toString() {
    return "BasicExecutionMonitor(active=" + active + ")";
  }

  private static class Installer {
    static final ExecutionMonitor LAZY_INSTANCE = new BasicExecutionMonitor();
  }
}
