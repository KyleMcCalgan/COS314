# CLAUDE.md — COS314 Assignment 2: GA vs ILS for 0/1 Knapsack

## Objective

Build a single Java JAR that solves the 0/1 Knapsack problem using two metaheuristic algorithms — a Genetic Algorithm (GA) and an Iterated Local Search (ILS) — across 11 benchmark instances. The program is seeded, menu-driven, and produces a results table for a comparative report.

Full assignment requirements are in [`SPEC.md`](./SPEC.md).

---

## Project Structure

```
assignment2/
├── src/
│   └── cos314/
│       ├── Main.java                ← entry point: seed prompt, menu, result table
│       ├── Item.java                ← data class: value + weight pair
│       ├── KnapsackInstance.java    ← problem data + parse() + evaluate() + repair()
│       ├── GeneticAlgorithm.java    ← full GA (all operators internal)
│       └── IteratedLocalSearch.java ← full ILS (all logic internal)
├── data/
│   ├── f1_l-d_kp_10_269
│   ├── f2_l-d_kp_20_878
│   ├── f3_l-d_kp_4_20
│   ├── f4_l-d_kp_4_11
│   ├── f5_l-d_kp_15_375
│   ├── f6_l-d_kp_10_60
│   ├── f7_l-d_kp_7_50
│   ├── f8_l-d_kp_23_10000
│   ├── f9_l-d_kp_5_80
│   ├── f10_l-d_kp_20_879
│   └── knapPI_1_100_1000_1
├── claude/
│   ├── CLAUDE.md   ← this file
│   ├── SPEC.md     ← full assignment spec decoded
│   ├── GA.md       ← GA internals reference
│   ├── ILS.md      ← ILS internals reference
│   └── STATS.md    ← Wilcoxon test implementation
└── README.md
```

---

## What Each File Does

**`Item.java`** — two fields only: `double value`, `double weight`. Nothing else.

**`KnapsackInstance.java`** — owns the problem data and three key methods:
- `parse(String filePath)` — reads a problem file, returns a `KnapsackInstance`
- `evaluate(int[] bits)` — returns total value if total weight ≤ capacity, else `0.0`
- `repair(int[] bits)` — mutates the array in-place, drops least efficient selected items until feasible

**`GeneticAlgorithm.java`** — all GA logic lives here as private methods (selection, crossover, mutation, elitism). Population is just `int[][]`. Exposes one public method: `double run(KnapsackInstance inst, Random rand)`.

**`IteratedLocalSearch.java`** — all ILS logic lives here as private methods (init, hill climb, perturbation, acceptance). Exposes one public method: `double run(KnapsackInstance inst, Random rand)`.

**`Main.java`** — prompts for seed, shows menu, loads file(s), calls algorithms, prints results. Holds the known-optimums map for display only — never passed to any algorithm.

---

## Reference Documents

| Document               | Contents                                                   |
|------------------------|------------------------------------------------------------|
| [`SPEC.md`](./SPEC.md) | Full decoded assignment spec, requirements, marking rubric |
| [`GA.md`](./GA.md)     | GA internals: representation, operators, loop, parameters  |
| [`ILS.md`](./ILS.md)   | ILS internals: local search, perturbation, loop, parameters|
| [`STATS.md`](./STATS.md)| Wilcoxon signed-rank test: steps + Java implementation    |

---

## Build & Run

```bash
# Compile
find src -name "*.java" > sources.txt
javac -d out @sources.txt
jar cfe assignment2.jar cos314.Main -C out .

# Run
java -jar assignment2.jar
```

### Expected interaction
```
Enter seed: 42

Select mode:
  1. Run GA on a single instance
  2. Run ILS on a single instance
  3. Run both on all instances (report table)
Choice: 3

Problem Instance           | Algo | Seed | Best    | Optimum | Time(s)
f1_l-d_kp_10_269           | ILS  | 42   | 295.0   | 295.0   | 0.41
f1_l-d_kp_10_269           | GA   | 42   | 295.0   | 295.0   | 1.23
...
```

---

## Critical Rules

### Seeding
- Prompt at startup: `System.out.print("Enter seed: ");`
- ONE `Random rand = new Random(seed)` passed into every algorithm call
- Never use `Math.random()` — always `rand.nextInt()` / `rand.nextDouble()`
- Same seed must always produce the same result

### No External Libraries
- Only `java.util.*`, `java.io.*` — no runtime dependencies
- Wilcoxon test implemented manually (see `STATS.md`)

### Solution Representation
- Both algorithms use `int[] bits` of length `n`
- `bits[i] = 1` → item i selected, `bits[i] = 0` → excluded
- No wrapper class — a solution is just an `int[]`

### Constraint Handling
- After any random operation call `inst.repair(bits)` before evaluating
- Repair drops the least value/weight-efficient items until weight ≤ capacity
- Prevents zero-fitness dead solutions from flooding the population

---

## Parameters

### GA
| Parameter       | Value             |
|-----------------|-------------------|
| Population size | 100               |
| Max generations | 500               |
| Crossover rate  | 0.8               |
| Mutation rate   | 1.0 / n           |
| Tournament size | 3                 |
| Elitism         | top 2 carry over  |

### ILS
| Parameter             | Value                     |
|-----------------------|---------------------------|
| Max iterations        | 1000                      |
| Stagnation limit      | 100                       |
| Perturbation strength | max(2, n / 4) bit flips   |
| Local search          | Best-improving hill climb |

---

## Testing Checklist

- [ ] `f3_l-d_kp_4_20` (4 items, W=20): both find value = 35
- [ ] `f4_l-d_kp_4_11` (4 items, W=11): both find value = 23
- [ ] Same seed → identical result on repeat runs
- [ ] All 11 files parse without error
- [ ] `java -jar assignment2.jar` runs with no IDE on a clean machine
