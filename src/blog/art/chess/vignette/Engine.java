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
import blog.art.chess.vignette.Pieces.Bishop;
import blog.art.chess.vignette.Pieces.Category;
import blog.art.chess.vignette.Pieces.Colour;
import blog.art.chess.vignette.Pieces.King;
import blog.art.chess.vignette.Pieces.Knight;
import blog.art.chess.vignette.Pieces.Pawn;
import blog.art.chess.vignette.Pieces.Piece;
import blog.art.chess.vignette.Pieces.Queen;
import blog.art.chess.vignette.Pieces.Rook;
import blog.art.chess.vignette.Pieces.Square;
import blog.art.chess.vignette.Pieces.Unit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class Engine {

  static boolean isLegal(Position position, List<Move> pseudoLegalMoves) {
    return generateMoves(position.board(), position.sideToMove(), position.castlingOrigins(),
        position.enPassantTarget(), pseudoLegalMoves, false) == 1;
  }

  private static int generateMoves(List<Unit> board, Colour sideToMove,
      Set<Integer> castlingOrigins, Integer enPassantTarget, List<Move> moves,
      boolean countChecks) {
    int nChecks = 0;
    for (int origin = 0; origin < 120; origin++) {
      if (board.get(origin) instanceof Piece piece && piece.colour() == sideToMove) {
        switch (piece) {
          case Category category -> {
            int[] directions = switch (category) {
              case King _, Queen _ -> new int[]{-11, -10, -9, -1, 1, 9, 10, 11};
              case Rook _ -> new int[]{-10, -1, 1, 10};
              case Bishop _ -> new int[]{-11, -9, 9, 11};
              case Knight _ -> new int[]{-21, -19, -12, -8, 8, 12, 19, 21};
            };
            for (int direction : directions) {
              for (int distance = 1; ; distance++) {
                int target = origin + distance * direction;
                Unit other = board.get(target);
                if (other.equals(Square.EMPTY)) {
                  if (moves != null) {
                    moves.add(new QuietMove(origin, target));
                  }
                  if (category instanceof King || category instanceof Knight) {
                    break;
                  }
                } else {
                  if (other instanceof Piece captured && captured.colour() != category.colour()) {
                    if (captured instanceof King) {
                      if (countChecks) {
                        nChecks++;
                      } else {
                        return 0;
                      }
                    }
                    if (moves != null) {
                      moves.add(new Capture(origin, target));
                    }
                  }
                  break;
                }
              }
            }
            if (category instanceof King) {
              if (castlingOrigins.contains(origin)) {
                int[] castlingDirections = {-10, 10};
                for (int direction : castlingDirections) {
                  int distance = 1;
                  int target2 = origin + distance * direction;
                  if (board.get(target2).equals(Square.EMPTY)) {
                    distance++;
                    int target = origin + distance * direction;
                    if (board.get(target).equals(Square.EMPTY)) {
                      distance++;
                      if (direction > 0) {
                        int origin2 = origin + distance * direction;
                        if (castlingOrigins.contains(origin2)) {
                          if (moves != null) {
                            moves.add(new ShortCastling(origin, target, origin2, target2));
                          }
                        }
                      } else {
                        int stop = origin + distance * direction;
                        if (board.get(stop).equals(Square.EMPTY)) {
                          distance++;
                          int origin2 = origin + distance * direction;
                          if (castlingOrigins.contains(origin2)) {
                            if (moves != null) {
                              moves.add(new LongCastling(origin, target, origin2, target2));
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          case Pawn pawn -> {
            int[] captureDirections = switch (pawn.colour()) {
              case WHITE -> new int[]{-9, 11};
              case BLACK -> new int[]{-11, 9};
            };
            for (int direction : captureDirections) {
              int target = origin + direction;
              Unit other = board.get(target);
              if (other.equals(Square.EMPTY)) {
                if (enPassantTarget != null) {
                  if (target == enPassantTarget) {
                    int stop = (target / 10) * 10 + origin % 10;
                    if (moves != null) {
                      moves.add(new EnPassant(origin, target, stop));
                    }
                  }
                }
              } else if (other instanceof Piece captured && captured.colour() != pawn.colour()) {
                if (captured instanceof King) {
                  if (countChecks) {
                    nChecks++;
                  } else {
                    return 0;
                  }
                }
                if (origin % 10 == switch (pawn.colour()) {
                  case WHITE -> 7;
                  case BLACK -> 2;
                }) {
                  List<Piece> box = List.of(new Queen(pawn.colour()), new Rook(pawn.colour()),
                      new Bishop(pawn.colour()), new Knight(pawn.colour()));
                  for (Piece promoted : box) {
                    if (moves != null) {
                      moves.add(new PromotionCapture(origin, target, promoted));
                    }
                  }
                } else {
                  if (moves != null) {
                    moves.add(new Capture(origin, target));
                  }
                }
              }
            }
            int direction = switch (pawn.colour()) {
              case WHITE -> 1;
              case BLACK -> -1;
            };
            int target = origin + direction;
            if (board.get(target).equals(Square.EMPTY)) {
              if (origin % 10 == switch (pawn.colour()) {
                case WHITE -> 7;
                case BLACK -> 2;
              }) {
                List<Piece> box = List.of(new Queen(pawn.colour()), new Rook(pawn.colour()),
                    new Bishop(pawn.colour()), new Knight(pawn.colour()));
                for (Piece promoted : box) {
                  if (moves != null) {
                    moves.add(new Promotion(origin, target, promoted));
                  }
                }
              } else {
                if (moves != null) {
                  moves.add(new QuietMove(origin, target));
                }
                if (origin % 10 == switch (pawn.colour()) {
                  case WHITE -> 2;
                  case BLACK -> 7;
                }) {
                  int target2 = origin + 2 * direction;
                  if (board.get(target2).equals(Square.EMPTY)) {
                    if (moves != null) {
                      moves.add(new DoubleStep(origin, target2, target));
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return nChecks == 0 ? 1 : -nChecks;
  }

  static Optional<Position> makeMove(Position position, Move move, List<Move> pseudoLegalMoves,
      StringBuilder lanBuilder) {
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
      Position result = makeMove(position, move);
      if (isLegal(result, pseudoLegalMoves)) {
        if (lanBuilder != null) {
          switch (move) {
            case NullMove() -> lanBuilder.append((String) null);
            case QuietMove(int origin, int target) ->
                lanBuilder.append(toLanCode((Piece) position.board().get(origin)))
                    .append(toLanCode(origin)).append("-").append(toLanCode(target));
            case Capture(int origin, int target) ->
                lanBuilder.append(toLanCode((Piece) position.board().get(origin)))
                    .append(toLanCode(origin)).append("x").append(toLanCode(target));
            case LongCastling(_, _, _, _) -> lanBuilder.append("0-0-0");
            case ShortCastling(_, _, _, _) -> lanBuilder.append("0-0");
            case DoubleStep(int origin, int target, _) ->
                lanBuilder.append(toLanCode((Piece) position.board().get(origin)))
                    .append(toLanCode(origin)).append("-").append(toLanCode(target));
            case EnPassant(int origin, int target, _) ->
                lanBuilder.append(toLanCode((Piece) position.board().get(origin)))
                    .append(toLanCode(origin)).append("x").append(toLanCode(target))
                    .append(" e.p.");
            case Promotion(int origin, int target, Piece promoted) ->
                lanBuilder.append(toLanCode((Piece) position.board().get(origin)))
                    .append(toLanCode(origin)).append("-").append(toLanCode(target)).append("=")
                    .append(toLanCode(promoted));
            case PromotionCapture(int origin, int target, Piece promoted) ->
                lanBuilder.append(toLanCode((Piece) position.board().get(origin)))
                    .append(toLanCode(origin)).append("x").append(toLanCode(target)).append("=")
                    .append(toLanCode(promoted));
          }
          List<Move> pseudoLegalMovesNext = pseudoLegalMoves;
          if (pseudoLegalMovesNext == null) {
            pseudoLegalMovesNext = new ArrayList<>();
            generateMoves(result.board(), result.sideToMove(), result.castlingOrigins(),
                result.enPassantTarget(), pseudoLegalMovesNext, true);
          }
          boolean terminal = true;
          for (Move moveNext : pseudoLegalMovesNext) {
            if (makeMove(result, moveNext, null, null).isPresent()) {
              terminal = false;
              break;
            }
          }
          Position opposite = makeMove(result, new NullMove());
          int legal = generateMoves(opposite.board(), opposite.sideToMove(),
              opposite.castlingOrigins(), opposite.enPassantTarget(), null, true);
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

  private static Position makeMove(Position position, Move move) {
    List<Unit> board = new ArrayList<>(position.board());
    Colour sideToMove = switch (position.sideToMove()) {
      case WHITE -> Colour.BLACK;
      case BLACK -> Colour.WHITE;
    };
    Set<Integer> castlingOrigins = new HashSet<>(position.castlingOrigins());
    Integer enPassantTarget = null;
    switch (move) {
      case NullMove() -> {
      }
      case QuietMove(int origin, int target) -> {
        board.set(target, board.set(origin, Square.EMPTY));
        castlingOrigins.remove(origin);
      }
      case Capture(int origin, int target) -> {
        board.set(target, board.set(origin, Square.EMPTY));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(target);
      }
      case LongCastling(int origin, int target, int origin2, int target2) -> {
        board.set(target, board.set(origin, Square.EMPTY));
        board.set(target2, board.set(origin2, Square.EMPTY));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
      }
      case ShortCastling(int origin, int target, int origin2, int target2) -> {
        board.set(target, board.set(origin, Square.EMPTY));
        board.set(target2, board.set(origin2, Square.EMPTY));
        castlingOrigins.remove(origin);
        castlingOrigins.remove(origin2);
      }
      case DoubleStep(int origin, int target, int stop) -> {
        board.set(target, board.set(origin, Square.EMPTY));
        enPassantTarget = stop;
      }
      case EnPassant(int origin, int target, int stop) -> {
        board.set(stop, Square.EMPTY);
        board.set(target, board.set(origin, Square.EMPTY));
      }
      case Promotion(int origin, int target, Piece promoted) -> {
        board.set(origin, Square.EMPTY);
        board.set(target, promoted);
      }
      case PromotionCapture(int origin, int target, Piece promoted) -> {
        board.set(origin, Square.EMPTY);
        board.set(target, promoted);
        castlingOrigins.remove(target);
      }
    }
    return new Position(board, sideToMove, castlingOrigins, enPassantTarget);
  }

  private static String toLanCode(Piece piece) {
    return switch (piece) {
      case King _ -> "K";
      case Queen _ -> "Q";
      case Rook _ -> "R";
      case Bishop _ -> "B";
      case Knight _ -> "N";
      case Pawn _ -> "";
    };
  }

  private static String toLanCode(int square) {
    return new String(new char[]{(char) ('a' + square / 10 - 2), (char) ('1' + square % 10 - 1)});
  }

  static Position newPosition(List<Unit> board, Colour sideToMove, Set<Integer> castlingOrigins,
      Integer enPassantTarget) {
    for (Colour value : new Colour[]{Colour.WHITE, Colour.BLACK}) {
      int frequency = 0;
      for (Unit unit : board) {
        if (unit instanceof King(Colour colour) && colour == value) {
          frequency++;
        }
      }
      if (!(frequency == 1)) {
        throw new IllegalArgumentException("Not accepted number of kings");
      }
    }
    for (int castlingOrigin : castlingOrigins) {
      int file = castlingOrigin / 10 - 1;
      int rank = castlingOrigin % 10;
      if (!(board.get(castlingOrigin) instanceof Piece piece && (file == 5 && piece instanceof King
          || (file == 1 || file == 8) && piece instanceof Rook) && (
          rank == 1 && piece.colour() == Colour.WHITE
              || rank == 8 && piece.colour() == Colour.BLACK))) {
        throw new IllegalArgumentException("Not accepted castling rights");
      }
    }
    if (enPassantTarget != null) {
      if (!(enPassantTarget % 10 == switch (sideToMove) {
        case WHITE -> 6;
        case BLACK -> 3;
      } && board.get(enPassantTarget + switch (sideToMove) {
        case WHITE -> 1;
        case BLACK -> -1;
      }).equals(Square.EMPTY) && board.get(enPassantTarget).equals(Square.EMPTY) && board.get(
          enPassantTarget + switch (sideToMove) {
            case WHITE -> -1;
            case BLACK -> 1;
          }) instanceof Pawn(Colour colour) && colour != sideToMove)) {
        throw new IllegalArgumentException("Not accepted en passant square");
      }
    }
    return new Position(new ArrayList<>(board), sideToMove, new HashSet<>(castlingOrigins),
        enPassantTarget);
  }
}
