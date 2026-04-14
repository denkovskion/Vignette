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
import blog.art.chess.vignette.Pieces.Colour;
import blog.art.chess.vignette.Pieces.King;
import blog.art.chess.vignette.Pieces.Knight;
import blog.art.chess.vignette.Pieces.Pawn;
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
                  case "K" -> board.set(square, new King(Colour.WHITE));
                  case "Q" -> board.set(square, new Queen(Colour.WHITE));
                  case "R" -> board.set(square, new Rook(Colour.WHITE));
                  case "B" -> board.set(square, new Bishop(Colour.WHITE));
                  case "N" -> board.set(square, new Knight(Colour.WHITE));
                  case "P" -> board.set(square, new Pawn(Colour.WHITE));
                  case "k" -> board.set(square, new King(Colour.BLACK));
                  case "q" -> board.set(square, new Queen(Colour.BLACK));
                  case "r" -> board.set(square, new Rook(Colour.BLACK));
                  case "b" -> board.set(square, new Bishop(Colour.BLACK));
                  case "n" -> board.set(square, new Knight(Colour.BLACK));
                  case "p" -> board.set(square, new Pawn(Colour.BLACK));
                }
              }
              characters.skip(rank > 1 ? "/" : "$");
            }
          }
          Colour sideToMove = null;
          switch (fields.next("[wb]")) {
            case "w" -> sideToMove = Colour.WHITE;
            case "b" -> sideToMove = Colour.BLACK;
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
                  new Problem(new Position(board, sideToMove, castlingOrigins, enPassantTarget),
                      new Perft(nPlies)));
            }
            case "dm" -> {
              fields.next("([1-9]\\d*);");
              int nMoves = Integer.parseInt(fields.match().group(1));
              fields.skip("\\s*$");
              problems.add(
                  new Problem(new Position(board, sideToMove, castlingOrigins, enPassantTarget),
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
}
