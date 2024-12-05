package ch.heigvd.dai.commands;

import picocli.CommandLine;

@CommandLine.Command(
    name = "notion",
    description = "Application No-Tion pour gérer des notes via un client/serveur.",
    version = "1.0.0",
    subcommands = {Server.class, Client.class},
    scope = CommandLine.ScopeType.INHERIT,
    mixinStandardHelpOptions = true)
public class Root implements Runnable {
  @Override
  public void run() {
    // Afficher l'aide si aucune sous-commande n'est spécifiée
    CommandLine.usage(this, System.out);
  }
}
