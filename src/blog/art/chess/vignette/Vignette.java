/*
 * MIT License
 *
 * Copyright (c) 2026 Ivan Denkovski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blog.art.chess.vignette;

import blog.art.chess.vignette.Solver.Problem;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

class Vignette {

  private static final Logger LOGGER = Logger.getLogger(Vignette.class.getName());

  static void main(String[] args) {
    configureLogging();
    boolean help = false;
    boolean version = false;
    for (String arg : args) {
      switch (arg) {
        case "--help" -> help = true;
        case "--version" -> version = true;
        default -> {
          if (arg.matches("-[hV]+")) {
            for (char letter : arg.substring(1).toCharArray()) {
              switch (letter) {
                case 'h' -> help = true;
                case 'V' -> version = true;
              }
            }
          } else {
            LOGGER.warning("Invalid argument: '%s'.".formatted(arg));
            System.exit(1);
          }
        }
      }
    }
    if (help) {
      System.out.printf("Usage:%n" + "  java -jar Vignette.jar [OPTIONS]%n%n"
          + "Chess mate searcher. Reads problems as EPD records (with one operation:%n"
          + "  dm for direct mate or acd for perft) until EOF, then solves them.%n%n" + "Options:%n"
          + "  -h, --help       Show help and exit%n"
          + "  -V, --version    Show version and exit%n");
      System.exit(0);
    }
    if (version) {
      System.out.printf("Vignette %s%n" + "Copyright (c) 2026 Ivan Denkovski%n" + "License: MIT%n",
          getVersion());
      System.exit(0);
    }
    LOGGER.info("Vignette %s Copyright (c) 2026 Ivan Denkovski".formatted(getVersion()));
    List<Problem> problems = Parser.readAllProblems();
    for (Problem problem : problems) {
      Parser.write(problem);
      Solver.solve(problem);
    }
  }

  private static String getVersion() {
    Package pkg = Vignette.class.getPackage();
    if (pkg != null) {
      String version = pkg.getImplementationVersion();
      if (version != null) {
        return version;
      }
      return "(development)";
    }
    return "(unknown)";
  }

  private static void configureLogging() {
    Logger root = Logger.getLogger("");
    root.setLevel(Level.INFO);
    for (Handler handler : root.getHandlers()) {
      handler.setLevel(Level.INFO);
    }
  }
}
