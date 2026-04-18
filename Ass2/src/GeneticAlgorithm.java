import java.util.*;

public class GeneticAlgorithm {

    private static final int    POP_SIZE        = 100;
    private static final int    MAX_GENERATIONS = 500;
    private static final double Pc              = 0.8;
    private static final int    TOURNAMENT_K    = 3;
    private static final int    ELITISM_COUNT   = 2;

    public double runtime = 0.0;  // seconds, set after run()

    public double run(KnapsackInstance inst, Random rand) {
        long start = System.currentTimeMillis();
        double Pm = 1.0 / inst.n;

        int[][] pop = new int[POP_SIZE][inst.n];
        double[] fit = new double[POP_SIZE];
        initPopulation(pop, inst, rand);
        for (int i = 0; i < POP_SIZE; i++) fit[i] = inst.evaluate(pop[i]);

        double bestFitness = -1;
        for (double f : fit) if (f > bestFitness) bestFitness = f;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            int[][] newPop = new int[POP_SIZE][inst.n];
            double[] newFit = new double[POP_SIZE];

            //copy top ELITISM_COUNT directly
            int[][] elite = getTopK(pop, fit, ELITISM_COUNT);
            for (int i = 0; i < ELITISM_COUNT; i++) {
                newPop[i] = elite[i].clone();
                newFit[i] = inst.evaluate(newPop[i]);
            }

            int idx = ELITISM_COUNT;
            while (idx < POP_SIZE) {
                int[] p1 = tournamentSelect(pop, fit, rand);
                int[] p2 = tournamentSelect(pop, fit, rand);

                int[][] children = rand.nextDouble() < Pc
                    ? singlePointCrossover(p1, p2, rand)
                    : new int[][]{p1.clone(), p2.clone()};

                for (int[] child : children) {
                    if (idx >= POP_SIZE) break;
                    mutate(child, rand, Pm);
                    inst.repair(child);
                    newPop[idx] = child;
                    newFit[idx] = inst.evaluate(child);
                    if (newFit[idx] > bestFitness) {
                        bestFitness = newFit[idx];
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

    private void initPopulation(int[][] pop, KnapsackInstance inst, Random rand) {
        for (int i = 0; i < pop.length; i++) {
            for (int j = 0; j < inst.n; j++) {
                pop[i][j] = rand.nextInt(2);
            }
            inst.repair(pop[i]);
        }
    }

    private int[] tournamentSelect(int[][] pop, double[] fitness, Random rand) {
        int best = rand.nextInt(pop.length);
        for (int i = 1; i < TOURNAMENT_K; i++) {
            int challenger = rand.nextInt(pop.length);
            if (fitness[challenger] > fitness[best]) best = challenger;
        }
        return pop[best].clone();
    }

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

    private void mutate(int[] bits, Random rand, double Pm) {
        for (int i = 0; i < bits.length; i++) {
            if (rand.nextDouble() < Pm) {
                bits[i] = 1 - bits[i];
            }
        }
    }

    // Returns top k solutions by fitness (descending).
    private int[][] getTopK(int[][] pop, double[] fitness, int k) {
        Integer[] indices = new Integer[pop.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Double.compare(fitness[b], fitness[a]));
        int[][] top = new int[k][];
        for (int i = 0; i < k; i++) top[i] = pop[indices[i]].clone();
        return top;
    }
}
