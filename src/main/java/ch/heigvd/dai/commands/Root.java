package ch.heigvd.dai.commands;

import picocli.CommandLine;

@CommandLine.Command(
    name = "notion",
    description = "No-Tion application to manage notes via client/server.",
    version = "1.0.0",
    subcommands = {Server.class, Client.class},
    scope = CommandLine.ScopeType.INHERIT,
    mixinStandardHelpOptions = true)
public class Root implements Runnable {
  @Override
  public void run() {
    // Display help if no subcommand is specified
    CommandLine.usage(this, System.out);
  }
}
