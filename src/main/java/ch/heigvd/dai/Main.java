package ch.heigvd.dai;

import ch.heigvd.dai.commands.Root;
import picocli.CommandLine;

public class Main {
  public static void main(String[] args) {
    // Define the command name based on the jar file name
    String jarFilename =
        new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
            .getName();
    // Create an instance of the root command
    Root root = new Root();
    // Execute the command with the provided arguments
    int exitCode =
        new CommandLine(root)
            .setCommandName(jarFilename)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);
    // Terminate the program with the exit code
    System.exit(exitCode);
  }
}
