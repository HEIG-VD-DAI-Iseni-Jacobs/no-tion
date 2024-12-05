package ch.heigvd.dai.model;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class User {
  private final String name;
  private final CopyOnWriteArrayList<Note> notes;

  public User(String name) {
    this.name = name;
    this.notes = new CopyOnWriteArrayList<>();
  }

  public String getName() {
    return name;
  }

  public List<Note> getNotes() {
    return notes;
  }

  /**
   * Checks if a note with a given title exists.
   *
   * @param title The title of the note.
   * @return true if a note with the given title exists, false otherwise.
   */
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

  /**
   * Deletes a note by its title.
   *
   * @param title The title of the note.
   * @return true if the note was deleted, false otherwise.
   */
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
