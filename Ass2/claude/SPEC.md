# SPEC.md — COS314 Assignment 2 Full Specification
**Due:** 18 April 2026 | **Language:** Java | **Team:** 2–3 members

---

## The Problem

0/1 Knapsack: given `n` items each with a `value` and `weight`, and a knapsack with capacity `W`, select a binary subset to maximise total value without exceeding W. Each item is in or out — no fractions. NP-hard at scale, so exact methods are infeasible → use metaheuristics.

---

## Input File Format

```
n W
v1 w1
v2 w2
...
vn wn
```

Line 1 is item count and capacity. Each subsequent line is one item's value then weight, space-separated. Treat all values as `double` — f5 has a fractional optimum (481.0694).

### All 11 Instances

| Filename                  | n   | W      | Known Optimum |
|---------------------------|-----|--------|---------------|
| f1_l-d_kp_10_269          | 10  | 269    | 295           |
| f2_l-d_kp_20_878          | 20  | 878    | 1024          |
| f3_l-d_kp_4_20            | 4   | 20     | 35            |
| f4_l-d_kp_4_11            | 4   | 11     | 23            |
| f5_l-d_kp_15_375          | 15  | 375    | 481.0694      |
| f6_l-d_kp_10_60           | 10  | 60     | 52            |
| f7_l-d_kp_7_50            | 7   | 50     | 107           |
| f8_l-d_kp_23_10000        | 23  | 10000  | 9767          |
| f9_l-d_kp_5_80            | 5   | 80     | 130           |
| f10_l-d_kp_20_879         | 20  | 879    | 1025          |
| knapPI_1_100_1000_1       | 100 | 995    | 9147          |

> Known optimums are for report display only — never used inside any algorithm.

---

## Required Algorithms

### Genetic Algorithm — 8 marks functionality
Population-based. Must have: binary representation, seeded random init, fitness function, selection, crossover, mutation, termination condition. Elitism strongly recommended.

### Iterated Local Search — 7 marks functionality
Trajectory-based. Must have: a starting solution, a local search procedure (hill climbing), a perturbation operator to escape local optima, an acceptance criterion, termination condition.

See `GA.md` and `ILS.md` for full implementation details.

---

## Program Requirements

1. **Seed prompt at startup** — ask user for seed, use `new Random(seed)` for all random operations
2. **Single JAR** — one program, menu to choose algorithm
3. **No external libraries** — `java.util.*` and `java.io.*` only
4. **Reproducible** — same seed always gives same result

---

## Report Requirements

### Results Table — 4 marks
One row per algorithm per instance, all 11 instances:

| Problem Instance        | Algorithm | Seed | Best Solution | Known Optimum | Runtime (s) |
|-------------------------|-----------|------|---------------|---------------|-------------|
| f1_l-d_kp_10_269        | ILS       | x    | x             | 295           | x           |
| f1_l-d_kp_10_269        | GA        | x    | x             | 295           | x           |
| ...                     |           |      |               |               |             |

### Configuration — 3 marks GA + 3 marks ILS
Document all parameter values and why they were chosen. Explain the termination condition.

### Wilcoxon Statistical Test — 2 marks
One-tailed Wilcoxon signed-rank test at α = 0.05 comparing GA vs ILS performance. See `STATS.md`.
- H₀: median performance of GA and ILS are equivalent
- Report W-statistic, p-value, and whether H₀ is rejected

### Critical Analysis — 3 marks
Compare solution quality and runtime. Discuss where each algorithm performed well or poorly and why, relating back to algorithm theory.

---

## Marking Breakdown

| Component                       | Marks |
|---------------------------------|-------|
| GA Functionality                | 8     |
| GA Configuration                | 3     |
| ILS Functionality               | 7     |
| ILS Configuration               | 3     |
| Results Table                   | 4     |
| Wilcoxon Test                   | 2     |
| Critical Analysis               | 3     |
| **Total**                       | **30**|
