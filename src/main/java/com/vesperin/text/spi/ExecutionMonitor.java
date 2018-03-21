package com.vesperin.text.spi;

import java.io.PrintStream;

/**
 * @author Huascar Sanchez
 */
public interface ExecutionMonitor {
  /**
   * Enables the monitor.
   */
  void enable();

  /**
   * Disables the monitor
   */
  void disable();

  /**
   * Logs some important information
   *
   * @param s the important information
   */
  default void info(Object s){
    if(isActive()) outStream().println("INFO: " + s);
  }

  /**
   * @return true if the monitor is active.
   */
  boolean isActive();

  /**
   * Logs an error caused by some throwable.
   *
   * @param s the error label
   * @param throwable the actual error
   */
  default void error(Object s, Throwable throwable) {
    if(isActive()){
      outStream().println("ERROR: " + s);
      throwable.printStackTrace(System.out);
    }
  }

  /**
   * Logs a warning message
   *
   * @param s the warning message
   */
  default void warn(Object s){
    if(isActive()) outStream().println("WARN: " + s);
  }

  /**
   * @return the controlling printing stream.
   */
  PrintStream outStream();
}
