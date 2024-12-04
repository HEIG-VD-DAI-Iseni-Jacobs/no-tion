package ch.heigvd.dai.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class User {
    private String name;
    private List<Note> notes;

    public User(String name) {
        this.name = name;
        this.notes = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public boolean hasNoteWithTitle(String title) {
        for (Note note : notes) {
            if (note.getTitle().equals(title)) {
                return true;
            }
        }
        return false;
    }

    public void addNote(Note note) {
        notes.add(note);
    }

    public boolean deleteNoteByTitle(String title) {
        Iterator<Note> iterator = notes.iterator();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            if (note.getTitle().equals(title)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }
}
