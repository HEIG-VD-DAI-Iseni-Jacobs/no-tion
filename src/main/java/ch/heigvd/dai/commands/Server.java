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

@CommandLine.Command(name = "server", description = "Démarre le serveur No-Tion.")
public class Server implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port à utiliser (défaut: ${DEFAULT-VALUE}).",
            defaultValue = "16447")
    protected int port;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Server()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        ExecutorService executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVEUR] Écoute sur le port " + port);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("[SERVEUR] IOException: " + e.getMessage());
            return 1;
        } finally {
            executorService.shutdown();
        }

        return 0;
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private User person;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    socket; // Le socket sera fermé automatiquement
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
            ) {
                String line;
                boolean connected = false;

                System.out.println("[SERVEUR] Nouveau client connecté depuis " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    String[] tokens = parseLine(line);
                    String command = tokens[0];

                    if (!connected && !command.equals("CONNECT")) {
                        sendError(out, -3);
                        continue;
                    }

                    switch (command) {
                        case "CONNECT":
                            if (tokens.length < 2) {
                                sendError(out, -3);
                            } else {
                                String name = tokens[1];
                                person = new User(name);
                                connected = true;
                                sendOK(out);
                            }
                            break;
                        case "DISCONNECT":
                            socket.close();
                            return;
                        case "CREATE_NOTE":
                            handleCreateNote(tokens, out);
                            break;
                        case "DELETE_NOTE":
                            handleDeleteNote(tokens, out);
                            break;
                        case "LIST_NOTES":
                            handleListNotes(out);
                            break;
                        case "GET_NOTE":
                            handleGetNote(tokens, out);
                            break;
                        case "UPDATE_CONTENT":
                            handleUpdateContent(tokens, out);
                            break;
                        case "UPDATE_TITLE":
                            handleUpdateTitle(tokens, out);
                            break;
                        default:
                            sendError(out, -3);
                            break;
                    }
                }

            } catch (IOException e) {
                System.err.println("[SERVEUR] IOException: " + e.getMessage());
            } finally {
                System.out.println("[SERVEUR] Connexion avec le client terminée");
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

        private void handleCreateNote(String[] tokens, BufferedWriter out) throws IOException {
            if (tokens.length < 2) {
                sendError(out, -3);
                return;
            }
            String title = tokens[1];
            if (person.hasNoteWithTitle(title)) {
                sendError(out, -2);
            } else {
                person.addNote(new Note(title, ""));
                sendOK(out);
            }
        }

        private void handleDeleteNote(String[] tokens, BufferedWriter out) throws IOException {
            if (tokens.length < 2) {
                sendError(out, -3);
                return;
            }
            String title = tokens[1];
            if (person.deleteNoteByTitle(title)) {
                sendOK(out);
            } else {
                sendError(out, -1);
            }
        }

        private void handleListNotes(BufferedWriter out) throws IOException {
            List<Note> notesList = person.getNotes();
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
            List<Note> notesList = person.getNotes();
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
            String newContent = tokens[2];
            List<Note> notesList = person.getNotes();
            if (index < 0 || index >= notesList.size()) {
                sendError(out, -1);
            } else {
                Note note = notesList.get(index);
                note.setContent(newContent);
                sendOK(out);
            }
        }

        private void handleUpdateTitle(String[] tokens, BufferedWriter out) throws IOException {
            int index = getNoteIndex(tokens, out);
            if (index == -1) return;
            String newTitle = tokens[2];
            List<Note> notesList = person.getNotes();
            if (index < 0 || index >= notesList.size()) {
                sendError(out, -1);
            } else if (person.hasNoteWithTitle(newTitle)) {
                sendError(out, -2);
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
