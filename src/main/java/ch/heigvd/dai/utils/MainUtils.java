package ch.heigvd.dai.utils;

import java.util.ArrayList;
import java.util.List;

public class MainUtils {
  public static String[] parseInput(String input) {
    List<String> tokens = new ArrayList<>();
    boolean inQuotes = false;
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);

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
}
