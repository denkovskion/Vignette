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
import java.util.List;

class Nodes {

  sealed interface Node {

  }

  record DivideRoot(long count, List<Node> children) implements Node {

  }

  record DivideLeaf(Move move, long count) implements Node {

  }

  record PerftNode(long count) implements Node {

  }

  record MateRoot(List<Node> children) implements Node {

  }

  record MateBranch(Move move, int distance, List<Node> children) implements Node {

  }

  record MateLeaf(Move move, int distance) implements Node {

  }

  record IllegalNode() implements Node {

  }

  static String formatToString(Node node, Position position, int moveNo, boolean inline) {
    StringBuilder output = new StringBuilder();
    switch (node) {
      case DivideRoot(long count, List<Node> children) -> {
        for (Node child : children) {
          output.append(formatToString(child, position, moveNo, false));
          output.append(System.lineSeparator());
        }
        output.append(count);
      }
      case DivideLeaf(Move move, long count) -> {
        Engine.makeMove(position, move, null, output);
        output.append(" ").append(count);
      }
      case PerftNode(long count) -> output.append(count);
      case MateRoot(List<Node> children) -> {
        boolean first = true;
        for (Node child : children) {
          if (!first) {
            output.append(System.lineSeparator());
          }
          output.append(formatToString(child, position, moveNo, false));
          first = false;
        }
      }
      case MateBranch(Move move, _, List<Node> children) -> {
        if (position.blackToMove()) {
          if (!inline) {
            output.append(moveNo).append("...");
          }
        } else {
          output.append(moveNo).append(".");
        }
        Position positionNext = Engine.makeMove(position, move, null, output).orElseThrow();
        boolean first = true;
        for (Node child : children) {
          if (first) {
            output.append(" ");
          } else {
            output.append(System.lineSeparator())
                .repeat("\t", positionNext.blackToMove() ? moveNo - 1 : moveNo);
          }
          output.append(
              formatToString(child, positionNext, positionNext.blackToMove() ? moveNo : moveNo + 1,
                  first));
          first = false;
        }
      }
      case MateLeaf(Move move, int distance) -> {
        Engine.makeMove(position, move, null, output);
        output.append(" [#").append(distance).append("]");
      }
      case IllegalNode() -> output.append("Illegal position");
    }
    return output.toString();
  }
}
