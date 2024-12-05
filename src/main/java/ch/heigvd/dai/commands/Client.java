package ch.heigvd.dai.commands;

import static ch.heigvd.dai.utils.MainUtils.parseInput;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Client pour l'application No-Tion.")
public class Client implements Callable<Integer> {
  public enum Command {
    CONNECT,
    DISCONNECT,
    CREATE_NOTE,
    DELETE_NOTE,
    LIST_NOTES,
    GET_NOTE,
    UPDATE_CONTENT,
    UPDATE_TITLE,
    HELP,
    QUIT
  }

  private boolean connected = false;

  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Adresse du serveur (par défaut: ${DEFAULT-VALUE}).",
      defaultValue = "localhost")
  private String host;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port du serveur (par défaut: ${DEFAULT-VALUE}).",
      defaultValue = "16447")
  private int port;

  @Override
  public Integer call() {
    try (Socket socket = new Socket(host, port);
        BufferedReader in =
            new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter out =
            new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader userInputReader =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      System.out.println("[Client] Connecté au serveur " + host + ":" + port);
      System.out.println();
      printHelp();
      while (!socket.isClosed()) {
        System.out.print("> ");
        String userInput = userInputReader.readLine();
        if (userInput == null) {
          break;
        }
        String[] inputTokens = parseInput(userInput);
        if (inputTokens.length == 0) {
          continue;
        }
        String commandStr = inputTokens[0].toUpperCase();
        Command command;
        try {
          command = Command.valueOf(commandStr);
        } catch (IllegalArgumentException e) {
          System.out.println("Commande inconnue. Tapez HELP pour la liste des commandes.");
          continue;
        }
        if (!connected
            && command != Command.CONNECT
            && command != Command.QUIT
            && command != Command.HELP) {
          System.out.println("Vous devez d'abord vous connecter avec la commande CONNECT.");
          continue;
        }
        switch (command) {
          case CONNECT -> {
            if (inputTokens.length < 2) {
              System.out.println("Usage: CONNECT <nom_utilisateur>");
              continue;
            }
            String name = inputTokens[1];
            sendCommand(out, "CONNECT " + name);
            String response = in.readLine();
            System.out.println(response);
            if ("OK".equals(response)) {
              connected = true;
            }
          }
          case DISCONNECT -> {
            sendCommand(out, "DISCONNECT");
            socket.close();
            System.out.println("Déconnecté du serveur.");
          }
          case CREATE_NOTE -> {
            if (inputTokens.length < 2) {
              System.out.println("Usage: CREATE_NOTE <titre>");
              continue;
            }
            String title = inputTokens[1];
            sendCommand(out, "CREATE_NOTE " + title);
            String response = in.readLine();
            System.out.println(response);
          }
          case DELETE_NOTE -> {
            if (inputTokens.length < 2) {
              System.out.println("Usage: DELETE_NOTE <titre>");
              continue;
            }
            String title = inputTokens[1];
            sendCommand(out, "DELETE_NOTE " + title);
            String response = in.readLine();
            System.out.println(response);
          }
          case LIST_NOTES -> {
            sendCommand(out, "LIST_NOTES");
            String responseLine;
            while ((responseLine = in.readLine()) != null && !responseLine.isEmpty()) {
              System.out.println(responseLine);
            }
          }
          case GET_NOTE -> {
            if (inputTokens.length < 2) {
              System.out.println("Usage: GET_NOTE <index>");
              continue;
            }
            String index = inputTokens[1];
            sendCommand(out, "GET_NOTE " + index);
            String response = in.readLine();
            System.out.println(response);
          }
          case UPDATE_CONTENT -> {
            if (inputTokens.length < 3) {
              System.out.println("Usage: UPDATE_CONTENT <index> <nouveau_contenu>");
              continue;
            }
            String index = inputTokens[1];
            String newContent = inputTokens[2];
            sendCommand(out, "UPDATE_CONTENT " + index + " \"" + newContent + "\"");
            String response = in.readLine();
            System.out.println(response);
          }
          case UPDATE_TITLE -> {
            if (inputTokens.length < 3) {
              System.out.println("Usage: UPDATE_TITLE <index> <nouveau_titre>");
              continue;
            }
            String index = inputTokens[1];
            String newTitle = inputTokens[2];
            sendCommand(out, "UPDATE_TITLE " + index + " " + newTitle);
            String response = in.readLine();
            System.out.println(response);
          }
          case HELP -> printHelp();
          case QUIT -> {
            if (connected) {
              sendCommand(out, "DISCONNECT");
            }
            socket.close();
            System.out.println("Fermeture du client.");
            return 0;
          }
          default ->
              System.out.println("Commande inconnue. Tapez HELP pour la liste des commandes.");
        }
      }
    } catch (IOException e) {
      System.err.println("[Client] Erreur : " + e.getMessage());
      return 1;
    }
    return 0;
  }

  private void sendCommand(BufferedWriter out, String command) throws IOException {
    out.write(command + "\n");
    out.flush();
  }

  private void printHelp() {
    System.out.println("Commandes disponibles :");
    System.out.println(
        "  CONNECT <nom_utilisateur>         - Se connecter au serveur avec le nom d'utilisateur spécifié.");
    System.out.println("  DISCONNECT                        - Se déconnecter du serveur.");
    System.out.println(
        "  CREATE_NOTE <titre>               - Créer une nouvelle note avec le titre spécifié.");
    System.out.println(
        "  DELETE_NOTE <titre>               - Supprimer la note avec le titre spécifié.");
    System.out.println("  LIST_NOTES                        - Lister toutes les notes existantes.");
    System.out.println(
        "  GET_NOTE <index>                  - Récupérer le contenu de la note à l'index spécifié.");
    System.out.println(
        "  UPDATE_CONTENT <index> <contenu>  - Mettre à jour le contenu de la note à l'index spécifié.");
    System.out.println(
        "  UPDATE_TITLE <index> <titre>      - Mettre à jour le titre de la note à l'index spécifié.");
    System.out.println("  HELP                              - Afficher ce message d'aide.");
    System.out.println("  QUIT                              - Quitter le client.");
  }
  //    public static void main(String[] args) {
  //        int exitCode = new CommandLine(new Client()).execute(args);
  //        System.exit(exitCode);
  //    }
}
