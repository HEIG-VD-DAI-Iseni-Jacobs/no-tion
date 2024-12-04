package ch.heigvd.dai.commands;

public class Client {
    public enum Command {
        CONNECT,
        DISCONNECT,
        CREATE_NOTE,
        DELETE_NOTE,
        LIST_NOTES,
        GET_NOTE,
        UPDATE_CONTENT,
        UPDATE_TITLE
    }
}
