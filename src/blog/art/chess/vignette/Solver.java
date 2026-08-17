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
import blog.art.chess.vignette.Nodes.DivideLeaf;
import blog.art.chess.vignette.Nodes.DivideRoot;
import blog.art.chess.vignette.Nodes.IllegalNode;
import blog.art.chess.vignette.Nodes.MateBranch;
import blog.art.chess.vignette.Nodes.MateLeaf;
import blog.art.chess.vignette.Nodes.MateRoot;
import blog.art.chess.vignette.Nodes.Node;
import blog.art.chess.vignette.Nodes.PerftNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

class Solver {

  private static final Logger LOGGER = Logger.getLogger(Solver.class.getName());

  sealed interface Problem {

    Position position();
  }

  record Perft(Position position, int nPlies) implements Problem {

  }

  record MateSearch(Position position, int nMoves) implements Problem {

  }

  static void solve(Problem problem, boolean detailed, boolean verbose) {
    IO.println("_".repeat(42));
    IO.println(Parser.formatToString(problem));
    IO.println();
    LOGGER.info(detailed ? "Solving with analysis..." : "Solving...");
    long begin = System.currentTimeMillis();
    Node solution;
    List<Move> pseudoLegalMoves = new ArrayList<>();
    if (Engine.isLegal(problem.position(), pseudoLegalMoves)) {
      switch (problem) {
        case Perft(Position position, int nPlies) -> {
          List<Node> nodes = detailed ? new ArrayList<>() : null;
          long nNodes = count(position, nPlies, pseudoLegalMoves, nodes, verbose);
          solution = detailed ? new DivideRoot(nNodes, nodes) : new PerftNode(nNodes);
        }
        case MateSearch(Position position, int nMoves) -> {
          List<Node> nodes = analyse(position, nMoves, pseudoLegalMoves, detailed, verbose);
          solution = new MateRoot(nodes);
        }
      }
    } else {
      solution = new IllegalNode();
    }
    IO.println(Nodes.formatToString(solution, problem.position(), 1, false));
    long end = System.currentTimeMillis();
    LOGGER.info("Finished solving in %dms.".formatted(end - begin));
  }

  private static long count(Position position, int nPlies, List<Move> pseudoLegalMoves,
      List<Node> nodes, boolean verbose) {
    if (nPlies == 0) {
      return 1;
    }
    long nNodes = 0;
    for (Move move : pseudoLegalMoves) {
      List<Move> pseudoLegalMovesNext = new ArrayList<>();
      StringBuilder lanBuilder = verbose ? new StringBuilder() : null;
      Optional<Position> positionNext = Engine.makeMove(position, move, pseudoLegalMovesNext,
          lanBuilder);
      if (positionNext.isPresent()) {
        long nChildNodes = count(positionNext.get(), nPlies - 1, pseudoLegalMovesNext, null, false);
        if (nodes != null) {
          nodes.add(new DivideLeaf(move, nChildNodes));
        }
        nNodes += nChildNodes;
        if (verbose) {
          LOGGER.fine(
              "Evaluated '%s'. Counted %d nodes at depth %d.".formatted(lanBuilder, nChildNodes,
                  nPlies));
        }
      }
    }
    if (verbose) {
      LOGGER.fine("Finished counting. %d nodes at depth %d.".formatted(nNodes, nPlies));
    }
    return nNodes;
  }

  private static List<Node> analyse(Position position, int nMoves, List<Move> pseudoLegalMoves,
      boolean detailed, boolean verbose) {
    List<Node> nodes = new ArrayList<>();
    if (detailed) {
      for (Move moveMax : pseudoLegalMoves) {
        List<Move> pseudoLegalMovesMin = new ArrayList<>();
        StringBuilder lanBuilder = verbose ? new StringBuilder() : null;
        Optional<Position> positionMin = Engine.makeMove(position, moveMax, pseudoLegalMovesMin,
            lanBuilder);
        if (positionMin.isPresent()) {
          int min = searchMin(positionMin.get(), nMoves, pseudoLegalMovesMin, true);
          if (min > 0) {
            int distanceMax = nMoves - min + 1;
            if (verbose) {
              LOGGER.fine("Tried '%s'. Found mate in %d.".formatted(lanBuilder, distanceMax));
            }
            List<Node> nodesMin = new ArrayList<>();
            for (Move moveMin : pseudoLegalMovesMin) {
              List<Move> pseudoLegalMovesMax = new ArrayList<>();
              Optional<Position> positionMax = Engine.makeMove(positionMin.get(), moveMin,
                  pseudoLegalMovesMax, null);
              if (positionMax.isPresent()) {
                int max = searchMax(positionMax.get(), distanceMax - 1, pseudoLegalMovesMax, true);
                int distanceMin = distanceMax - max;
                List<Node> nodesMax = analyse(positionMax.get(), distanceMin, pseudoLegalMovesMax,
                    true, false);
                nodesMin.add(new MateBranch(moveMin, distanceMin, nodesMax));
              }
            }
            nodesMin.sort(
                Comparator.comparingInt(node -> ((MateBranch) node).distance()).reversed());
            nodes.add(new MateBranch(moveMax, distanceMax, nodesMin));
            if (verbose) {
              LOGGER.fine("Finished analysis of '%s'.".formatted(lanBuilder));
            }
          } else {
            if (verbose) {
              LOGGER.fine("Tried '%s'. No mate in %d.".formatted(lanBuilder, nMoves));
            }
          }
        }
      }
      nodes.sort(Comparator.comparingInt(node -> ((MateBranch) node).distance()));
    } else {
      for (Move moveMax : pseudoLegalMoves) {
        List<Move> pseudoLegalMovesMin = new ArrayList<>();
        StringBuilder lanBuilder = verbose ? new StringBuilder() : null;
        Optional<Position> positionMin = Engine.makeMove(position, moveMax, pseudoLegalMovesMin,
            lanBuilder);
        if (positionMin.isPresent()) {
          int depth = 1;
          for (; depth <= nMoves; depth++) {
            if (searchMin(positionMin.get(), depth, pseudoLegalMovesMin, false) == 1) {
              nodes.add(new MateLeaf(moveMax, depth));
              break;
            }
          }
          if (verbose) {
            LOGGER.fine(
                depth <= nMoves ? "Tried '%s'. Found mate in %d.".formatted(lanBuilder, depth)
                    : "Tried '%s'. No mate in %d.".formatted(lanBuilder, nMoves));
          }
        }
      }
      nodes.sort(Comparator.comparingInt(node -> ((MateLeaf) node).distance()));
    }
    return nodes;
  }

  private static int searchMax(Position positionMax, int nMoves, List<Move> pseudoLegalMovesMax,
      boolean detailed) {
    int max = -1;
    for (Move moveMax : pseudoLegalMovesMax) {
      List<Move> pseudoLegalMovesMin = new ArrayList<>();
      Optional<Position> positionMin = Engine.makeMove(positionMax, moveMax, pseudoLegalMovesMin,
          null);
      if (positionMin.isPresent()) {
        int min = searchMin(positionMin.get(), nMoves, pseudoLegalMovesMin, detailed);
        if (min > max) {
          max = min;
          if (max == (detailed ? nMoves : 1)) {
            break;
          }
        }
      }
    }
    return max;
  }

  private static int searchMin(Position positionMin, int nMoves, List<Move> pseudoLegalMovesMin,
      boolean detailed) {
    int min = 0;
    if (nMoves == 1) {
      for (Move moveMin : pseudoLegalMovesMin) {
        if (Engine.makeMove(positionMin, moveMin, null, null).isPresent()) {
          min = -1;
          break;
        }
      }
    } else {
      for (Move moveMin : pseudoLegalMovesMin) {
        List<Move> pseudoLegalMovesMax = new ArrayList<>();
        Optional<Position> positionMax = Engine.makeMove(positionMin, moveMin, pseudoLegalMovesMax,
            null);
        if (positionMax.isPresent()) {
          int max = searchMax(positionMax.get(), nMoves - 1, pseudoLegalMovesMax, detailed);
          if (min == 0 || max < min) {
            min = max;
            if (min == -1) {
              break;
            }
          }
        }
      }
    }
    if (min == 0) {
      min = Engine.makeMove(positionMin, new NullMove(), null, null).isPresent() ? -1
          : detailed ? nMoves : 1;
    }
    return min;
  }
}
