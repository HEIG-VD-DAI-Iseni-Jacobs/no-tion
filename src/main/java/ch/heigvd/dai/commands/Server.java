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

  public enum Error {
    NOTE_NOT_FOUND(-1),
    NOTE_ALREADY_EXISTS(-2),
    INVALID_COMMAND(-3);

    private final int code;

    Error(int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }
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

  static class ClientHandler implements Runnable {
    private final Socket socket;
    private User user;
    private boolean connected = false;

    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try (socket;
           BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
           BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
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
            // unknown command
          }

          if (!connected && !tokens[0].equals(Client.Command.CONNECT.toString())) {
            sendError(out, Error.INVALID_COMMAND.getCode());
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
            case null, default -> sendError(out, Error.INVALID_COMMAND.getCode());
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

    private void handleConnect(String[] tokens, BufferedWriter out) throws IOException {
      if (connected || tokens.length < 2) {
        sendError(out, Error.INVALID_COMMAND.getCode());
      } else {
        String name = tokens[1];
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

    private void handleCreateNote(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 2) {
        sendError(out, Error.INVALID_COMMAND.getCode());
        return;
      }
      String title = tokens[1];
      if (user.hasNoteWithTitle(title)) {
        sendError(out, Error.NOTE_ALREADY_EXISTS.getCode());
      } else {
        // Note initially empty, no scrambling needed since it's empty
        user.addNote(new Note(title, ""));
        sendOK(out);
      }
    }

    private void handleDeleteNote(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 2) {
        sendError(out, Error.INVALID_COMMAND.getCode());
        return;
      }
      String title = tokens[1];
      if (user.deleteNoteByTitle(title)) {
        sendOK(out);
      } else {
        sendError(out, Error.NOTE_NOT_FOUND.getCode());
      }
    }

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

    private void handleGetNote(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 2) {
        sendError(out, Error.INVALID_COMMAND.getCode());
        return;
      }
      int index;
      try {
        index = Integer.parseInt(tokens[1]) - 1;
      } catch (NumberFormatException e) {
        sendError(out, Error.INVALID_COMMAND.getCode());
        return;
      }
      List<Note> notesList = user.getNotes();
      if (index < 0 || index >= notesList.size()) {
        sendError(out, Error.NOTE_NOT_FOUND.getCode());
      } else {
        Note note = notesList.get(index);
        out.write("NOTE " + note.getContent() + "\n");
        out.flush();
      }
    }

    /**
     * Handle the update content command. Update the content of the note.
     * Before storing the content, we scramble each word so that only the first and last letters
     * remain in place, and the middle letters are randomized.
     */
    private void handleUpdateContent(String[] tokens, BufferedWriter out) throws IOException {
      int index = getNoteIndex(tokens, out);
      if (index == Error.NOTE_NOT_FOUND.getCode()) return;
      List<Note> notesList = user.getNotes();
      if (index < 0 || index >= notesList.size()) {
        sendError(out, Error.NOTE_NOT_FOUND.getCode());
        return;
      }
      // The content is at tokens[2]
      String newContent = tokens[2];

      // Scramble the content before storing
      newContent = scrambleContent(newContent);

      Note note = notesList.get(index);
      note.setContent(newContent);
      sendOK(out);
    }

    private void handleUpdateTitle(String[] tokens, BufferedWriter out) throws IOException {
      int index = getNoteIndex(tokens, out);
      List<Note> notesList = user.getNotes();
      if (index < 0 || index >= notesList.size()) {
        sendError(out, Error.NOTE_NOT_FOUND.getCode());
        return;
      }
      String newTitle = tokens[2];
      if (user.hasNoteWithTitle(newTitle)) {
        sendError(out, Error.NOTE_ALREADY_EXISTS.getCode());
      } else if (newTitle.isEmpty()) {
        sendError(out, Error.INVALID_COMMAND.getCode());
      } else {
        Note note = notesList.get(index);
        note.setTitle(newTitle);
        sendOK(out);
      }
    }

    private int getNoteIndex(String[] tokens, BufferedWriter out) throws IOException {
      if (tokens.length < 3) {
        sendError(out, Error.INVALID_COMMAND.getCode());
        return Error.NOTE_NOT_FOUND.getCode();
      }
      int index;
      try {
        index = Integer.parseInt(tokens[1]) - 1;
        return index;
      } catch (NumberFormatException e) {
        sendError(out, Error.INVALID_COMMAND.getCode());
      }
      return Error.NOTE_NOT_FOUND.getCode();
    }

    /**
     * Scramble the content of a note.
     * For each word, the first and last letter remain the same, but the middle letters are shuffled.
     */
    private String scrambleContent(String content) {
      String[] words = content.split(" ");
      StringBuilder scrambled = new StringBuilder();
      for (int i = 0; i < words.length; i++) {
        if (i > 0) scrambled.append(" ");
        scrambled.append(scrambleWord(words[i]));
      }
      return scrambled.toString();
    }

    /**
     * Scramble a single word by shuffling the letters between the first and last character.
     */
    private String scrambleWord(String word) {
      if (word.length() <= 3) {
        return word; // Too short to shuffle or not worth changing
      }
      char[] chars = word.toCharArray();
      List<Character> middle = new ArrayList<>();
      for (int i = 1; i < chars.length - 1; i++) {
        middle.add(chars[i]);
      }
      // Shuffle the middle characters
      Collections.shuffle(middle);
      for (int i = 1; i < chars.length - 1; i++) {
        chars[i] = middle.get(i - 1);
      }
      return new String(chars);
    }
  }
}
