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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class Pieces {

  sealed interface Unit {

  }

  record Border() implements Unit {

  }

  enum Colour {WHITE, BLACK}

  sealed interface Piece extends Unit {

    Colour colour();
  }

  sealed interface Leaper extends Piece {

  }

  sealed interface Rider extends Piece {

  }

  record King(Colour colour) implements Leaper {

  }

  record Queen(Colour colour) implements Rider {

  }

  record Rook(Colour colour) implements Rider {

  }

  record Bishop(Colour colour) implements Rider {

  }

  record Knight(Colour colour) implements Leaper {

  }

  record Pawn(Colour colour) implements Piece {

  }

  static boolean generateMoves(Piece piece, int origin, List<Unit> board,
      Set<Integer> castlingOrigins, Integer enPassantTarget, List<Move> moves) {
    switch (piece) {
      case Leaper leaper -> {
        int[] directions = switch (leaper) {
          case King _ -> new int[]{-11, -10, -9, -1, 1, 9, 10, 11};
          case Knight _ -> new int[]{-21, -19, -12, -8, 8, 12, 19, 21};
        };
        for (int direction : directions) {
          int target = origin + direction;
          Unit other = board.get(target);
          if (other != null) {
            if (other instanceof Piece captured) {
              if (captured.colour() != leaper.colour()) {
                if (captured instanceof King) {
                  return false;
                } else {
                  if (moves != null) {
                    moves.add(new Capture(origin, target));
                  }
                }
              }
            }
          } else {
            if (moves != null) {
              moves.add(new QuietMove(origin, target));
            }
          }
        }
        switch (leaper) {
          case King _ -> {
            if (castlingOrigins.contains(origin)) {
              int[] castlingDirections = {-10, 10};
              for (int direction : castlingDirections) {
                int distance = 1;
                int target2 = origin + distance * direction;
                if (board.get(target2) == null) {
                  distance++;
                  int target = origin + distance * direction;
                  if (board.get(target) == null) {
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
                      if (board.get(stop) == null) {
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
          case Knight _ -> {
          }
        }
      }
      case Rider rider -> {
        int[] directions = switch (rider) {
          case Queen _ -> new int[]{-11, -10, -9, -1, 1, 9, 10, 11};
          case Rook _ -> new int[]{-10, -1, 1, 10};
          case Bishop _ -> new int[]{-11, -9, 9, 11};
        };
        for (int direction : directions) {
          for (int distance = 1; ; distance++) {
            int target = origin + distance * direction;
            Unit other = board.get(target);
            if (other != null) {
              if (other instanceof Piece captured) {
                if (captured.colour() != rider.colour()) {
                  if (captured instanceof King) {
                    return false;
                  } else {
                    if (moves != null) {
                      moves.add(new Capture(origin, target));
                    }
                  }
                }
              }
              break;
            } else {
              if (moves != null) {
                moves.add(new QuietMove(origin, target));
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
          if (board.get(target) instanceof Piece captured) {
            if (captured.colour() != pawn.colour()) {
              if (captured instanceof King) {
                return false;
              } else {
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
          } else {
            if (enPassantTarget != null) {
              if (target == enPassantTarget) {
                int stop = (target / 10) * 10 + origin % 10;
                if (moves != null) {
                  moves.add(new EnPassant(origin, target, stop));
                }
              }
            }
          }
        }
        int direction = switch (pawn.colour()) {
          case WHITE -> 1;
          case BLACK -> -1;
        };
        int target = origin + direction;
        if (board.get(target) == null) {
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
              if (board.get(target2) == null) {
                if (moves != null) {
                  moves.add(new DoubleStep(origin, target2, target));
                }
              }
            }
          }
        }
      }
    }
    return true;
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

  static void validate(List<Unit> board, Colour sideToMove, Set<Integer> castlingOrigins,
      Integer enPassantTarget) {
    for (Colour value : Colour.values()) {
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
      int doubleStepOrigin = enPassantTarget + switch (sideToMove) {
        case WHITE -> 1;
        case BLACK -> -1;
      };
      int doubleStepTarget = enPassantTarget + switch (sideToMove) {
        case WHITE -> -1;
        case BLACK -> 1;
      };
      int doubleStepStop = enPassantTarget;
      if (!(doubleStepStop % 10 == switch (sideToMove) {
        case WHITE -> 6;
        case BLACK -> 3;
      } && board.get(doubleStepOrigin) == null && board.get(doubleStepStop) == null && board.get(
          doubleStepTarget) instanceof Pawn(Colour colour) && colour != sideToMove)) {
        throw new IllegalArgumentException("Not accepted en passant square");
      }
    }
  }

  static String toFormatted(List<Unit> board, Colour sideToMove, Set<Integer> castlingOrigins,
      Integer enPassantTarget, String operation) {
    List<String> args = new ArrayList<>();
    for (int rank = 8; rank >= 1; rank--) {
      for (int file = 1; file <= 8; file++) {
        if (board.get((file + 1) * 10 + rank) instanceof Piece piece) {
          String code = switch (piece) {
            case King _ -> "K";
            case Queen _ -> "Q";
            case Rook _ -> "R";
            case Bishop _ -> "B";
            case Knight _ -> "N";
            case Pawn _ -> "P";
          };
          args.add(switch (piece.colour()) {
            case WHITE -> code.toUpperCase();
            case BLACK -> code.toLowerCase();
          });
        } else {
          args.add(".");
        }
      }
      switch (rank) {
        case 8 -> args.add(switch (sideToMove) {
          case WHITE -> "w";
          case BLACK -> "b";
        });
        case 7 -> {
          if (!castlingOrigins.isEmpty()) {
            StringBuilder arg = new StringBuilder();
            if (castlingOrigins.contains(61)) {
              if (castlingOrigins.contains(91)) {
                arg.append("K");
              }
              if (castlingOrigins.contains(21)) {
                arg.append("Q");
              }
            }
            if (castlingOrigins.contains(68)) {
              if (castlingOrigins.contains(98)) {
                arg.append("k");
              }
              if (castlingOrigins.contains(28)) {
                arg.append("q");
              }
            }
            args.add(arg.toString());
          } else {
            args.add("-");
          }
        }
        case 6 -> {
          if (enPassantTarget != null) {
            args.add(new String(new char[]{(char) ('a' + enPassantTarget / 10 - 2),
                (char) ('1' + enPassantTarget % 10 - 1)}));
          } else {
            args.add("-");
          }
        }
        case 4 -> args.add(operation);
      }
    }
    return ("8 %s %s %s %s %s %s %s %s    Side to move: %s%n"
        + "7 %s %s %s %s %s %s %s %s    Castling rights: %s%n"
        + "6 %s %s %s %s %s %s %s %s    En passant target: %s%n" + "5 %s %s %s %s %s %s %s %s%n"
        + "4 %s %s %s %s %s %s %s %s    %s%n" + "3 %s %s %s %s %s %s %s %s%n"
        + "2 %s %s %s %s %s %s %s %s%n" + "1 %s %s %s %s %s %s %s %s%n"
        + "  a b c d e f g h").formatted(args.toArray());
  }
}
