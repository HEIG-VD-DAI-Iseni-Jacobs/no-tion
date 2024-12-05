package ch.heigvd.dai.commands;

import static ch.heigvd.dai.utils.MainUtils.parseInput;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Client implementation for the No-Tion note-taking application. Handles user interaction with the
 * server and supports various commands to manage notes. Implements Callable interface for
 * command-line execution via picocli.
 */
@CommandLine.Command(name = "client", description = "Client for the No-Tion application.")
public class Client implements Callable<Integer> {

  /** Enum representing the list of commands supported by the client. */
  public enum Command {
    CONNECT, // Connect to the server
    DISCONNECT, // Disconnect from the server
    CREATE_NOTE, // Create a new note
    DELETE_NOTE, // Delete an existing note
    LIST_NOTES, // List all notes
    GET_NOTE, // Retrieve a specific note by index
    UPDATE_CONTENT, // Update the content of a note
    UPDATE_TITLE, // Update the title of a note
    HELP, // Display help information
    QUIT // Quit the client
  }

  /** Tracks whether the client is connected to the server. */
  private boolean connected = false;

  /** Hostname or IP address of the server. */
  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Address of the server (default: ${DEFAULT-VALUE}).",
      defaultValue = "localhost")
  private String host;

  /** Port number of the server. */
  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port of the server (default: ${DEFAULT-VALUE}).",
      defaultValue = "16447")
  private int port;

  /**
   * Main entry point for the client. Manages user input and communicates with the server.
   *
   * @return 0 if execution succeeds, 1 if an error occurs.
   */
  @Override
  public Integer call() {
    try (
    // Establish a connection to the server
    Socket socket = new Socket(host, port);
        // Reader to receive responses from the server
        BufferedReader in =
            new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        // Writer to send commands to the server
        BufferedWriter out =
            new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        // Reader to get user input from the console
        BufferedReader userInputReader =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      System.out.println("[Client] Connected to server " + host + ":" + port);
      System.out.println();

      printHelp();

      // Main client loop
      while (!socket.isClosed()) {
        System.out.print("> ");
        String userInput = userInputReader.readLine(); // Read user input
        if (userInput == null) {
          break; // Exit if no input is provided
        }

        String[] inputTokens = parseInput(userInput);
        if (inputTokens.length == 0) {
          continue; // Ignore empty commands
        }

        String commandStr = inputTokens[0].toUpperCase(); // Get the command
        Command command;
        try {
          command = Command.valueOf(commandStr); // Parse the command
        } catch (IllegalArgumentException e) {
          System.out.println("Unknown command. Type HELP for the list of commands.");
          continue;
        }

        // Ensure the client is connected for specific commands
        if (!connected
            && command != Command.CONNECT
            && command != Command.QUIT
            && command != Command.HELP) {
          System.out.println("You must connect first using the CONNECT command.");
          continue;
        }

        // Handle commands
        switch (command) {
          case CONNECT -> {
            if (inputTokens.length < 2) {
              System.out.println("Usage: CONNECT <username>");
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
            socket.close(); // Close the connection
            System.out.println("Disconnected from the server.");
          }
          case CREATE_NOTE -> {
            if (inputTokens.length < 2) {
              System.out.println("Usage: CREATE_NOTE <title>");
              continue;
            }
            String title = inputTokens[1];
            sendCommand(out, "CREATE_NOTE " + title);
            String response = in.readLine();
            System.out.println(response);
          }
          case DELETE_NOTE -> {
            if (inputTokens.length < 2) {
              System.out.println("Usage: DELETE_NOTE <title>");
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
              System.out.println("Usage: UPDATE_CONTENT <index> <new_content>");
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
              System.out.println("Usage: UPDATE_TITLE <index> <new_title>");
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
            System.out.println("Exiting the client.");
            return 0;
          }
          default -> System.out.println("Unknown command. Type HELP for the list of commands.");
        }
      }
    } catch (IOException e) {
      System.err.println("[Client] Error: " + e.getMessage());
      return 1;
    }
    return 0;
  }

  /**
   * Sends a command to the server.
   *
   * @param out The writer to send the command.
   * @param command The command to send.
   * @throws IOException If an error occurs while writing.
   */
  private void sendCommand(BufferedWriter out, String command) throws IOException {
    out.write(command + "\n");
    out.flush();
  }

  /** Prints the list of available commands to the console. */
  private void printHelp() {
    System.out.println("Available commands:");
    System.out.println(
        "  CONNECT <username>         - Connect to the server with the specified username.");
    System.out.println("  DISCONNECT                 - Disconnect from the server.");
    System.out.println(
        "  CREATE_NOTE <title>        - Create a new note with the specified title.");
    System.out.println("  DELETE_NOTE <title>        - Delete the note with the specified title.");
    System.out.println("  LIST_NOTES                 - List all existing notes.");
    System.out.println(
        "  GET_NOTE <index>           - Retrieve the content of the note at the specified index.");
    System.out.println(
        "  UPDATE_CONTENT <index> <content> - Update the content of the note at the specified index.");
    System.out.println(
        "  UPDATE_TITLE <index> <title> - Update the title of the note at the specified index.");
    System.out.println("  HELP                       - Display this help message.");
    System.out.println("  QUIT                       - Exit the client.");
  }
}
