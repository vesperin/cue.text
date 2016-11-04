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
    outStream().println("INFO: " + s);
  }

  /**
   * Logs an error caused by some throwable.
   *
   * @param s the error label
   * @param throwable the actual error
   */
  default void error(Object s, Throwable throwable) {
    outStream().println("ERROR: " + s);
    throwable.printStackTrace(System.out);
  }

  /**
   * Logs a warning message
   *
   * @param s the warning message
   */
  default void warn(Object s){
    outStream().println("WARN: " + s);
  }

  /**
   * @return the controlling printing stream.
   */
  PrintStream outStream();
}
