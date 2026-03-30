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

## Example

> Sam Loyd, Bradford Courier 1878

### Input

```
8/8/8/1R6/8/kBp1p3/1qQ4K/1nBn4 w - - dm 2;
```

### Output

```
Qc2-g2 [#2]
```

## Author

Ivan Denkovski is the author of Vignette.
