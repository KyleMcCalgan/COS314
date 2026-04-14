# STATS.md — Wilcoxon Signed-Rank Test
**Spec reference:** SPEC.md § Wilcoxon Statistical Test (2 marks)

---

## What's Required

A **one-tailed Wilcoxon signed-rank test at α = 0.05** comparing GA vs ILS solution quality across runs.

- **H₀:** median performance of GA and ILS are equivalent
- **H₁:** one algorithm performs significantly better (specify direction from your data)
- Report: W-statistic, z-score, p-value, conclusion

Run each algorithm at least 10 times per instance (seeds 1–10) to get paired samples.

---

## Steps

Given `ga[i]` and `ils[i]` result pairs for `i = 0..N-1`:

1. **Differences:** `d[i] = ga[i] - ils[i]`
2. **Drop zeros:** discard pairs where `d[i] == 0`, let N = remaining count
3. **Rank absolute differences:** sort `|d[i]|` ascending, assign ranks 1..N. Ties get average rank.
4. **Sign ranks:** rank gets `+` if `d[i] > 0` (GA better), `-` if `d[i] < 0` (ILS better)
5. **Compute:** `W+ = sum of positive ranks`, `W- = sum of negative ranks`, `W = min(W+, W-)`
6. **Normal approximation** (valid for N ≥ 10):
   ```
   μ = N(N+1)/4
   σ = sqrt(N(N+1)(2N+1)/24)
   z = (W - μ) / σ
   ```
7. **One-tailed p-value:** reject H₀ if `z < -1.645` (α = 0.05)

---

## Java Implementation

Add this as a static method in `Main.java` or a standalone `WilcoxonTest.java`:

```java
public static void wilcoxon(double[] ga, double[] ils, String label) {
    int n = ga.length;
    List<double[]> nonZero = new ArrayList<>(); // [diff, absDiff]
    for (int i = 0; i < n; i++) {
        double d = ga[i] - ils[i];
        if (d != 0) nonZero.add(new double[]{d, Math.abs(d)});
    }
    int N = nonZero.size();
    if (N == 0) { System.out.println(label + ": all ties, test not applicable"); return; }

    // Sort by absolute difference, assign ranks with tie averaging
    nonZero.sort((a, b) -> Double.compare(a[1], b[1]));
    double[] ranks = new double[N];
    for (int i = 0; i < N; ) {
        int j = i;
        while (j < N && nonZero.get(j)[1] == nonZero.get(i)[1]) j++;
        double avg = (i + 1 + j) / 2.0;
        for (int k = i; k < j; k++) ranks[k] = avg;
        i = j;
    }

    double Wplus = 0, Wminus = 0;
    for (int i = 0; i < N; i++) {
        if (nonZero.get(i)[0] > 0) Wplus  += ranks[i];
        else                        Wminus += ranks[i];
    }
    double W  = Math.min(Wplus, Wminus);
    double mu = N * (N + 1) / 4.0;
    double sigma = Math.sqrt(N * (N + 1) * (2 * N + 1) / 24.0);
    double z  = (W - mu) / sigma;
    double p  = normalCDF(z);

    System.out.printf("--- Wilcoxon: %s ---%n", label);
    System.out.printf("N=%d  W=%.1f  W+=%.1f  W-=%.1f  z=%.4f  p≈%.4f%n", N, W, Wplus, Wminus, z, p);
    if (p < 0.05)
        System.out.printf("REJECT H0: %s significantly better (α=0.05)%n", Wplus > Wminus ? "GA" : "ILS");
    else
        System.out.println("FAIL TO REJECT H0: no significant difference (α=0.05)");
}

// Abramowitz & Stegun normal CDF approximation
private static double normalCDF(double z) {
    double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
    double p = 1 - (Math.exp(-0.5 * z * z) / Math.sqrt(2 * Math.PI))
             * t * (0.319381530 + t * (-0.356563782
             + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
    return z >= 0 ? p : 1 - p;
}
```

---

## Collecting Data

```java
double[] gaResults  = new double[10];
double[] ilsResults = new double[10];
for (int seed = 1; seed <= 10; seed++) {
    gaResults[seed-1]  = new GeneticAlgorithm().run(inst, new Random(seed));
    ilsResults[seed-1] = new IteratedLocalSearch().run(inst, new Random(seed));
}
wilcoxon(gaResults, ilsResults, inst.name);
```

---

## Small-N Critical Values (if fewer than 10 runs)

| N  | Critical W (α=0.05, one-tailed) |
|----|----------------------------------|
| 5  | 2                                |
| 6  | 3                                |
| 7  | 5                                |
| 8  | 8                                |
| 9  | 10                               |
| 10 | 13                               |

Reject H₀ if `W ≤` the critical value for your N.

---

## What to Write in the Report

1. State H₀ and H₁ explicitly
2. Show a table of paired GA vs ILS results (10 runs, all instances or aggregated)
3. Report N, W, z, p-value per comparison
4. State conclusion: rejected or not at α = 0.05
5. Interpret: if rejected, which algorithm is better and by how much
