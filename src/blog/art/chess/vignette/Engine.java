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
import blog.art.chess.vignette.Pieces.Piece;
import blog.art.chess.vignette.Pieces.Square;
import blog.art.chess.vignette.Pieces.Unit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

class Engine {

  static class Position {

    private final List<Unit> board;
    private boolean blackToMove;
    private final Set<Integer> castlingOrigins;
    private Integer enPassantTarget;

    Position(List<Unit> board, boolean blackToMove, Set<Integer> castlingOrigins,
        Integer enPassantTarget) {
      Pieces.validate(board, blackToMove, castlingOrigins, enPassantTarget);
      this.board = new ArrayList<>(board);
      this.blackToMove = blackToMove;
      this.castlingOrigins = new HashSet<>(castlingOrigins);
      this.enPassantTarget = enPassantTarget;
    }

    private Position(Position other) {
      this.board = new ArrayList<>(other.board);
      this.blackToMove = other.blackToMove;
      this.castlingOrigins = new HashSet<>(other.castlingOrigins);
      this.enPassantTarget = other.enPassantTarget;
    }

    List<Unit> board() {
      return board;
    }

    boolean blackToMove() {
      return blackToMove;
    }

    Set<Integer> castlingOrigins() {
      return castlingOrigins;
    }

    Integer enPassantTarget() {
      return enPassantTarget;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", Position.class.getSimpleName() + "[", "]").add("board=" + board)
          .add("blackToMove=" + blackToMove).add("castlingOrigins=" + castlingOrigins)
          .add("enPassantTarget=" + enPassantTarget).toString();
    }
  }

  static boolean isLegal(Position position, List<Move> pseudoLegalMoves) {
    return Pieces.isLegal(position.board, position.blackToMove, position.castlingOrigins,
        position.enPassantTarget, pseudoLegalMoves, false) == 1;
  }

  static Optional<Position> makeMove(Position position, Move move, List<Move> pseudoLegalMoves,
      StringBuilder lanBuilder) {
    if (lanBuilder != null) {
      switch (move) {
        case NullMove() -> lanBuilder.append((String) null);
        case QuietMove(int origin, int target) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(Pieces.toLanCode(origin)).append("-").append(Pieces.toLanCode(target));
        case Capture(int origin, int target) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(Pieces.toLanCode(origin)).append("x").append(Pieces.toLanCode(target));
        case LongCastling(_, _, _, _) -> lanBuilder.append("0-0-0");
        case ShortCastling(_, _, _, _) -> lanBuilder.append("0-0");
        case DoubleStep(int origin, int target, _) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(Pieces.toLanCode(origin)).append("-").append(Pieces.toLanCode(target));
        case EnPassant(int origin, int target, _) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(Pieces.toLanCode(origin)).append("x").append(Pieces.toLanCode(target))
                .append(" e.p.");
        case Promotion(int origin, int target, Piece promoted) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(Pieces.toLanCode(origin)).append("-").append(Pieces.toLanCode(target))
                .append("=").append(Pieces.toLanCode(promoted));
        case PromotionCapture(int origin, int target, Piece promoted) ->
            lanBuilder.append(Pieces.toLanCode((Piece) position.board.get(origin)))
                .append(Pieces.toLanCode(origin)).append("x").append(Pieces.toLanCode(target))
                .append("=").append(Pieces.toLanCode(promoted));
      }
    }
    if (switch (move) {
      case NullMove(), QuietMove(_, _), Capture(_, _) -> true;
      case LongCastling(int origin, _, _, int target2) -> {
        if (makeMove(position, new NullMove(), null, null).isPresent()) {
          if (makeMove(position, new QuietMove(origin, target2), null, null).isPresent()) {
            yield true;
          }
        }
        yield false;
      }
      case ShortCastling(int origin, _, _, int target2) -> {
        if (makeMove(position, new NullMove(), null, null).isPresent()) {
          if (makeMove(position, new QuietMove(origin, target2), null, null).isPresent()) {
            yield true;
          }
        }
        yield false;
      }
      case DoubleStep(_, _, _), EnPassant(_, _, _), Promotion(_, _, _), PromotionCapture(_, _, _) ->
          true;
    }) {
      Position result = doMakeMove(position, move);
      if (isLegal(result, pseudoLegalMoves)) {
        if (lanBuilder != null) {
          List<Move> pseudoLegalMovesNext = pseudoLegalMoves;
          if (pseudoLegalMovesNext == null) {
            pseudoLegalMovesNext = new ArrayList<>();
            Pieces.isLegal(result.board, result.blackToMove, result.castlingOrigins,
                result.enPassantTarget, pseudoLegalMovesNext, true);
          }
          boolean terminal = true;
          for (Move moveNext : pseudoLegalMovesNext) {
            if (makeMove(result, moveNext, null, null).isPresent()) {
              terminal = false;
              break;
            }
          }
          Position opposite = doMakeMove(result, new NullMove());
          int legal = Pieces.isLegal(opposite.board, opposite.blackToMove, opposite.castlingOrigins,
              opposite.enPassantTarget, null, true);
          if (terminal) {
            if (legal == 1) {
              lanBuilder.append("=");
            } else {
              if (legal < -1) {
                lanBuilder.repeat("+", -legal);
              }
              lanBuilder.append("#");
            }
          } else {
            if (legal < 0) {
              lanBuilder.repeat("+", -legal);
            }
          }
        }
        return Optional.of(result);
      }
    }
    return Optional.empty();
  }

  private static Position doMakeMove(Position position, Move move) {
    Position result = new Position(position);
    switch (move) {
      case NullMove() -> result.enPassantTarget = null;
      case QuietMove(int origin, int target) -> {
        result.board.set(target, result.board.set(origin, Square.EMPTY));
        result.castlingOrigins.remove(origin);
        result.enPassantTarget = null;
      }
      case Capture(int origin, int target) -> {
        result.board.set(target, result.board.set(origin, Square.EMPTY));
        result.castlingOrigins.remove(origin);
        result.castlingOrigins.remove(target);
        result.enPassantTarget = null;
      }
      case LongCastling(int origin, int target, int origin2, int target2) -> {
        result.board.set(target, result.board.set(origin, Square.EMPTY));
        result.board.set(target2, result.board.set(origin2, Square.EMPTY));
        result.castlingOrigins.remove(origin);
        result.castlingOrigins.remove(origin2);
        result.enPassantTarget = null;
      }
      case ShortCastling(int origin, int target, int origin2, int target2) -> {
        result.board.set(target, result.board.set(origin, Square.EMPTY));
        result.board.set(target2, result.board.set(origin2, Square.EMPTY));
        result.castlingOrigins.remove(origin);
        result.castlingOrigins.remove(origin2);
        result.enPassantTarget = null;
      }
      case DoubleStep(int origin, int target, int stop) -> {
        result.board.set(target, result.board.set(origin, Square.EMPTY));
        result.enPassantTarget = stop;
      }
      case EnPassant(int origin, int target, int stop) -> {
        result.board.set(stop, Square.EMPTY);
        result.board.set(target, result.board.set(origin, Square.EMPTY));
        result.enPassantTarget = null;
      }
      case Promotion(int origin, int target, Piece promoted) -> {
        result.board.set(origin, Square.EMPTY);
        result.board.set(target, promoted);
        result.enPassantTarget = null;
      }
      case PromotionCapture(int origin, int target, Piece promoted) -> {
        result.board.set(origin, Square.EMPTY);
        result.board.set(target, promoted);
        result.castlingOrigins.remove(target);
        result.enPassantTarget = null;
      }
    }
    result.blackToMove = !result.blackToMove;
    return result;
  }
}
