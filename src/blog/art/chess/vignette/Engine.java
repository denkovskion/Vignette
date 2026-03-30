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

import blog.art.chess.vignette.Moves.Capture;
import blog.art.chess.vignette.Moves.DoubleStep;
import blog.art.chess.vignette.Moves.EnPassant;
import blog.art.chess.vignette.Moves.LongCastling;
import blog.art.chess.vignette.Moves.Move;
import blog.art.chess.vignette.Moves.NullMove;
import blog.art.chess.vignette.Moves.Promotion;
import blog.art.chess.vignette.Moves.PromotionCapture;
import blog.art.chess.vignette.Moves.QuietMove;
import blog.art.chess.vignette.Moves.ShortCastling;
import blog.art.chess.vignette.Pieces.Colour;
import blog.art.chess.vignette.Pieces.Piece;
import blog.art.chess.vignette.Pieces.Unit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

class Engine {

  static class Position {

    private final List<Unit> board;
    private Colour sideToMove;
    private final Set<Integer> castlingOrigins;
    private Integer enPassantTarget;

    Position(List<Unit> board, Colour sideToMove, Set<Integer> castlingOrigins,
        Integer enPassantTarget) {
      Pieces.validate(board, sideToMove, castlingOrigins, enPassantTarget);
      this.board = new ArrayList<>(board);
      this.sideToMove = sideToMove;
      this.castlingOrigins = new HashSet<>(castlingOrigins);
      this.enPassantTarget = enPassantTarget;
    }

    Position(Position other) {
      this.board = new ArrayList<>(other.board);
      this.sideToMove = other.sideToMove;
      this.castlingOrigins = new HashSet<>(other.castlingOrigins);
      this.enPassantTarget = other.enPassantTarget;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Position.class.getSimpleName() + "[", "]").add("board=" + board)
          .add("sideToMove=" + sideToMove).add("castlingOrigins=" + castlingOrigins)
          .add("enPassantTarget=" + enPassantTarget).toString();
    }
  }

  static boolean isPositionLegal(Position position, List<Move> pseudoLegalMoves) {
    for (int origin = 0; origin < 120; origin++) {
      if (position.board.get(origin) instanceof Piece piece) {
        if (piece.colour() == position.sideToMove) {
          if (!Pieces.generateMoves(piece, origin, position.board, position.castlingOrigins,
              position.enPassantTarget, pseudoLegalMoves)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  static boolean makeMove(Move move, Position position, List<Move> pseudoLegalMoves,
      StringBuilder lanBuilder) {
    if (lanBuilder != null) {
      switch (move) {
        case NullMove() -> lanBuilder.append((String) null);
        case QuietMove(int origin, int target) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(toLanCode(origin)).append("-").append(toLanCode(target));
        case Capture(int origin, int target) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(toLanCode(origin)).append("x").append(toLanCode(target));
        case LongCastling(_, _, _, _) -> lanBuilder.append("0-0-0");
        case ShortCastling(_, _, _, _) -> lanBuilder.append("0-0");
        case DoubleStep(int origin, int target, _) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(toLanCode(origin)).append("-").append(toLanCode(target));
        case EnPassant(int origin, int target, _) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(toLanCode(origin)).append("x").append(toLanCode(target)).append(" e.p.");
        case Promotion(int origin, int target, Piece promoted) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(toLanCode(origin)).append("-").append(toLanCode(target)).append("=")
                .append(Pieces.toLanCode(promoted));
        case PromotionCapture(int origin, int target, Piece promoted) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(toLanCode(origin)).append("x").append(toLanCode(target)).append("=")
                .append(Pieces.toLanCode(promoted));
      }
    }
    if (switch (move) {
      case NullMove(), QuietMove(_, _), Capture(_, _) -> true;
      case LongCastling(int origin, _, _, int target2) -> {
        if (makeMove(new NullMove(), new Position(position), null, null)) {
          if (makeMove(new QuietMove(origin, target2), new Position(position), null, null)) {
            yield true;
          }
        }
        yield false;
      }
      case ShortCastling(int origin, _, _, int target2) -> {
        if (makeMove(new NullMove(), new Position(position), null, null)) {
          if (makeMove(new QuietMove(origin, target2), new Position(position), null, null)) {
            yield true;
          }
        }
        yield false;
      }
      case DoubleStep(_, _, _), EnPassant(_, _, _), Promotion(_, _, _), PromotionCapture(_, _, _) ->
          true;
    }) {
      doMakeMove(move, position);
      if (isPositionLegal(position, pseudoLegalMoves)) {
        if (lanBuilder != null) {
          List<Move> pseudoLegalMovesNext = pseudoLegalMoves;
          if (pseudoLegalMovesNext == null) {
            pseudoLegalMovesNext = new ArrayList<>();
            for (int origin = 0; origin < 120; origin++) {
              if (position.board.get(origin) instanceof Piece piece) {
                if (piece.colour() == position.sideToMove) {
                  Pieces.generateMoves(piece, origin, position.board, position.castlingOrigins,
                      position.enPassantTarget, pseudoLegalMovesNext);
                }
              }
            }
          }
          boolean terminal = true;
          for (Move moveNext : pseudoLegalMovesNext) {
            if (makeMove(moveNext, new Position(position), null, null)) {
              terminal = false;
              break;
            }
          }
          int nChecks = 0;
          Position opposite = new Position(position);
          doMakeMove(new NullMove(), opposite);
          for (int origin = 0; origin < 120; origin++) {
            if (opposite.board.get(origin) instanceof Piece piece) {
              if (piece.colour() == opposite.sideToMove) {
                if (!Pieces.generateMoves(piece, origin, opposite.board, opposite.castlingOrigins,
                    opposite.enPassantTarget, null)) {
                  nChecks++;
                }
              }
            }
          }
          if (terminal) {
            if (nChecks > 0) {
              if (nChecks > 1) {
                lanBuilder.repeat("+", nChecks);
              }
              lanBuilder.append("#");
            } else {
              lanBuilder.append("=");
            }
          } else {
            if (nChecks > 0) {
              lanBuilder.repeat("+", nChecks);
            }
          }
        }
        return true;
      }
    }
    return false;
  }

  private static void doMakeMove(Move move, Position position) {
    switch (move) {
      case NullMove() -> position.enPassantTarget = null;
      case QuietMove(int origin, int target) -> {
        position.board.set(target, position.board.set(origin, null));
        position.castlingOrigins.remove(origin);
        position.enPassantTarget = null;
      }
      case Capture(int origin, int target) -> {
        position.board.set(target, position.board.set(origin, null));
        position.castlingOrigins.remove(origin);
        position.castlingOrigins.remove(target);
        position.enPassantTarget = null;
      }
      case LongCastling(int origin, int target, int origin2, int target2) -> {
        position.board.set(target, position.board.set(origin, null));
        position.board.set(target2, position.board.set(origin2, null));
        position.castlingOrigins.remove(origin);
        position.castlingOrigins.remove(origin2);
        position.enPassantTarget = null;
      }
      case ShortCastling(int origin, int target, int origin2, int target2) -> {
        position.board.set(target, position.board.set(origin, null));
        position.board.set(target2, position.board.set(origin2, null));
        position.castlingOrigins.remove(origin);
        position.castlingOrigins.remove(origin2);
        position.enPassantTarget = null;
      }
      case DoubleStep(int origin, int target, int stop) -> {
        position.board.set(target, position.board.set(origin, null));
        position.enPassantTarget = stop;
      }
      case EnPassant(int origin, int target, int stop) -> {
        position.board.set(stop, null);
        position.board.set(target, position.board.set(origin, null));
        position.enPassantTarget = null;
      }
      case Promotion(int origin, int target, Piece promoted) -> {
        position.board.set(origin, null);
        position.board.set(target, promoted);
        position.enPassantTarget = null;
      }
      case PromotionCapture(int origin, int target, Piece promoted) -> {
        position.board.set(origin, null);
        position.board.set(target, promoted);
        position.castlingOrigins.remove(target);
        position.enPassantTarget = null;
      }
    }
    position.sideToMove = switch (position.sideToMove) {
      case WHITE -> Colour.BLACK;
      case BLACK -> Colour.WHITE;
    };
  }

  private static String toLanCode(int square) {
    return new String(new char[]{(char) ('a' + square / 10 - 2), (char) ('1' + square % 10 - 1)});
  }

  static String toFormatted(Position position, String operation) {
    return Pieces.toFormatted(position.board, position.sideToMove, position.castlingOrigins,
        position.enPassantTarget, operation);
  }
}
