package ch.heigvd.dai.commands;

import static ch.heigvd.dai.utils.MainUtils.parseInput;

import ch.heigvd.dai.model.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Start the server No-Tion.")
public class Server implements Callable<Integer> {
  public enum Message {
    OK,
    ERROR,
  }

  private static final CopyOnWriteArrayList<User> users = new CopyOnWriteArrayList<>();
  private static final int NUMBER_OF_THREADS = 20;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port à utiliser (défaut: ${DEFAULT-VALUE}).",
      defaultValue = "16447")
  private int port;

  @Override
  public Integer call() {
    try (ServerSocket serverSocket = new ServerSocket(port);
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS); ) {
      System.out.println("[Server] starting");
      System.out.println("[Server] listening on port " + port);

      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();
        executor.submit(new ClientHandler(clientSocket));
      }
    } catch (IOException e) {
      System.out.println("[SERVEUR] IOException: " + e);
      return 1;
    }
    return 0;
  }

  /**
   * The client handler class. Handle the client connection. Each client is handled in a separate thread.
   */
  static class ClientHandler implements Runnable {
    private final Socket socket;
    private User user;
    private boolean connected = false;

    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try (socket; // This allows to use try-with-resources with the socket
          BufferedReader in =
              new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
          BufferedWriter out =
              new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
        String line;

        System.out.println(
            "[SERVEUR] New client connected from "
                + socket.getInetAddress().getHostAddress()
                + ":"
                + socket.getPort());

        while ((line = in.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty()) continue;

          String[] tokens = parseInput(line);
          Client.Command command = null;

          try {
            command = Client.Command.valueOf(tokens[0]);
          } catch (Exception e) {
            // Do nothing
          }

          // Check if the user is connected and the command is not CONNECT
          if (!connected && !tokens[0].equals(Client.Command.CONNECT.toString())) {
            sendError(out, -3);
            continue;
          }

          switch (command) {
            case CONNECT -> handleConnect(tokens, out);
            case DISCONNECT -> {
              socket.close();
              return;
            }
            case CREATE_NOTE -> handleCreateNote(tokens, out);
            case DELETE_NOTE -> handleDeleteNote(tokens, out);
            case LIST_NOTES -> handleListNotes(out);
            case GET_NOTE -> handleGetNote(tokens, out);
            case UPDATE_CONTENT -> handleUpdateContent(tokens, out);
            case UPDATE_TITLE -> handleUpdateTitle(tokens, out);
            case null, default -> sendError(out, -3);
          }
        }
        out.flush();
        System.out.println("[SERVEUR] Connexion avec le client terminée");
      } catch (IOException e) {
        System.out.println("[SERVEUR] IOException: " + e);
      }
    }

    private void sendOK(BufferedWriter out) throws IOException {
      out.write("OK\n");
      out.flush();
    }

    private void sendError(BufferedWriter out, int code) throws IOException {
      out.write("ERROR " + code + "\n");
      out.flush();
    }

    /**
     * Handle the connect command. Connect the user to the server.
     *
     * @param tokens The tokens from the client.
     * @param out The output stream to send the error.
     * @throws IOException If an I/O error occurs.
     */
    private void handleConnect(String[] tokens, BufferedWriter out) throws IOException {
      if (connected || tokens.length < 2) {
        sendError(out, -3);
      } else {
        String name = tokens[1];
        // checks if user already exists
        for (User u : users) {
          if (u.getName().equals(name)) {
            user = u;
          }
        }
        if (user == null) {
          user = new User(name);
        }
        users.add(user);
        connected = true;
        sendOK(out);
      }
    }

    /**
     * Handle the create note command. Create a new note with the given title.
     *
     * @param tokens The tokens from the client.
     * @param out The output stream to send the error.
     * @throws IOException If an I/O error occurs.
     */
    private void handleCreateNote(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 2) {
        sendError(out, -3);
        return;
      }
      String title = tokens[1];
      if (user.hasNoteWithTitle(title)) {
        sendError(out, -2);
      } else {
        user.addNote(new Note(title, ""));
        sendOK(out);
      }
    }

    /**
     * Handle the delete note command. Delete the note with the given title.
     *
     * @param tokens The tokens from the client.
     * @param out The output stream to send the error.
     * @throws IOException If an I/O error occurs.
     */
    private void handleDeleteNote(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 2) {
        sendError(out, -3);
        return;
      }
      String title = tokens[1];
      if (user.deleteNoteByTitle(title)) {
        sendOK(out);
      } else {
        sendError(out, -1);
      }
    }

    /**
     * Handle the list notes command. Send the list of notes to the client.
     *
     * @param out The output stream to send the error.
     * @throws IOException If an I/O error occurs.
     */
    private void handleListNotes(BufferedWriter out) throws IOException {
      List<Note> notesList = user.getNotes();
      int index = 1;
      for (Note note : notesList) {
        out.write(index + " " + note.getTitle() + "\n");
        index++;
      }
      out.write("\n");
      out.flush();
    }

    /**
     * Handle the get note command. Send the content of the note to the client.
     *
     * @param tokens The tokens from the client.
     * @param out The output stream to send the error.
     * @throws IOException If an I/O error occurs.
     */
    private void handleGetNote(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 2) {
        sendError(out, -3);
        return;
      }
      int index;
      try {
        index = Integer.parseInt(tokens[1]) - 1;
      } catch (NumberFormatException e) {
        sendError(out, -3);
        return;
      }
      List<Note> notesList = user.getNotes();
      if (index < 0 || index >= notesList.size()) {
        sendError(out, -1);
      } else {
        Note note = notesList.get(index);
        out.write("NOTE " + note.getContent() + "\n");
        out.flush();
      }
    }

    /**
     * Handle the update content command. Update the content of the note.
     *
     * @param tokens The tokens from the client.
     * @param out The output stream to send the error.
     * @throws IOException If an I/O error occurs.
     */
    private void handleUpdateContent(String[] tokens, BufferedWriter out) throws IOException {
      int index = getNoteIndex(tokens, out);
      if (index == -1) return;
      List<Note> notesList = user.getNotes();
      if (index < 0 || index >= notesList.size()) {
        sendError(out, -1);
        return;
      }
      String newContent = tokens[2];
      Note note = notesList.get(index);
      note.setContent(newContent);
      sendOK(out);
    }

    /**
     * Handle the update title command. Update the title of the note.
     *
     * @param tokens The tokens from the client.
     * @param out The output stream to send the error.
     * @throws IOException If an I/O error occurs.
     */
    private void handleUpdateTitle(String[] tokens, BufferedWriter out) throws IOException {
      int index = getNoteIndex(tokens, out);
      List<Note> notesList = user.getNotes();
      if (index < 0 || index >= notesList.size()) {
        sendError(out, -1);
        return;
      }
      String newTitle = tokens[2];
      if (user.hasNoteWithTitle(newTitle)) {
        sendError(out, -2);
      } else if (newTitle.isEmpty()) {
        sendError(out, -3);
      } else {
        Note note = notesList.get(index);
        note.setTitle(newTitle);
        sendOK(out);
      }
    }

    /**
     * Get the index of the note from the tokens.
     *
     * @param tokens The tokens from the client.
     * @param out The output stream to send the error.
     * @return The index of the note.
     * @throws IOException If an I/O error occurs.
     */
    private int getNoteIndex(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 3) {
        sendError(out, -3);
        return -1;
      }
      int index;
      try {
        index = Integer.parseInt(tokens[1]) - 1;
        return index;
      } catch (NumberFormatException e) {
        sendError(out, -3);
      }
      return -1;
    }
  }
}
