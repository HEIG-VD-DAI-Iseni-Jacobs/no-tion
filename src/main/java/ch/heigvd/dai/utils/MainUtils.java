package ch.heigvd.dai.utils;

import java.util.ArrayList;
import java.util.List;

public class MainUtils {
  /**
   * Parses an input string into multiple tokens considering quotation marks.
   *
   * @param input The input string to parse.
   * @return An array of extracted tokens.
   */
  public static String[] parseInput(String input) {
    List<String> tokens = new ArrayList<>();
    boolean inQuotes = false;
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

      if (c == '"') {
        inQuotes = !inQuotes; // Toggle quotes
      } else if (c == ' ' && !inQuotes) {
        if (!sb.isEmpty()) {
          tokens.add(sb.toString());
          sb.setLength(0); // Reset the StringBuilder
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
}
