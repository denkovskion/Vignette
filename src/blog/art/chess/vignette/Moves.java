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

import blog.art.chess.vignette.Pieces.Piece;

class Moves {

  sealed interface Move {

  }

  record NullMove() implements Move {

  }

  record QuietMove(int origin, int target) implements Move {

  }

  record Capture(int origin, int target) implements Move {

  }

  record LongCastling(int origin, int target, int origin2, int target2) implements Move {

  }

  record ShortCastling(int origin, int target, int origin2, int target2) implements Move {

  }

  record DoubleStep(int origin, int target, int stop) implements Move {

  }

  record EnPassant(int origin, int target, int stop) implements Move {

  }

  record Promotion(int origin, int target, Piece promoted) implements Move {

  }

  record PromotionCapture(int origin, int target, Piece promoted) implements Move {

  }
}
