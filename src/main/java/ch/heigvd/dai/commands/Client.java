package ch.heigvd.dai.commands;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Client for the No-Tion application with a menu-driven interface. The user selects an action from
 * a menu and then provides required parameters step by step.
 */
@CommandLine.Command(name = "client", description = "Client for the No-Tion application.")
public class Client implements Callable<Integer> {

  /** Enum representing the list of commands supported by the client. */
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

  /** Tracks whether the client is connected to the server. */
  private boolean connected = false;

  /** Hostname or IP address of the server. */
  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Server address (default: ${DEFAULT-VALUE}).",
      defaultValue = "localhost")
  private String host;

  /** Port number of the server. */
  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Server port (default: ${DEFAULT-VALUE}).",
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

      // Display welcome message
      System.out.println("========================================");
      System.out.println("   Welcome to the No-Tion Client!       ");
      System.out.println("========================================");
      System.out.println("Connected to server: " + host + ":" + port);
      System.out.println("Use the menu to select actions.");
      System.out.println("You must CONNECT with a username before performing note operations.");
      System.out.println("Type QUIT to exit at any time.");
      System.out.println();

      // Main loop
      while (!socket.isClosed()) {
        Command command = chooseCommand(userInputReader); // Show menu and get user choice
        if (command == null) {
          // Invalid choice, just continue
          continue;
        }

        // Check if user must be connected for this command
        if (!connected
            && command != Command.CONNECT
            && command != Command.HELP
            && command != Command.QUIT) {
          System.out.println("You must connect first using the CONNECT command.\n");
          continue;
        }

        switch (command) {
          case CONNECT -> {
            String username = prompt(userInputReader, "Enter username: ");
            if (username == null || username.isBlank()) {
              System.out.println("Invalid username.\n");
              continue;
            }
            sendCommand(out, "CONNECT " + username);
            String response = in.readLine();
            handleServerResponse(response);
            if ("OK".equals(response)) {
              connected = true;
              System.out.println("You are now connected as '" + username + "'.\n");
            } else {
              System.out.println();
            }
          }
          case DISCONNECT -> {
            sendCommand(out, "DISCONNECT");
            socket.close();
            System.out.println("Disconnected from the server. Goodbye!\n");
          }
          case CREATE_NOTE -> {
            String title = prompt(userInputReader, "Enter note title: ");
            if (title == null || title.isBlank()) {
              System.out.println("Invalid title.\n");
              continue;
            }
            sendCommand(out, "CREATE_NOTE " + title);
            String response = in.readLine();
            handleServerResponse(response);
            System.out.println();
          }
          case DELETE_NOTE -> {
            String title = prompt(userInputReader, "Enter note title to delete: ");
            if (title == null || title.isBlank()) {
              System.out.println("Invalid title.\n");
              continue;
            }
            sendCommand(out, "DELETE_NOTE " + title);
            String response = in.readLine();
            handleServerResponse(response);
            System.out.println();
          }
          case LIST_NOTES -> {
            sendCommand(out, "LIST_NOTES");
            String responseLine;
            boolean notesFound = false;
            while ((responseLine = in.readLine()) != null && !responseLine.isEmpty()) {
              if (!notesFound) {
                System.out.println("Your notes:");
                notesFound = true;
              }
              System.out.println("  " + responseLine);
            }
            if (!notesFound) {
              System.out.println("You have no notes.\n");
            } else {
              System.out.println();
            }
          }
          case GET_NOTE -> {
            String index = prompt(userInputReader, "Enter note index: ");
            if (index == null || index.isBlank()) {
              System.out.println("Invalid index.\n");
              continue;
            }
            sendCommand(out, "GET_NOTE " + index);
            String response = in.readLine();
            if (response != null && response.startsWith("NOTE ")) {
              String content = response.substring(5);
              System.out.println("Note content:\n" + content + "\n");
            } else {
              handleServerResponse(response);
              System.out.println();
            }
          }
          case UPDATE_CONTENT -> {
            String index = prompt(userInputReader, "Enter note index to update: ");
            if (index == null || index.isBlank()) {
              System.out.println("Invalid index.\n");
              continue;
            }
            String newContent = prompt(userInputReader, "Enter new content: ");
            if (newContent == null) {
              System.out.println("Invalid content.\n");
              continue;
            }
            sendCommand(out, "UPDATE_CONTENT " + index + " \"" + newContent + "\"");
            String response = in.readLine();
            handleServerResponse(response);
            System.out.println();
          }
          case UPDATE_TITLE -> {
            String index = prompt(userInputReader, "Enter note index to update: ");
            if (index == null || index.isBlank()) {
              System.out.println("Invalid index.\n");
              continue;
            }
            String newTitle = prompt(userInputReader, "Enter new title: ");
            if (newTitle == null || newTitle.isBlank()) {
              System.out.println("Invalid title.\n");
              continue;
            }
            sendCommand(out, "UPDATE_TITLE " + index + " " + newTitle);
            String response = in.readLine();
            handleServerResponse(response);
            System.out.println();
          }
          case HELP -> {
            printHelp();
            System.out.println();
          }
          case QUIT -> {
            if (connected) {
              sendCommand(out, "DISCONNECT");
            }
            socket.close();
            System.out.println("Exiting the client. Have a great day!\n");
            return 0;
          }
          default -> {
            System.out.println("Unknown command.\n");
          }
        }
      }
    } catch (IOException e) {
      System.err.println("[Client] Error: " + e.getMessage());
      return 1;
    }

    return 0;
  }

  /** Shows a menu of available commands and returns the chosen command. */
  private Command chooseCommand(BufferedReader userInputReader) throws IOException {
    System.out.println("========================================");
    System.out.println("             MAIN MENU                  ");
    System.out.println("========================================");
    System.out.println("1) CONNECT");
    System.out.println("2) DISCONNECT");
    System.out.println("3) CREATE_NOTE");
    System.out.println("4) DELETE_NOTE");
    System.out.println("5) LIST_NOTES");
    System.out.println("6) GET_NOTE");
    System.out.println("7) UPDATE_CONTENT");
    System.out.println("8) UPDATE_TITLE");
    System.out.println("9) HELP");
    System.out.println("0) QUIT");
    System.out.print("Choose an action (0-9): ");

    String line = userInputReader.readLine();
    if (line == null) {
      return null;
    }

      return switch (line.trim()) {
          case "0" -> Command.QUIT;
          case "1" -> Command.CONNECT;
          case "2" -> Command.DISCONNECT;
          case "3" -> Command.CREATE_NOTE;
          case "4" -> Command.DELETE_NOTE;
          case "5" -> Command.LIST_NOTES;
          case "6" -> Command.GET_NOTE;
          case "7" -> Command.UPDATE_CONTENT;
          case "8" -> Command.UPDATE_TITLE;
          case "9" -> Command.HELP;
          default -> {
              System.out.println("Invalid choice. Please try again.\n");
              yield null;
          }
      };
  }

  private void sendCommand(BufferedWriter out, String command) throws IOException {
    out.write(command + "\n");
    out.flush();
  }

  /** Prompts the user for input with a given message. */
  private String prompt(BufferedReader reader, String message) throws IOException {
    System.out.print(message);
    return reader.readLine();
  }

  /** Handle server responses, providing a more readable output. */
  private void handleServerResponse(String response) {
    if (response == null) {
      System.out.println("No response from server.");
      return;
    }

    if (response.startsWith("ERROR")) {
      System.out.println("Server error: " + response);
    } else if ("OK".equals(response)) {
      System.out.println("Operation successful.");
    } else {
      System.out.println(response);
    }
  }

  /** Print help information about the commands. */
  private void printHelp() {
    System.out.println("========================================");
    System.out.println("              HELP MENU                 ");
    System.out.println("========================================");
    System.out.println("CONNECT           Connect to the server (will prompt for username).");
    System.out.println("DISCONNECT        Disconnect from the server.");
    System.out.println("CREATE_NOTE       Create a new note (will prompt for a title).");
    System.out.println("DELETE_NOTE       Delete a note (will prompt for a title).");
    System.out.println("LIST_NOTES        List all existing notes.");
    System.out.println("GET_NOTE          Show the content of a note (will prompt for index).");
    System.out.println(
        "UPDATE_CONTENT    Update note content (will prompt for index and new content).");
    System.out.println(
        "UPDATE_TITLE      Update the note title (will prompt for index and new title).");
    System.out.println("HELP              Display this help message.");
    System.out.println("QUIT              Exit the client.");
    System.out.println("========================================");
  }
}
