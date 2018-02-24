package com.vesperin.text;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vesperin.text.Emitter.WordPack;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * To run the Introspector's CLI from the command line, run
 * <code>java com.vesperin.text.Cli --from-path &lt;path/to/src-files&gt;
 * --to-path-json &lt;path/to/output.json&gt; -v</code>.
 *
 * @author Huascar Sanchez
 */
public class Cli {

  /**
   * Run Introspector's CLI operation mentioned in the <code>args</code>.
   * If all operations run successfully, exit with a status of 0. Otherwise exit
   * with a status of 1.
   *
   * @param args Introspector's options
   */
  public static void main(String... args) {
    try {
      final boolean wasSuccessful = new Cli().runMain(args);
      System.exit(wasSuccessful ? 0 : 1);
    } catch (Exception e){
      System.exit(1);
    }
  }

  /**
   * Run the main operations.
   *
   * @param args from main()
   * @return either a successful or fail operation.
   */
  private boolean runMain(String... args) {

    final CommandLineParseResult result = CommandLineParseResult.parseArgs(args);

    try {
      result.throwParsingErrors();
    } catch (Exception ignored){
      result.printUsage();
      return false;
    }

    return run(result.createRequest());
  }

  private boolean run(Request request){
    try {
      request.process();
      return true;
    } catch (Exception e){
      request.error("Unable to process " + request, e);
      return false;
    }
  }

  /**
   * A class of methods that are useful when reading arguments
   * from the command line
   */
  static class CommandLineParseResult {

    private static final Set<String> NON_BOOLEAN_OPTIONS;
    private static final Set<String> BOOLEAN_OPTIONS;

    static {
      BOOLEAN_OPTIONS = ImmutableSet.copyOf(Arrays.asList(
              "-v", "--verbose"
      ));

      NON_BOOLEAN_OPTIONS = ImmutableSet.copyOf(Arrays.asList(
              "-f", "--from-path", "-t", "--to-path-json"
      ));
    }

    // declare private class level variables
    private final Map<String,String> arguments;
    private final List<String>       leftovers;
    private final List<Throwable>    parsingErrors;

    CommandLineParseResult(Iterator<String> args){
      this.arguments      = new HashMap<>();
      this.leftovers      = new ArrayList<>();
      this.parsingErrors  = new ArrayList<>();


      // Scan 'args'.
      while (args.hasNext()) {
        final String arg = args.next();
        if (arg.equals("--")) {

          // "--" marks the end of options and the beginning of positional arguments.
          break;
        } else if (arg.startsWith("--")) {
          // A long option.
          parseLongOption(arg, args);
        } else if (arg.startsWith("-")) {
          // A short option.
          parseGroupedShortOptions(arg, args);
        } else {
          // The first non-option marks the end of options.
          leftovers.add(arg);
          break;
        }
      }

      // Package up the leftovers.
      while (args.hasNext()) {
        leftovers.add(args.next());
      }

      // there must be NO left overs...
      Preconditions.checkArgument((leftovers.isEmpty()));
    }

    /**
     *
     * Creates a new Mints request.
     *
     * @return Either a Data request or a patterns discovery request.
     */
    Request createRequest(){

      final boolean log = verboseLogging();
      if(containsCorpusPath()){
        final Optional<String> fromPath = Optional.ofNullable(getCorpusPath());
        Preconditions.checkArgument(fromPath.isPresent());

        final Path from = Paths.get(fromPath.get());

        // fail fast
        Preconditions.checkArgument(Files.exists(from));
        if(!containsJsonPath()){

          return new PrintJSON(from, log);

        } else {

          Preconditions.checkArgument(containsJsonPath());

          final Optional<String> toPath = Optional.ofNullable(getJsonPath());
          Preconditions.checkArgument(toPath.isPresent());
          final Path to = Paths.get(toPath.get());

          final List<Path> paths = Arrays.asList(from, to);

          return new CreateJSON(paths, log);
        }

      } else {
        throw new NoSuchElementException("Unable to understand user's input");
      }
    }

    /**
     * Parses the command-line arguments 'args'.
     *
     * @return a list of the positional arguments
     *    that were left over after processing all
     *    options.
     */
    static CommandLineParseResult parseArgs(String... args) {
      final List<String> argList = Arrays.asList(args);
      final CommandLineParseResult result = new CommandLineParseResult(argList.iterator());

      if(argList.isEmpty()) {
        result.parsingErrors.add(new IllegalArgumentException("Missing Cli [options]"));
      }

      return result;
    }


    // Returns the next element of 'args' if there is one.
    private String grabNextValue(Iterator<String> args, String name) {
      if (!args.hasNext()) {
        throw new RuntimeException(String.format("no value found for %s", name));
      }

      return args.next();
    }


    private void parseLongOption(String arg, Iterator<String> args) {
      String name = arg.replaceFirst("^--no-", "--");
      String value = null;

      // Support "--name=value" as well as "--name value".
      final int equalsIndex = name.indexOf('=');
      if (equalsIndex != -1) {
        value = name.substring(equalsIndex + 1);
        name  = name.substring(0, equalsIndex);
      }

      if (value == null) {

        if(BOOLEAN_OPTIONS.contains(name)){
          value = arg.startsWith("--no-") ? "false" : "true";
        } else {
          value = grabNextValue(args, name);
        }

      }

      if(arguments.containsKey(name)){
        parsingErrors.add(new IllegalStateException("Option already entered."));
      } else {
        arguments.put(name, value);
      }

    }


    private void parseGroupedShortOptions(String arg, Iterator<String> args) {
      final int len = arg.length();

      for (int i = 1; i < len; ++i) {
        final String name = "-" + arg.charAt(i);

        // We need a value. If there's anything left, we take the rest
        // of this "short option".

        String value;

        if(BOOLEAN_OPTIONS.contains(name)){
          value = "true";
        } else {
          if (i + 1 < arg.length()) { // similar to args.hasNext()
            value = arg.substring(i + 1);
            i = arg.length() - 1;
          } else {
            value = grabNextValue(args, name);
          }
        }

        arguments.put(name, value);
      }
    }


    /**
     * check to see if a key exists in the list arguments
     *
     * @param  key   the key to lookup
     * @return value the value of the arguments
     */
    boolean containsKey(String key) {

      // check to ensure the key is valid
      if(isValid(key) && (BOOLEAN_OPTIONS.contains(key)
              || NON_BOOLEAN_OPTIONS.contains(key))) {

        return arguments.get(key) != null;
      }

      // invalid key so return null
      return false;

    }


    private boolean containsCorpusPath(){
      return ((containsKey("--from-path")
              || containsKey("-f")));
    }

    private String getCorpusPath(){
      return getValue("-f", "--from-path");
    }

    private boolean containsJsonPath(){
      return ((containsKey("--to-path-json")
              || containsKey("-t")));
    }

    private String getJsonPath(){
      return getValue("-t", "--to-path-json");
    }

    /**
     * get the value of the first key found in the list of arguments
     *
     * @param  key   the key to lookup
     * @param orElse the alternative key
     * @return value the value of the arguments
     */
    String getValue(String key, String... orElse) {

      // check to ensure the key is valid
      if(containsKey(key)) {
        // return the key if found or null if it isn't
        return arguments.get(key);
      } else {
        final List<String> alts = Arrays.asList(orElse);
        if(!alts.isEmpty() && !alts.contains(null)){
          final String keyAlternative = alts.get(0);
          if(containsKey(keyAlternative)){
            return arguments.get(keyAlternative);
          }
        }
      }

      throw new NoSuchElementException(String.format("unable to find %s", key));
    }

    private static boolean isValid(String value) {

      // check on the parameter value
      if(value == null) {
        return false;
      } else {
        value = value.trim();
        return !"".equals(value);
      }

      // passed validation
    }


    void printUsage(){
      reset();

      System.out.println("Usage: Cli [options]... ");
      System.out.println();
      System.out.println("OPTIONS");
      System.out.println();
      System.out.println("  (-f|--from-path) <dir>: A directory of source files to process. Example: ");
      System.out.println("      --from-path path/to/bigclonebench/");
      System.out.println();
      System.out.println("  (-t|--to-path-json)  <file>: JSON file to generate. Examples: ");
      System.out.println("      --to-path-json path/to/Foo.json");
      System.out.println();
      System.out.println("      Warn: if -t | --to-path-json is not used, then print results to screen.");
      System.out.println();
      System.out.println("  --verbose: turn on verbose output.");
    }

    private void reset(){
      arguments.clear();
      leftovers.clear();
      parsingErrors.clear();
    }

    void throwParsingErrors() {
      if(!parsingErrors.isEmpty()){
        throw new ParsingException(parsingErrors);
      }
    }

    private boolean verboseLogging(){
      final boolean keyExist = (containsKey("--verbose") || containsKey("-v"));

      if(keyExist){
        return Boolean.valueOf(getValue("-v", "--verbose"));
      } else {
        return false;
      }
    }

  }

  interface Request {
    void error(String message, Throwable cause);
    void process() throws Exception;
  }

  static class PrintJSON implements Request {
    private final Path directory;
    private final boolean log;

    PrintJSON(Path directory, boolean log){
      this.directory = directory;
      this.log = log;
    }

    @Override public void error(String message, Throwable cause) {
      if (log) System.out.println("ERROR: " + message + ". See: " + cause.getMessage());
    }

    @SuppressWarnings("RedundantThrows") @Override public void process() throws Exception {
      try {
        final List<WordPack> packs = Emitter.emitWordPacks(directory);
        final BagOfWordPacks model = new BagOfWordPacks();
        model.wordPacks.addAll(packs);
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(model));
      } catch (Exception e){
        error("Unable to process request", e);
        throw e;
      }
    }
  }

  static class CreateJSON implements Request {
    private final List<Path> directories;
    private final boolean log;

    CreateJSON(List<Path> directories, boolean log){
      Preconditions.checkArgument(directories != null
              && !directories.isEmpty() && directories.size() == 2);

      this.directories  = directories;
      this.log          = log;
    }

    @Override public void error(String message, Throwable cause) {
      if (log) System.out.println("ERROR: " + message + ". See: " + cause.getMessage());
    }

    @Override public void process() throws Exception {
      try {

        final Path from = directories.get(0);
        final Path to   = directories.get(1);
        Files.deleteIfExists(to);

        final List<WordPack> packs = Emitter.emitWordPacks(from);
        final BagOfWordPacks model = new BagOfWordPacks();
        model.wordPacks.addAll(packs);

        try (Writer writer = new FileWriter(to.toFile())) {
          final Gson gson = new GsonBuilder().setPrettyPrinting().create();
          gson.toJson(model, writer);
        }

      } catch (IOException cause){
        error("Unable to process request", cause);
        throw cause;
      }
    }
  }

  static class BagOfWordPacks {
    List<WordPack> wordPacks = new ArrayList<>();
    BagOfWordPacks(){}
  }


  static class ParsingException extends RuntimeException {

    static final long serialVersionUID = 1L;

    ParsingException(Collection<Throwable> throwables){
      super(createErrorMessage(throwables));

    }

    private static String createErrorMessage(Collection<Throwable> errorMessages) {

      final List<Throwable> encounteredErrors = new ArrayList<>(errorMessages);
      if(!encounteredErrors.isEmpty()){
        encounteredErrors.sort(new ThrowableComparator());
      }

      final java.util.Formatter messageFormatter = new java.util.Formatter();
      messageFormatter.format("Parsing errors:%n%n");
      int index = 1;

      for (Throwable errorMessage : encounteredErrors) {
        final String    message = errorMessage.getLocalizedMessage();
        final String    line    = "line " + message.substring(
                message.lastIndexOf("line") + 5, message.lastIndexOf("line") + 6
        );

        messageFormatter.format("%s) Error at %s:%n", index++, line)
                .format(" %s%n%n", message);
      }

      return messageFormatter.format("%s error[s]", encounteredErrors.size()).toString();
    }
  }


  static class ThrowableComparator implements Comparator<Throwable> {
    @Override public int compare(Throwable a, Throwable b) {
      return a.getMessage().compareTo(b.getMessage());
    }
  }
}
