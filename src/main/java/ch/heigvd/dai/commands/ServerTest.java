// package ch.heigvd.dai.commands;
//
// import java.util.concurrent.Callable;
// import java.net.ServerSocket;
// import java.net.Socket;
// import java.io.*;
// import java.nio.charset.StandardCharsets;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.*;
// import ch.heigvd.dai.model.*;
//
// import static ch.heigvd.dai.utils.MainUtils.parseInput;
//
// public class ServerTest {
//    public enum Message {
//        OK,
//        ERROR,
//    }
//
//    private static final List<User> users = new ArrayList<>();
//    private static final int NUMBER_OF_THREADS = 20;
//
//    private int port = 16447; // Valeur par défaut
//
//    public void start(String[] args) {
//        parseArguments(args);
//
//        try (ServerSocket serverSocket = new ServerSocket(port);
//             ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS)) {
//            System.out.println("[Server] démarrage");
//            System.out.println("[Server] écoute sur le port " + port);
//
//            while (!serverSocket.isClosed()) {
//                Socket clientSocket = serverSocket.accept();
//                executor.submit(new ClientHandler(clientSocket));
//            }
//        } catch (IOException e) {
//            System.out.println("[SERVEUR] IOException: " + e);
//            System.exit(1);
//        }
//    }
//
//    private void parseArguments(String[] args) {
//        for (int i = 0; i < args.length; i++) {
//            switch (args[i]) {
//                case "-p":
//                case "--port":
//                    if (i + 1 < args.length) {
//                        try {
//                            port = Integer.parseInt(args[++i]);
//                        } catch (NumberFormatException e) {
//                            System.err.println("Le port doit être un nombre entier.");
//                            System.exit(1);
//                        }
//                    } else {
//                        System.err.println("Option " + args[i] + " nécessite un argument.");
//                        System.exit(1);
//                    }
//                    break;
//                default:
//                    System.err.println("Option inconnue : " + args[i]);
//                    System.exit(1);
//            }
//        }
//    }
//
//    static class ClientHandler implements Runnable {
//        private final Socket socket;
//        private User user;
//        private boolean connected = false;
//
//        public ClientHandler(Socket socket) {
//            this.socket = socket;
//        }
//
//        @Override
//        public void run() {
//            try (socket; // Utilisation de try-with-resources avec le socket
//                 BufferedReader in =
//                         new BufferedReader(
//                                 new InputStreamReader(socket.getInputStream(),
// StandardCharsets.UTF_8));
//                 BufferedWriter out =
//                         new BufferedWriter(
//                                 new OutputStreamWriter(socket.getOutputStream(),
// StandardCharsets.UTF_8))) {
//                String line;
//
//                System.out.println(
//                        "[SERVEUR] Nouveau client connecté depuis "
//                                + socket.getInetAddress().getHostAddress()
//                                + ":"
//                                + socket.getPort());
//
//                while ((line = in.readLine()) != null) {
//                    line = line.trim();
//                    if (line.isEmpty()) continue;
//
//                    String[] tokens = parseInput(line);
//                    // Débogage : Afficher les tokens reçus
//                    System.out.println("Tokens reçus : " + Arrays.toString(tokens));
//                    ClientTest.Command command = null;
//
//                    if (tokens.length > 0) {
//                        try {
//                            command = ClientTest.Command.valueOf(tokens[0].toUpperCase());
//                        } catch (IllegalArgumentException e) {
//                            // Commande inconnue
//                        }
//                    }
//
//                    if (command == null) {
//                        sendError(out, -3);
//                        continue;
//                    }
//
//                    if (!connected && command != ClientTest.Command.CONNECT && command !=
// ClientTest.Command.QUIT && command != ClientTest.Command.HELP) {
//                        sendError(out, -3);
//                        continue;
//                    }
//
//                    switch (command) {
//                        case CONNECT -> handleConnect(tokens, out);
//                        case DISCONNECT -> {
//                            sendOK(out);
//                            socket.close();
//                            return;
//                        }
//                        case CREATE_NOTE -> handleCreateNote(tokens, out);
//                        case DELETE_NOTE -> handleDeleteNote(tokens, out);
//                        case LIST_NOTES -> handleListNotes(out);
//                        case GET_NOTE -> handleGetNote(tokens, out);
//                        case UPDATE_CONTENT -> handleUpdateContent(tokens, out);
//                        case UPDATE_TITLE -> handleUpdateTitle(tokens, out);
//                        case HELP -> sendHelp(out);
//                        case QUIT -> {
//                            if (connected) {
//                                sendOK(out);
//                            }
//                            socket.close();
//                            System.out.println("[SERVEUR] Fermeture de la connexion avec le
// client.");
//                            return;
//                        }
//                        default -> sendError(out, -3);
//                    }
//                }
//                out.flush();
//                System.out.println("[SERVEUR] Connexion avec le client terminée");
//            } catch (IOException e) {
//                System.out.println("[SERVEUR] IOException: " + e);
//            }
//        }
//
//        private void sendOK(BufferedWriter out) throws IOException {
//            out.write("OK\n");
//            out.flush();
//        }
//
//        private void sendError(BufferedWriter out, int code) throws IOException {
//            out.write("ERROR " + code + "\n");
//            out.flush();
//        }
//
//        private void sendHelp(BufferedWriter out) throws IOException {
//            out.write("Commandes disponibles :\n");
//            out.write("  CONNECT <nom_utilisateur>         - Se connecter avec le nom
// d'utilisateur spécifié.\n");
//            out.write("  DISCONNECT                        - Se déconnecter du serveur.\n");
//            out.write("  CREATE_NOTE <titre>               - Créer une nouvelle note avec le titre
// spécifié.\n");
//            out.write("  DELETE_NOTE <titre>               - Supprimer la note avec le titre
// spécifié.\n");
//            out.write("  LIST_NOTES                        - Lister toutes les notes
// existantes.\n");
//            out.write("  GET_NOTE <index>                  - Récupérer le contenu de la note à
// l'index spécifié.\n");
//            out.write("  UPDATE_CONTENT <index> <contenu>  - Mettre à jour le contenu de la note à
// l'index spécifié.\n");
//            out.write("  UPDATE_TITLE <index> <titre>      - Mettre à jour le titre de la note à
// l'index spécifié.\n");
//            out.write("  HELP                              - Afficher ce message d'aide.\n");
//            out.write("  QUIT                              - Quitter la connexion.\n");
//            out.flush();
//        }
//
//        private void handleConnect(String[] tokens, BufferedWriter out) throws IOException {
//            if (connected || tokens.length < 2) {
//                sendError(out, -3);
//            } else {
//                String name = tokens[1];
//                // Vérifie si l'utilisateur existe déjà
//                for (User u : users) {
//                    if (u.getName().equals(name)) {
//                        user = u;
//                        break;
//                    }
//                }
//                if (user == null) {
//                    user = new User(name);
//                    users.add(user);
//                }
//                connected = true;
//                sendOK(out);
//            }
//        }
//
//        private void handleCreateNote(String[] tokens, BufferedWriter out) throws IOException {
//            if (tokens.length < 2) {
//                sendError(out, -3);
//                return;
//            }
//            String title = tokens[1];
//            if (user.hasNoteWithTitle(title)) {
//                sendError(out, -2);
//            } else {
//                user.addNote(new Note(title, ""));
//                sendOK(out);
//            }
//        }
//
//        private void handleDeleteNote(String[] tokens, BufferedWriter out) throws IOException {
//            if (tokens.length < 2) {
//                sendError(out, -3);
//                return;
//            }
//            String title = tokens[1];
//            if (user.deleteNoteByTitle(title)) {
//                sendOK(out);
//            } else {
//                sendError(out, -1);
//            }
//        }
//
//        private void handleListNotes(BufferedWriter out) throws IOException {
//            List<Note> notesList = user.getNotes();
//            int index = 1;
//            for (Note note : notesList) {
//                out.write(index + " " + note.getTitle() + "\n");
//                index++;
//            }
//            out.write("\n");
//            out.flush();
//        }
//
//        private void handleGetNote(String[] tokens, BufferedWriter out) throws IOException {
//            if (tokens.length < 2) {
//                sendError(out, -3);
//                return;
//            }
//            int index;
//            try {
//                index = Integer.parseInt(tokens[1]) - 1;
//            } catch (NumberFormatException e) {
//                sendError(out, -3);
//                return;
//            }
//            List<Note> notesList = user.getNotes();
//            if (index < 0 || index >= notesList.size()) {
//                sendError(out, -1);
//            } else {
//                Note note = notesList.get(index);
//                out.write("NOTE " + note.getContent() + "\n");
//                out.flush();
//            }
//        }
//
//        private void handleUpdateContent(String[] tokens, BufferedWriter out) throws IOException {
//            if (tokens.length < 3) {
//                sendError(out, -3);
//                return;
//            }
//            int index;
//            try {
//                index = Integer.parseInt(tokens[1]) - 1;
//            } catch (NumberFormatException e) {
//                sendError(out, -3);
//                return;
//            }
//            List<Note> notesList = user.getNotes();
//            if (index < 0 || index >= notesList.size()) {
//                sendError(out, -1);
//                return;
//            }
//            String newContent = tokens[2];
//            Note note = notesList.get(index);
//            note.setContent(newContent);
//            sendOK(out);
//        }
//
//        private void handleUpdateTitle(String[] tokens, BufferedWriter out) throws IOException {
//            if (tokens.length < 3) {
//                sendError(out, -3);
//                return;
//            }
//            int index;
//            try {
//                index = Integer.parseInt(tokens[1]) - 1;
//            } catch (NumberFormatException e) {
//                sendError(out, -3);
//                return;
//            }
//            String newTitle = tokens[2];
//            if (newTitle.isEmpty()) {
//                sendError(out, -3);
//                return;
//            }
//            List<Note> notesList = user.getNotes();
//            if (index < 0 || index >= notesList.size()) {
//                sendError(out, -1);
//                return;
//            }
//            if (user.hasNoteWithTitle(newTitle)) {
//                sendError(out, -2);
//                return;
//            }
//            Note note = notesList.get(index);
//            note.setTitle(newTitle);
//            sendOK(out);
//        }
//    }
//
//    public static void main(String[] args) {
//        ServerTest server = new ServerTest();
//        server.start(args);
//    }
// }
