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
import blog.art.chess.vignette.Pieces.Bishop;
import blog.art.chess.vignette.Pieces.King;
import blog.art.chess.vignette.Pieces.Knight;
import blog.art.chess.vignette.Pieces.Pawn;
import blog.art.chess.vignette.Pieces.Piece;
import blog.art.chess.vignette.Pieces.Queen;
import blog.art.chess.vignette.Pieces.Rook;
import blog.art.chess.vignette.Pieces.Square;
import blog.art.chess.vignette.Pieces.Unit;
import blog.art.chess.vignette.Solver.MateSearch;
import blog.art.chess.vignette.Solver.Perft;
import blog.art.chess.vignette.Solver.Problem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.MatchResult;

class Parser {

  private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());

  static List<Problem> readAllProblems() {
    List<Problem> problems = new ArrayList<>();
    for (String line; (line = IO.readln()) != null; ) {
      if (!line.isBlank()) {
        try (Scanner fields = new Scanner(line)) {
          List<Unit> board = new ArrayList<>();
          for (int square = 0; square < 120; square++) {
            int file = square / 10 - 1;
            int rank = square % 10;
            board.add(
                file >= 1 && file <= 8 && rank >= 1 && rank <= 8 ? Square.EMPTY : Square.BORDER);
          }
          try (Scanner characters = new Scanner(fields.next())) {
            characters.useDelimiter("");
            for (int rank = 8; rank >= 1; rank--) {
              for (int file = 1; file <= 8; file++) {
                if (characters.hasNext("[" + "12345678".substring(0, 8 - (file - 1)) + "]")) {
                  file += characters.nextInt();
                  if (file > 8) {
                    break;
                  }
                }
                String letter = characters.next("[KQRBNPkqrbnp]");
                int square = (file + 1) * 10 + rank;
                switch (letter) {
                  case "K" -> board.set(square, new King(false));
                  case "Q" -> board.set(square, new Queen(false));
                  case "R" -> board.set(square, new Rook(false));
                  case "B" -> board.set(square, new Bishop(false));
                  case "N" -> board.set(square, new Knight(false));
                  case "P" -> board.set(square, new Pawn(false));
                  case "k" -> board.set(square, new King(true));
                  case "q" -> board.set(square, new Queen(true));
                  case "r" -> board.set(square, new Rook(true));
                  case "b" -> board.set(square, new Bishop(true));
                  case "n" -> board.set(square, new Knight(true));
                  case "p" -> board.set(square, new Pawn(true));
                }
              }
              characters.skip(rank > 1 ? "/" : "$");
            }
          }
          boolean blackToMove = false;
          if (fields.hasNext("w")) {
            fields.next();
          } else {
            fields.next("b");
            blackToMove = true;
          }
          Set<Integer> castlingOrigins = new HashSet<>();
          if (fields.hasNext("-")) {
            fields.next();
          } else {
            for (String letter : fields.next("\\bK?Q?k?q?").split("")) {
              switch (letter) {
                case "K", "Q" -> castlingOrigins.add(61);
                case "k", "q" -> castlingOrigins.add(68);
              }
              switch (letter) {
                case "K" -> castlingOrigins.add(91);
                case "Q" -> castlingOrigins.add(21);
                case "k" -> castlingOrigins.add(98);
                case "q" -> castlingOrigins.add(28);
              }
            }
          }
          Integer enPassantTarget = null;
          if (fields.hasNext("-")) {
            fields.next();
          } else {
            fields.next("([a-h])([36])");
            MatchResult result = fields.match();
            int file = 1 + result.group(1).charAt(0) - 'a';
            int rank = 1 + result.group(2).charAt(0) - '1';
            enPassantTarget = (file + 1) * 10 + rank;
          }
          switch (fields.next("acd|dm")) {
            case "acd" -> {
              fields.next("(0|[1-9]\\d*);");
              int nPlies = Integer.parseInt(fields.match().group(1));
              fields.skip("\\s*$");
              problems.add(
                  new Problem(new Position(board, blackToMove, castlingOrigins, enPassantTarget),
                      new Perft(nPlies)));
            }
            case "dm" -> {
              fields.next("([1-9]\\d*);");
              int nMoves = Integer.parseInt(fields.match().group(1));
              fields.skip("\\s*$");
              problems.add(
                  new Problem(new Position(board, blackToMove, castlingOrigins, enPassantTarget),
                      new MateSearch(nMoves)));
            }
          }
        } catch (IllegalArgumentException ex) {
          LOGGER.warning("Not accepted line: '%s'. %s.".formatted(line, ex.getMessage()));
          return List.of();
        } catch (NoSuchElementException _) {
          LOGGER.warning("Invalid line: '%s'.".formatted(line));
          return List.of();
        }
      }
    }
    return problems;
  }

  static void write(Problem problem) {
    IO.println("_".repeat(42));
    List<String> args = new ArrayList<>();
    for (int rank = 8; rank >= 1; rank--) {
      for (int file = 1; file <= 8; file++) {
        Unit other = problem.position().board().get((file + 1) * 10 + rank);
        if (other.equals(Square.EMPTY)) {
          args.add(".");
        } else if (other instanceof Piece piece) {
          String code = switch (piece) {
            case King _ -> "K";
            case Queen _ -> "Q";
            case Rook _ -> "R";
            case Bishop _ -> "B";
            case Knight _ -> "N";
            case Pawn _ -> "P";
          };
          args.add(piece.black() ? code.toLowerCase() : code.toUpperCase());
        }
      }
      switch (rank) {
        case 8 -> args.add(problem.position().blackToMove() ? "b" : "w");
        case 7 -> {
          if (!problem.position().castlingOrigins().isEmpty()) {
            StringBuilder arg = new StringBuilder();
            if (problem.position().castlingOrigins().contains(61)) {
              if (problem.position().castlingOrigins().contains(91)) {
                arg.append("K");
              }
              if (problem.position().castlingOrigins().contains(21)) {
                arg.append("Q");
              }
            }
            if (problem.position().castlingOrigins().contains(68)) {
              if (problem.position().castlingOrigins().contains(98)) {
                arg.append("k");
              }
              if (problem.position().castlingOrigins().contains(28)) {
                arg.append("q");
              }
            }
            args.add(arg.toString());
          } else {
            args.add("-");
          }
        }
        case 6 -> {
          if (problem.position().enPassantTarget() != null) {
            args.add(new String(
                new char[]{(char) ('a' + problem.position().enPassantTarget() / 10 - 2),
                    (char) ('1' + problem.position().enPassantTarget() % 10 - 1)}));
          } else {
            args.add("-");
          }
        }
        case 4 -> args.add(switch (problem.stipulation()) {
          case Perft(int nPlies) -> "Perft at depth %d".formatted(nPlies);
          case MateSearch(int nMoves) -> "Mate in %d".formatted(nMoves);
        });
      }
    }
    IO.println(("8 %s %s %s %s %s %s %s %s    Side to move: %s%n"
        + "7 %s %s %s %s %s %s %s %s    Castling rights: %s%n"
        + "6 %s %s %s %s %s %s %s %s    En passant target: %s%n" + "5 %s %s %s %s %s %s %s %s%n"
        + "4 %s %s %s %s %s %s %s %s    %s%n" + "3 %s %s %s %s %s %s %s %s%n"
        + "2 %s %s %s %s %s %s %s %s%n" + "1 %s %s %s %s %s %s %s %s%n"
        + "  a b c d e f g h").formatted(args.toArray()));
    IO.println();
  }
}
