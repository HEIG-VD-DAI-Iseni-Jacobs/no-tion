package ch.heigvd.dai;

import ch.heigvd.dai.commands.Root;
import picocli.CommandLine;

public class Main {

  public static void main(String[] args) {
    // Définir le nom de la commande (nom du jar)
    String jarFilename =
        new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
            .getName();

    // Créer la commande racine
    Root root = new Root();

    // Exécuter la commande
    int exitCode =
        new CommandLine(root)
            .setCommandName(jarFilename)
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);

    System.exit(exitCode);
  }
}
