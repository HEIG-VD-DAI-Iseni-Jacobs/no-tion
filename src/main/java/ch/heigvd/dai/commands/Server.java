package ch.heigvd.dai.commands;

import picocli.CommandLine;
import java.util.concurrent.Callable;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;
import ch.heigvd.dai.model.*;

@CommandLine.Command(name = "server", description = "Start the server No-Tion.")
public class Server implements Callable<Integer> {
    public enum Message {
        OK,
        ERROR,
    }

    private static final int NUMBER_OF_THREADS = 20;

    protected int port;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port à utiliser (défaut: ${DEFAULT-VALUE}).",
            defaultValue = "16447")

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Server()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try (ServerSocket serverSocket = new ServerSocket(port);
             ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);) {
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

                    String[] tokens = parseLine(line);
                    Client.Command command = null;

                    try {
                        command = Client.Command.valueOf(tokens[0]);
                    } catch (Exception e) {
                        // Do nothing
                    }

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

        private String[] parseLine(String line) {
            List<String> tokens = new ArrayList<>();
            boolean inQuotes = false;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ' ' && !inQuotes) {
                    if (!sb.isEmpty()) {
                        tokens.add(sb.toString());
                        sb.setLength(0);
                    }
                } else {
                    sb.append(c);
                }
            }

            if (!sb.isEmpty()) {
                tokens.add(sb.toString());
            }

            return tokens.toArray(new String[0]);
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
                sendError(out, -3);
            } else {
                String name = tokens[1];
                user = new User(name);
                connected = true;
                sendOK(out);
            }
        }

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

        private void handleListNotes(BufferedWriter out) throws IOException {
            List<Note> notesList = user.getNotes();
            int index = 1;
            for (Note note : notesList) {
                out.write(index + " " + note.getTitle() + "\n");
                index++;
            }
            out.flush();
        }

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
