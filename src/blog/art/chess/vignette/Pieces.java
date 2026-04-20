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
import blog.art.chess.vignette.Moves.Promotion;
import blog.art.chess.vignette.Moves.PromotionCapture;
import blog.art.chess.vignette.Moves.QuietMove;
import blog.art.chess.vignette.Moves.ShortCastling;
import java.util.List;
import java.util.Set;

class Pieces {

  sealed interface Unit {

  }

  enum Square implements Unit {EMPTY, BORDER}

  sealed interface Piece extends Unit {

    boolean black();
  }

  sealed interface Category extends Piece {

  }

  record King(boolean black) implements Category {

  }

  record Queen(boolean black) implements Category {

  }

  record Rook(boolean black) implements Category {

  }

  record Bishop(boolean black) implements Category {

  }

  record Knight(boolean black) implements Category {

  }

  record Pawn(boolean black) implements Piece {

  }

  static int isLegal(List<Unit> board, boolean blackToMove, Set<Integer> castlingOrigins,
      Integer enPassantTarget, List<Move> moves, boolean count) {
    int nChecks = 0;
    for (int origin = 0; origin < 120; origin++) {
      if (board.get(origin) instanceof Piece piece && piece.black() == blackToMove) {
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
                  if (other instanceof Piece captured && captured.black() != category.black()) {
                    if (captured instanceof King) {
                      if (count) {
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
            int[] captureDirections = pawn.black() ? new int[]{-11, 9} : new int[]{-9, 11};
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
              } else if (other instanceof Piece captured && captured.black() != pawn.black()) {
                if (captured instanceof King) {
                  if (count) {
                    nChecks++;
                  } else {
                    return 0;
                  }
                }
                if (origin % 10 == (pawn.black() ? 2 : 7)) {
                  List<Piece> box = List.of(new Queen(pawn.black()), new Rook(pawn.black()),
                      new Bishop(pawn.black()), new Knight(pawn.black()));
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
            int direction = pawn.black() ? -1 : 1;
            int target = origin + direction;
            if (board.get(target).equals(Square.EMPTY)) {
              if (origin % 10 == (pawn.black() ? 2 : 7)) {
                List<Piece> box = List.of(new Queen(pawn.black()), new Rook(pawn.black()),
                    new Bishop(pawn.black()), new Knight(pawn.black()));
                for (Piece promoted : box) {
                  if (moves != null) {
                    moves.add(new Promotion(origin, target, promoted));
                  }
                }
              } else {
                if (moves != null) {
                  moves.add(new QuietMove(origin, target));
                }
                if (origin % 10 == (pawn.black() ? 7 : 2)) {
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

  static String toLanCode(Piece piece) {
    return switch (piece) {
      case King _ -> "K";
      case Queen _ -> "Q";
      case Rook _ -> "R";
      case Bishop _ -> "B";
      case Knight _ -> "N";
      case Pawn _ -> "";
    };
  }

  static String toLanCode(int square) {
    return new String(new char[]{(char) ('a' + square / 10 - 2), (char) ('1' + square % 10 - 1)});
  }

  static void validate(List<Unit> board, boolean blackToMove, Set<Integer> castlingOrigins,
      Integer enPassantTarget) {
    for (boolean value : new boolean[]{false, true}) {
      int frequency = 0;
      for (Unit unit : board) {
        if (unit instanceof King(boolean black) && black == value) {
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
          || (file == 1 || file == 8) && piece instanceof Rook) && (rank == 1 && !piece.black()
          || rank == 8 && piece.black()))) {
        throw new IllegalArgumentException("Not accepted castling rights");
      }
    }
    if (enPassantTarget != null) {
      int doubleStepOrigin = enPassantTarget + (blackToMove ? -1 : 1);
      int doubleStepTarget = enPassantTarget + (blackToMove ? 1 : -1);
      int doubleStepStop = enPassantTarget;
      if (!(doubleStepStop % 10 == (blackToMove ? 3 : 6)
          && board.get(doubleStepOrigin) == Square.EMPTY
          && board.get(doubleStepStop) == Square.EMPTY && board.get(
          doubleStepTarget) instanceof Pawn(boolean black) && black != blackToMove)) {
        throw new IllegalArgumentException("Not accepted en passant square");
      }
    }
  }
}
