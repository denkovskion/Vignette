# Vignette

Vignette is a mate search chess program.

## Usage

Java 25 or later is required.

```
java -jar Vignette.jar [OPTIONS]
```

Vignette reads problems
as [Extended Position Description](https://www.chessprogramming.org/Extended_Position_Description)
records (with one operation: `dm` for direct mate or `acd` for perft) from standard input until
end-of-file, then solves them and writes solutions to standard output.

## Options

- `-h`, `--help` Shows help and exits.
- `-V`, `--version` Shows version and exits.
- `-d`, `--detailed` Enables detailed analysis.
- `-v`, `--verbose` Enables verbose logging.

## Example

> Fritz Giegold, Die Welt 1978

### Input

```
8/8/4p3/2P1k1p1/pPp1P3/B1Pp2p1/P2P2B1/R3KR2 w Q - dm 5;
```

### Output (default)

```
Ra1-c1 [#5]
```

### Output with `--detailed`

```
1.Ra1-c1 g5-g4 2.Rc1-c2 d3xc2 3.Ba3-c1 a4-a3 4.Rf1-f4 Ke5xf4 5.d2-d4#
```

## Author

Ivan Denkovski is the author of Vignette.
