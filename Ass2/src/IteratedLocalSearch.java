import java.util.*;

public class IteratedLocalSearch {

    private static final int MAX_ITERATIONS   = 1000;
    private static final int STAGNATION_LIMIT = 100;

    public double runtime = 0.0;  // seconds, set after run()

    public double run(KnapsackInstance inst, Random rand) {
        long start = System.currentTimeMillis();
        int strength = Math.max(2, inst.n / 4);

        int[] current  = localSearch(randomInit(inst, rand), inst);
        int[] bestEver = current.clone();
        double bestFit = inst.evaluate(bestEver);
        int stagnation = 0;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            int[] perturbed   = perturb(current, strength, inst, rand);
            int[] newLocalOpt = localSearch(perturbed, inst);
            double newFit     = inst.evaluate(newLocalOpt);

            // Non-worsening acceptance
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

            // Random restart if stuck
            if (stagnation >= STAGNATION_LIMIT) {
                current    = localSearch(randomInit(inst, rand), inst);
                stagnation = 0;
            }
        }

        this.runtime = (System.currentTimeMillis() - start) / 1000.0;
        return bestFit;
    }

    private int[] randomInit(KnapsackInstance inst, Random rand) {
        int[] bits = new int[inst.n];
        for (int i = 0; i < inst.n; i++) {
            bits[i] = rand.nextInt(2);
        }
        inst.repair(bits);
        return bits;
    }

    //Best-improving hill climb: scan all 1-flip neighbours, accept the best.
    private int[] localSearch(int[] bits, KnapsackInstance inst) {
        int[] current    = bits.clone();
        double currentFit = inst.evaluate(current);
        boolean improved  = true;

        while (improved) {
            improved = false;
            int[] bestNeighbour = null;
            double bestFit = currentFit;

            for (int i = 0; i < inst.n; i++) {
                int[] neighbour = current.clone();
                neighbour[i] = 1 - neighbour[i];
                inst.repair(neighbour);
                double f = inst.evaluate(neighbour);
                if (f > bestFit) {
                    bestFit = f;
                    bestNeighbour = neighbour;
                }
            }

            if (bestNeighbour != null) {
                current    = bestNeighbour;
                currentFit = bestFit;
                improved   = true;
            }
        }

        return current;
    }

    // Flip 'strength' distinct random bits, then repair.
    private int[] perturb(int[] bits, int strength, KnapsackInstance inst, Random rand) {
        int[] perturbed = bits.clone();
        List<Integer> indices = new ArrayList<>(inst.n);
        for (int i = 0; i < inst.n; i++) indices.add(i);
        Collections.shuffle(indices, rand);
        for (int i = 0; i < strength; i++) {
            int idx = indices.get(i);
            perturbed[idx] = 1 - perturbed[idx];
        }
        inst.repair(perturbed);
        return perturbed;
    }
}
