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

import blog.art.chess.vignette.Engine.Position;
import blog.art.chess.vignette.Moves.Move;
import blog.art.chess.vignette.Moves.NullMove;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;

class Solver {

  private static final Logger LOGGER = Logger.getLogger(Solver.class.getName());

  sealed interface Stipulation {

  }

  record Perft(int nPlies) implements Stipulation {

  }

  record MateSearch(int nMoves) implements Stipulation {

  }

  static class Problem {

    private final Position position;
    private final Stipulation stipulation;

    Problem(Position position, Stipulation stipulation) {
      this.position = position;
      this.stipulation = stipulation;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Problem.class.getSimpleName() + "[", "]").add(
          "position=" + position).add("stipulation=" + stipulation).toString();
    }
  }

  private record MateNode(Move move, int distance) {

  }

  static void solve(Problem problem) {
    IO.println("_".repeat(42));
    IO.println(Engine.toFormatted(problem.position, switch (problem.stipulation) {
      case Perft(int nPlies) -> "Perft at depth %d".formatted(nPlies);
      case MateSearch(int nMoves) -> "Mate in %d".formatted(nMoves);
    }));
    IO.println();
    LOGGER.info("Solving...");
    long begin = System.currentTimeMillis();
    List<Move> pseudoLegalMoves = new ArrayList<>();
    if (Engine.isPositionLegal(problem.position, pseudoLegalMoves)) {
      switch (problem.stipulation) {
        case Perft(int nPlies) -> {
          long nNodes = count(nPlies, problem.position, pseudoLegalMoves);
          IO.println(nNodes);
        }
        case MateSearch(int nMoves) -> {
          List<MateNode> nodes = new ArrayList<>();
          for (Move move : pseudoLegalMoves) {
            Position positionMin = new Position(problem.position);
            List<Move> pseudoLegalMovesMin = new ArrayList<>();
            if (Engine.makeMove(move, positionMin, pseudoLegalMovesMin, null)) {
              for (int depth = 1; depth <= nMoves; depth++) {
                if (searchMin(depth, positionMin, pseudoLegalMovesMin)) {
                  nodes.add(new MateNode(move, depth));
                  break;
                }
              }
            }
          }
          nodes.sort(Comparator.comparingInt(MateNode::distance));
          List<String> lines = new ArrayList<>();
          for (MateNode node : nodes) {
            StringBuilder lanBuilder = new StringBuilder();
            Engine.makeMove(node.move(), new Position(problem.position), null, lanBuilder);
            lines.add("%s [#%d]".formatted(lanBuilder.toString(), node.distance()));
          }
          IO.println(String.join(System.lineSeparator(), lines));
        }
      }
    } else {
      IO.println("Illegal position");
    }
    long end = System.currentTimeMillis();
    LOGGER.info("Finished solving in %dms.".formatted(end - begin));
  }

  private static long count(int nPlies, Position position, List<Move> pseudoLegalMoves) {
    if (nPlies == 0) {
      return 1;
    }
    long nNodes = 0;
    for (Move move : pseudoLegalMoves) {
      Position positionNext = new Position(position);
      List<Move> pseudoLegalMovesNext = new ArrayList<>();
      if (Engine.makeMove(move, positionNext, pseudoLegalMovesNext, null)) {
        nNodes += count(nPlies - 1, positionNext, pseudoLegalMovesNext);
      }
    }
    return nNodes;
  }

  private static boolean searchMax(int nMoves, Position positionMax,
      List<Move> pseudoLegalMovesMax) {
    for (Move moveMax : pseudoLegalMovesMax) {
      Position positionMin = new Position(positionMax);
      List<Move> pseudoLegalMovesMin = new ArrayList<>();
      if (Engine.makeMove(moveMax, positionMin, pseudoLegalMovesMin, null)) {
        if (searchMin(nMoves, positionMin, pseudoLegalMovesMin)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean searchMin(int nMoves, Position positionMin,
      List<Move> pseudoLegalMovesMin) {
    boolean terminal = true;
    if (nMoves == 1) {
      for (Move moveMin : pseudoLegalMovesMin) {
        if (Engine.makeMove(moveMin, new Position(positionMin), null, null)) {
          return false;
        }
      }
    } else {
      for (Move moveMin : pseudoLegalMovesMin) {
        Position positionMax = new Position(positionMin);
        List<Move> pseudoLegalMovesMax = new ArrayList<>();
        if (Engine.makeMove(moveMin, positionMax, pseudoLegalMovesMax, null)) {
          if (!searchMax(nMoves - 1, positionMax, pseudoLegalMovesMax)) {
            return false;
          }
          terminal = false;
        }
      }
    }
    if (terminal) {
      return !Engine.makeMove(new NullMove(), new Position(positionMin), null, null);
    }
    return true;
  }
}
