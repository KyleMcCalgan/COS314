import java.io.*;
import java.util.*;

public class Main {

    // Known optimums for display only (never passed to any algorithm)
    private static final Map<String, Double> OPTIMUMS = new LinkedHashMap<>();
    static {
        OPTIMUMS.put("f1_l-d_kp_10_269",    295.0);
        OPTIMUMS.put("f2_l-d_kp_20_878",   1024.0);
        OPTIMUMS.put("f3_l-d_kp_4_20",       35.0);
        OPTIMUMS.put("f4_l-d_kp_4_11",       23.0);
        OPTIMUMS.put("f5_l-d_kp_15_375",    481.0694);
        OPTIMUMS.put("f6_l-d_kp_10_60",      52.0);
        OPTIMUMS.put("f7_l-d_kp_7_50",      107.0);
        OPTIMUMS.put("f8_l-d_kp_23_10000", 9767.0);
        OPTIMUMS.put("f9_l-d_kp_5_80",      130.0);
        OPTIMUMS.put("f10_l-d_kp_20_879",  1025.0);
        OPTIMUMS.put("knapPI_1_100_1000_1", 9147.0);
    }

    private static final String[] FILE_NAMES = OPTIMUMS.keySet().toArray(new String[0]);

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter seed: ");
        long seed = sc.nextLong();
        Random rand = new Random(seed);

        System.out.println();
        System.out.println("Select mode:");
        System.out.println("  1. Run GA on a single instance");
        System.out.println("  2. Run ILS on a single instance");
        System.out.println("  3. Run both on all instances (report table)");
        System.out.println("  4. Run Wilcoxon statistical test (seeds 1-10, all instances)");
        System.out.print("Choice: ");
        int choice = sc.nextInt();

        // Locate the data directory relative to CWD or JAR location
        String dataDir = findDataDir();

        switch (choice) {
            case 1:  runSingle("GA",  seed, rand, sc, dataDir); break;
            case 2:  runSingle("ILS", seed, rand, sc, dataDir); break;
            case 3:  runAll(seed, rand, dataDir);                break;
            case 4:  runWilcoxon(dataDir);                       break;
            default: System.out.println("Invalid choice.");
        }
    }

    //mode 1 & 2: single instance

    private static void runSingle(String algo, long seed, Random rand, Scanner sc, String dataDir) throws Exception {
        System.out.println("\nAvailable instances:");
        for (int i = 0; i < FILE_NAMES.length; i++) {
            System.out.printf("  %2d. %s%n", i + 1, FILE_NAMES[i]);
        }
        System.out.print("Select instance number: ");
        int idx = sc.nextInt() - 1;
        if (idx < 0 || idx >= FILE_NAMES.length) { System.out.println("Invalid."); return; }

        String name = FILE_NAMES[idx];
        KnapsackInstance inst = KnapsackInstance.parse(dataDir + File.separator + name);

        double result;
        double timeS;
        if (algo.equals("GA")) {
            GeneticAlgorithm ga = new GeneticAlgorithm();
            result = ga.run(inst, rand);
            timeS  = ga.runtime;
        } else {
            IteratedLocalSearch ils = new IteratedLocalSearch();
            result = ils.run(inst, rand);
            timeS  = ils.runtime;
        }

        double opt = OPTIMUMS.getOrDefault(name, Double.NaN);
        System.out.println();
        System.out.printf("Problem Instance: %-30s%n", name);
        System.out.printf("Algorithm       : %s%n", algo);
        System.out.printf("Seed            : %d%n", seed);
        System.out.printf("Best Solution   : %.4f%n", result);
        System.out.printf("Known Optimum   : %.4f%n", opt);
        System.out.printf("Runtime (s)     : %.3f%n", timeS);
    }

    //mode 3: full results table

    private static void runAll(long seed, Random rand, String dataDir) throws Exception {
        String hdr = String.format("%-30s | %-5s | %-6s | %-12s | %-12s | %s",
            "Problem Instance", "Algo", "Seed", "Best", "Optimum", "Time(s)");
        StringBuilder sbSep = new StringBuilder();
        for (int i = 0; i < hdr.length(); i++) sbSep.append('-');
        String sep = sbSep.toString();
        System.out.println();
        System.out.println(sep);
        System.out.println(hdr);
        System.out.println(sep);

        for (String name : FILE_NAMES) {
            KnapsackInstance inst = KnapsackInstance.parse(dataDir + File.separator + name);
            double opt = OPTIMUMS.getOrDefault(name, Double.NaN);

            // ILS first, then GA, each gets its own derived Random from the shared rand
            // (so the single seed deterministically produces two sub-seeds)
            long ilsSeed = rand.nextLong();
            long gaSeed  = rand.nextLong();

            IteratedLocalSearch ils = new IteratedLocalSearch();
            double ilsResult = ils.run(inst, new Random(ilsSeed));
            System.out.printf("%-30s | %-5s | %-6d | %-12.4f | %-12.4f | %.3f%n",
                name, "ILS", seed, ilsResult, opt, ils.runtime);

            GeneticAlgorithm ga = new GeneticAlgorithm();
            double gaResult = ga.run(inst, new Random(gaSeed));
            System.out.printf("%-30s | %-5s | %-6d | %-12.4f | %-12.4f | %.3f%n",
                name, "GA", seed, gaResult, opt, ga.runtime);
        }

        System.out.println(sep);
    }

    //mode 4: Wilcoxon test — seeds 1..10, aggregate across all instances

    private static void runWilcoxon(String dataDir) throws Exception {
        System.out.println("\nRunning Wilcoxon test (10 seeds x 11 instances) — this may take a moment...");
        int RUNS = 10;
        double[] gaAll  = new double[RUNS * FILE_NAMES.length];
        double[] ilsAll = new double[RUNS * FILE_NAMES.length];
        int ptr = 0;

        for (String name : FILE_NAMES) {
            KnapsackInstance inst = KnapsackInstance.parse(dataDir + File.separator + name);
            System.out.printf("  %s ...%n", name);
            for (int s = 1; s <= RUNS; s++) {
                gaAll[ptr]  = new GeneticAlgorithm().run(inst, new Random(s));
                ilsAll[ptr] = new IteratedLocalSearch().run(inst, new Random(s));
                ptr++;
            }
        }

        System.out.println();
        wilcoxon(gaAll, ilsAll, "GA vs ILS (all instances, seeds 1-10)");
    }

    //Wilcoxon signed-rank test

    public static void wilcoxon(double[] ga, double[] ils, String label) {
        int n = ga.length;
        List<double[]> nonZero = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double d = ga[i] - ils[i];
            if (d != 0) nonZero.add(new double[]{d, Math.abs(d)});
        }
        int N = nonZero.size();
        if (N == 0) {
            System.out.println(label + ": all ties, test not applicable");
            return;
        }

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
        double W     = Math.min(Wplus, Wminus);
        double mu    = N * (N + 1) / 4.0;
        double sigma = Math.sqrt(N * (N + 1) * (2 * N + 1) / 24.0);
        double z     = (W - mu) / sigma;
        double p     = normalCDF(z);

        System.out.printf("--- Wilcoxon: %s ---%n", label);
        System.out.printf("N=%d  W=%.1f  W+=%.1f  W-=%.1f  z=%.4f  p≈%.4f%n",
            N, W, Wplus, Wminus, z, p);
        if (p < 0.05)
            System.out.printf("REJECT H0: %s significantly better (alpha=0.05)%n",
                Wplus > Wminus ? "GA" : "ILS");
        else
            System.out.println("FAIL TO REJECT H0: no significant difference (alpha=0.05)");
    }

    //Abramowitz & Stegun normal CDF approximation
    private static double normalCDF(double z) {
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double p = 1 - (Math.exp(-0.5 * z * z) / Math.sqrt(2 * Math.PI))
                 * t * (0.319381530 + t * (-0.356563782
                 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
        return z >= 0 ? p : 1 - p;
    }

    //utility: find the data directory

    private static String findDataDir() {
        // Try CWD/data, then CWD/../data, then CWD/Ass2/data
        String[] candidates = {
            "data",
            "../data",
            "Ass2/data",
            System.getProperty("user.dir") + "/data",
            System.getProperty("user.dir") + "/../data",
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.isDirectory() && new File(f, "f1_l-d_kp_10_269").exists()) {
                return f.getAbsolutePath();
            }
        }
        // fallback — let parse() produce a helpful FileNotFoundException
        return "data";
    }
}
