# COS314 Assignment 2 — GA vs ILS for 0/1 Knapsack

## Compile & Run (development)

```bash
cd src
javac *.java
java Main
```

## Build & Run the JAR (required for submission)

```bash
# From the Ass2/ directory
find src -name "*.java" > sources.txt
mkdir -p out
javac -d out @sources.txt
jar cfe assignment2.jar Main -C out .

java -jar assignment2.jar
```

> The JAR does not bundle the data files. It must be run from a directory that has a `data/` folder next to it. If submitting, zip the JAR and the `data/` folder together — whoever extracts it just needs to `cd` into that folder and run the command above.

## Modes

| Choice | Description |
|--------|-------------|
| 1 | Run GA on a single instance |
| 2 | Run ILS on a single instance |
| 3 | Run both on all 11 instances (report table) |
| 4 | Wilcoxon statistical test (seeds 1–10, all instances) |
