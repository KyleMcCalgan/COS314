import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Scanner;

public class RunExperiment {
    private static final int RUN_COUNT = 30;
    private static final double CRITICAL_T_DF_29_ALPHA_005 = 2.045;

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter train CSV path: ");
        Path trainPath = Paths.get(scanner.nextLine().trim());
        System.out.print("Enter test CSV path: ");
        Path testPath = Paths.get(scanner.nextLine().trim());

        double[] arithmeticAccuracies = new double[RUN_COUNT];
        double[] treeAccuracies = new double[RUN_COUNT];
        double[] bestArithmetic = null;
        double[] bestTree = null;
        int bestArithmeticSeed = -1;
        int bestTreeSeed = -1;

        System.out.println();
        System.out.println("Seed | Arithmetic Test % | DecisionTree Test %");
        System.out.println("-----+-------------------+--------------------");

        for (int i = 0; i < RUN_COUNT; i++) {
            int seed = i + 1;
            double[] arithmetic = ArithmeticGP.runTraining(seed, trainPath, testPath, false, false);
            double[] tree = DecisionTreeGP.runTraining(seed, trainPath, testPath, false, false);

            arithmeticAccuracies[i] = arithmetic[1];
            treeAccuracies[i] = tree[1];

            if (bestArithmetic == null || arithmetic[1] > bestArithmetic[1]) {
                bestArithmetic = arithmetic;
                bestArithmeticSeed = seed;
            }
            if (bestTree == null || tree[1] > bestTree[1]) {
                bestTree = tree;
                bestTreeSeed = seed;
            }

            System.out.printf("%4d | %17.2f | %18.2f%n",
                    seed,
                    arithmetic[1] * 100.0,
                    tree[1] * 100.0);
        }

        double tStatistic = pairedTStatistic(arithmeticAccuracies, treeAccuracies);
        System.out.println();
        System.out.println("=== BEST RUNS ===");
        System.out.printf("ArithmeticGP: seed %d | train %.2f%% | test %.2f%% | F %.4f | runtime %.0f ms%n",
                bestArithmeticSeed,
                bestArithmetic[0] * 100.0,
                bestArithmetic[1] * 100.0,
                bestArithmetic[2],
                bestArithmetic[3]);
        System.out.printf("DecisionTreeGP: seed %d | train %.2f%% | test %.2f%% | F %.4f | runtime %.0f ms%n",
                bestTreeSeed,
                bestTree[0] * 100.0,
                bestTree[1] * 100.0,
                bestTree[2],
                bestTree[3]);

        System.out.println();
        System.out.println("=== PAIRED T-TEST ===");
        System.out.printf("t-statistic: %.4f%n", tStatistic);
        System.out.println("degrees of freedom: 29");
        if (Math.abs(tStatistic) > CRITICAL_T_DF_29_ALPHA_005) {
            System.out.println("Conclusion: statistically significant difference (p < 0.05)");
        } else {
            System.out.println("Conclusion: no statistically significant difference at p < 0.05");
        }

        ArithmeticGP.runTraining(bestArithmeticSeed, trainPath, testPath, false, true);
        DecisionTreeGP.runTraining(bestTreeSeed, trainPath, testPath, false, true);
        System.out.println();
        System.out.println("Saved best models for the reported best seeds.");
    }

    private static double pairedTStatistic(double[] first, double[] second) {
        double meanDifference = 0.0;
        for (int i = 0; i < first.length; i++) {
            meanDifference += first[i] - second[i];
        }
        meanDifference /= first.length;

        double sumSquaredDifferences = 0.0;
        for (int i = 0; i < first.length; i++) {
            double centered = (first[i] - second[i]) - meanDifference;
            sumSquaredDifferences += centered * centered;
        }

        double standardDeviation = Math.sqrt(sumSquaredDifferences / (first.length - 1));
        if (standardDeviation == 0.0) {
            return meanDifference == 0.0 ? 0.0 : Double.POSITIVE_INFINITY;
        }
        return meanDifference / (standardDeviation / Math.sqrt(first.length));
    }
}
