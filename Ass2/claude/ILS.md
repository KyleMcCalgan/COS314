# ILS.md — Iterated Local Search Implementation
**Spec reference:** SPEC.md § Iterated Local Search (7 marks functionality, 3 marks configuration)

Everything described here lives inside `IteratedLocalSearch.java` as private methods and fields. The only public-facing surface is:

```java
public double run(KnapsackInstance inst, Random rand)
```

---

## Representation

Same as GA — a solution is just `int[] bits` of length `n`. No wrapper class. ILS works with one current solution and one best-ever solution at a time.

---

## Initial Solution

```java
private int[] randomInit(KnapsackInstance inst, Random rand) {
    int[] bits = new int[inst.n];
    for (int i = 0; i < inst.n; i++) {
        bits[i] = rand.nextInt(2);
    }
    inst.repair(bits);
    return bits;
}
```

Random seeded start, repaired immediately for feasibility.

---

## Local Search — Best-Improving Hill Climb

Repeatedly scan all n neighbours (flip one bit each), accept the best improvement found, repeat until no improvement exists. This finds the nearest local optimum.

```java
private int[] localSearch(int[] bits, KnapsackInstance inst) {
    int[] current = bits.clone();
    double currentFit = inst.evaluate(current);
    boolean improved = true;

    while (improved) {
        improved = false;
        int bestIdx = -1;
        double bestFit = currentFit;

        for (int i = 0; i < inst.n; i++) {
            current[i] = 1 - current[i];         // flip
            inst.repair(current);
            double f = inst.evaluate(current);
            if (f > bestFit) {
                bestFit = f;
                bestIdx = i;
            }
            current[i] = 1 - current[i];         // flip back
            // Note: if repair changed other bits, need to re-clone before flipping back
            // Safer: work on a copy per neighbour (see note below)
        }

        if (bestIdx != -1) {
            current[bestIdx] = 1 - current[bestIdx];
            inst.repair(current);
            currentFit = inst.evaluate(current);
            improved = true;
        }
    }

    return current;
}
```

> **Implementation note:** Because `repair()` modifies the array in-place, evaluate each neighbour on a fresh clone to avoid state corruption:
> ```java
> int[] neighbour = current.clone();
> neighbour[i] = 1 - neighbour[i];
> inst.repair(neighbour);
> double f = inst.evaluate(neighbour);
> if (f > bestFit) { bestFit = f; bestNeighbour = neighbour; }
> ```
> Keep whichever approach is cleaner in your implementation — just be consistent.

---

## Perturbation — Random Multi-Bit Flip

Flips `strength` randomly chosen bits to escape the current local optimum basin.

```java
private int[] perturb(int[] bits, int strength, KnapsackInstance inst, Random rand) {
    int[] perturbed = bits.clone();
    // Pick `strength` distinct random indices to flip
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < inst.n; i++) indices.add(i);
    Collections.shuffle(indices, rand);
    for (int i = 0; i < strength; i++) {
        int idx = indices.get(i);
        perturbed[idx] = 1 - perturbed[idx];
    }
    inst.repair(perturbed);
    return perturbed;
}
```

`strength = Math.max(2, inst.n / 4)` — flips ~25% of bits. Too small and you stay in the same basin; too large and it's effectively a random restart.

---

## Acceptance Criterion

Accept the new local optimum if it is equal to or better than the current solution:

```java
if (inst.evaluate(newLocalOpt) >= inst.evaluate(current)) {
    current = newLocalOpt;
}
```

Non-worsening acceptance — ILS relies purely on perturbation to escape, not probabilistic acceptance like simulated annealing.

---

## Main Loop

```java
public double run(KnapsackInstance inst, Random rand) {
    long start = System.currentTimeMillis();
    int strength = Math.max(2, inst.n / 4);

    int[] current   = localSearch(randomInit(inst, rand), inst);
    int[] bestEver  = current.clone();
    double bestFit  = inst.evaluate(bestEver);
    int stagnation  = 0;

    for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
        int[] perturbed    = perturb(current, strength, inst, rand);
        int[] newLocalOpt  = localSearch(perturbed, inst);
        double newFit      = inst.evaluate(newLocalOpt);

        // Acceptance
        if (newFit >= inst.evaluate(current)) {
            current = newLocalOpt;
            stagnation = 0;
        } else {
            stagnation++;
        }

        // Track global best
        if (newFit > bestFit) {
            bestFit  = newFit;
            bestEver = newLocalOpt.clone();
        }

        // Restart if stuck
        if (stagnation >= STAGNATION_LIMIT) {
            current    = localSearch(randomInit(inst, rand), inst);
            stagnation = 0;
        }
    }

    this.runtime = (System.currentTimeMillis() - start) / 1000.0;
    return bestFit;
}
```

---

## Configuration Constants

```java
private static final int MAX_ITERATIONS   = 1000;
private static final int STAGNATION_LIMIT = 100;
// strength = Math.max(2, inst.n / 4) — computed per instance in run()
```

Store `runtime` as a field so `Main` can retrieve it after calling `run()`.

---

## Report: What to Document (3 marks)

- Final values for all constants above
- Choice of best-improving vs first-improving local search and why
- Perturbation strength rationale — what happens if it's too small or too large
- Why non-worsening acceptance rather than probabilistic (SA-style)
- Role of the stagnation restart in avoiding permanent local optima traps
