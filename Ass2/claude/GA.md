# GA.md — Genetic Algorithm Implementation
**Spec reference:** SPEC.md § Genetic Algorithm (8 marks functionality, 3 marks configuration)

Everything described here lives inside `GeneticAlgorithm.java` as private methods and fields. The only public-facing surface is:

```java
public double run(KnapsackInstance inst, Random rand)
```

---

## Representation

A solution (chromosome) is just `int[] bits` of length `n`. No wrapper class needed. A population is `int[][] population` — an array of these bit arrays. Fitness values live in a parallel `double[] fitness` array.

```java
int[][] population = new int[POP_SIZE][inst.n];
double[] fitness   = new double[POP_SIZE];
```

---

## Initialisation

```java
private void initPopulation(int[][] pop, KnapsackInstance inst, Random rand) {
    for (int i = 0; i < pop.length; i++) {
        for (int j = 0; j < inst.n; j++) {
            pop[i][j] = rand.nextInt(2);
        }
        inst.repair(pop[i]);
    }
}
```

Repair immediately after random init — avoids a first generation full of zero-fitness solutions.

---

## Fitness

Delegated entirely to `inst.evaluate(bits)` — returns total value if feasible, else `0.0`. No logic needed here.

---

## Selection — Tournament

```java
private int[] tournamentSelect(int[][] pop, double[] fitness, Random rand) {
    int best = rand.nextInt(pop.length);
    for (int i = 1; i < TOURNAMENT_K; i++) {
        int challenger = rand.nextInt(pop.length);
        if (fitness[challenger] > fitness[best]) best = challenger;
    }
    return pop[best].clone();
}
```

Returns a copy so the original population is not modified.

---

## Crossover — Single-Point

```java
private int[][] singlePointCrossover(int[] p1, int[] p2, Random rand) {
    int n = p1.length;
    int point = rand.nextInt(n - 1) + 1;
    int[] c1 = new int[n], c2 = new int[n];
    for (int i = 0; i < n; i++) {
        c1[i] = i < point ? p1[i] : p2[i];
        c2[i] = i < point ? p2[i] : p1[i];
    }
    return new int[][]{c1, c2};
}
```

Only applied with probability `Pc`. If crossover is skipped, children are clones of the parents.

---

## Mutation — Bit-Flip

```java
private void mutate(int[] bits, Random rand) {
    for (int i = 0; i < bits.length; i++) {
        if (rand.nextDouble() < Pm) {
            bits[i] = 1 - bits[i];
        }
    }
}
```

`Pm = 1.0 / n` gives approximately one expected flip per chromosome — standard default.

---

## Main Loop

```java
public double run(KnapsackInstance inst, Random rand) {
    long start = System.currentTimeMillis();

    int[][] pop    = new int[POP_SIZE][inst.n];
    double[] fit   = new double[POP_SIZE];
    initPopulation(pop, inst, rand);
    for (int i = 0; i < POP_SIZE; i++) fit[i] = inst.evaluate(pop[i]);

    int[] bestEver    = findBest(pop, fit).clone();
    double bestFitness = inst.evaluate(bestEver);

    for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
        int[][] newPop = new int[POP_SIZE][inst.n];
        double[] newFit = new double[POP_SIZE];

        // Elitism: copy top 2 directly
        int[][] elite = getTopK(pop, fit, ELITISM_COUNT);
        for (int i = 0; i < ELITISM_COUNT; i++) {
            newPop[i] = elite[i].clone();
            newFit[i] = inst.evaluate(newPop[i]);
        }

        // Fill the rest
        int idx = ELITISM_COUNT;
        while (idx < POP_SIZE) {
            int[] p1 = tournamentSelect(pop, fit, rand);
            int[] p2 = tournamentSelect(pop, fit, rand);

            int[][] children = rand.nextDouble() < Pc
                ? singlePointCrossover(p1, p2, rand)
                : new int[][]{p1, p2};

            for (int[] child : children) {
                if (idx >= POP_SIZE) break;
                mutate(child, rand);
                inst.repair(child);
                newPop[idx] = child;
                newFit[idx] = inst.evaluate(child);
                if (newFit[idx] > bestFitness) {
                    bestFitness = newFit[idx];
                    bestEver = child.clone();
                }
                idx++;
            }
        }

        pop = newPop;
        fit = newFit;
    }

    this.runtime = (System.currentTimeMillis() - start) / 1000.0;
    return bestFitness;
}
```

---

## Configuration Constants

```java
private static final int    POP_SIZE        = 100;
private static final int    MAX_GENERATIONS = 500;
private static final double Pc              = 0.8;
private static final int    TOURNAMENT_K    = 3;
private static final int    ELITISM_COUNT   = 2;
// Pm = 1.0 / inst.n  — computed per instance in run()
```

Store `runtime` as a field so `Main` can retrieve it after calling `run()`.

---

## Report: What to Document (3 marks)

- Final values for all constants above
- Why `Pm = 1/n` is the standard choice
- Why tournament selection over roulette wheel (simpler, no fitness scaling issues)
- Termination: fixed generations vs stagnation — justify your choice
- Effect of elitism on convergence stability
