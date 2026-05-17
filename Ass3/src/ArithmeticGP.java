import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public class ArithmeticGP {
    private static final int FEATURE_COUNT = 9;
    private static final int POPULATION_SIZE = 200;
    private static final int MAX_GENERATIONS = 100;
    private static final int MAX_INIT_DEPTH = 4;
    private static final int MAX_OFFSPRING_DEPTH = 8;
    private static final int TOURNAMENT_SIZE = 5;
    private static final double CROSSOVER_RATE = 0.90;
    private static final double MUTATION_RATE = 0.10;
    private static final String MODEL_FILE = "best_models.txt";
    private static final String MODEL_SECTION = "[ARITHMETIC]";
    private static final char[] OPERATORS = {'+', '-', '*', '%'};
    private static final Random RNG = new Random();

    private static double[][] trainFeatures;
    private static int[] trainLabels;
    private static List<String> lastBest = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter seed: ");
        long seed = Long.parseLong(scanner.nextLine().trim());
        System.out.print("Enter train CSV path: ");
        Path trainPath = Paths.get(scanner.nextLine().trim());
        System.out.print("Enter test CSV path: ");
        Path testPath = Paths.get(scanner.nextLine().trim());
        System.out.print("Mode (train/test): ");
        String mode = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

        if ("train".equals(mode)) {
            double[] result = runTraining(seed, trainPath, testPath, true, true);
            System.out.println();
            System.out.println("=== FINAL RESULTS ===");
            System.out.printf("Seed:              %d%n", seed);
            System.out.printf("Training Accuracy: %.2f%%%n", result[0] * 100.0);
            System.out.printf("Test Accuracy:     %.2f%%%n", result[1] * 100.0);
            System.out.printf("F-Measure:         %.4f%n", result[2]);
            System.out.printf("Runtime:           %.0f ms%n", result[3]);
            System.out.printf("Best Expression:   %s%n", toExpression(lastBest));
            System.out.printf("Saved Model File:  %s%n", MODEL_FILE);
        } else if ("test".equals(mode)) {
            List<String> model = loadModelSection(MODEL_SECTION);
            double[][] testFeatures = loadFeatures(testPath);
            int[] testLabels = loadLabels(testPath);
            double[] metrics = evaluate(model, testFeatures, testLabels);
            System.out.println("=== TEST RESULTS ===");
            System.out.printf("Accuracy:  %.2f%%%n", metrics[0] * 100.0);
            System.out.printf("F-Measure: %.4f%n", metrics[3]);
            System.out.printf("Model:     %s %s%n", MODEL_FILE, MODEL_SECTION);
        } else {
            throw new IllegalArgumentException("Unsupported mode: " + mode);
        }
    }

    public static double[] runTraining(long seed, Path trainPath, Path testPath, boolean verbose, boolean saveModel)
            throws IOException {
        RNG.setSeed(seed);
        trainFeatures = loadFeatures(trainPath);
        trainLabels = loadLabels(trainPath);
        double[][] testFeatures = loadFeatures(testPath);
        int[] testLabels = loadLabels(testPath);

        long start = System.currentTimeMillis();
        List<List<String>> population = initializePopulation();
        double[] fitness = new double[POPULATION_SIZE];
        List<String> globalBest = null;
        double globalBestFitness = Double.NEGATIVE_INFINITY;

        for (int generation = 1; generation <= MAX_GENERATIONS; generation++) {
            evaluatePopulation(population, fitness);
            int bestIndex = bestIndex(fitness);
            List<String> generationBest = population.get(bestIndex);
            double generationBestFitness = fitness[bestIndex];
            double[] generationMetrics = evaluate(generationBest, trainFeatures, trainLabels);

            if (generationBestFitness > globalBestFitness) {
                globalBestFitness = generationBestFitness;
                globalBest = copy(generationBest);
            }

            if (verbose) {
                System.out.printf(
                        "Gen %-3d | Best Fitness: %.4f | F-Measure: %.4f | Expression: %s%n",
                        generation,
                        generationBestFitness,
                        generationMetrics[3],
                        trim(toExpression(generationBest)));
            }

            List<List<String>> nextPopulation = new ArrayList<List<String>>();
            nextPopulation.add(copy(generationBest));
            while (nextPopulation.size() < POPULATION_SIZE) {
                List<String> child;
                if (RNG.nextDouble() < CROSSOVER_RATE) {
                    child = crossover(tournamentSelect(population, fitness), tournamentSelect(population, fitness));
                } else {
                    child = copy(tournamentSelect(population, fitness));
                }
                if (RNG.nextDouble() < MUTATION_RATE) {
                    mutate(child);
                }
                nextPopulation.add(child);
            }
            population = nextPopulation;
        }

        lastBest = globalBest;
        long runtime = System.currentTimeMillis() - start;
        double[] training = evaluate(globalBest, trainFeatures, trainLabels);
        double[] test = evaluate(globalBest, testFeatures, testLabels);

        if (saveModel) {
            saveModelSection(MODEL_SECTION, globalBest);
        }

        return new double[] {training[0], test[0], test[3], runtime};
    }

    private static List<List<String>> initializePopulation() {
        List<List<String>> population = new ArrayList<List<String>>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            int targetDepth = 2 + (i % (MAX_INIT_DEPTH - 1));
            boolean full = ((i / (MAX_INIT_DEPTH - 1)) % 2) == 0;
            List<String> tree = new ArrayList<String>();
            generateTree(tree, 1, targetDepth, full);
            population.add(tree);
        }
        return population;
    }

    private static void generateTree(List<String> tree, int currentDepth, int maxDepth, boolean full) {
        if (currentDepth >= maxDepth || (!full && currentDepth > 1 && RNG.nextBoolean())) {
            tree.add(randomTerminal());
            return;
        }
        tree.add(Character.toString(OPERATORS[RNG.nextInt(OPERATORS.length)]));
        generateTree(tree, currentDepth + 1, maxDepth, full);
        generateTree(tree, currentDepth + 1, maxDepth, full);
    }

    private static String randomTerminal() {
        if (RNG.nextBoolean()) {
            return "x" + RNG.nextInt(FEATURE_COUNT);
        }
        return Double.toString(RNG.nextDouble() * 10.0 - 5.0);
    }

    private static void evaluatePopulation(List<List<String>> population, double[] fitness) {
        for (int i = 0; i < population.size(); i++) {
            fitness[i] = evaluate(population.get(i), trainFeatures, trainLabels)[0];
        }
    }

    private static int bestIndex(double[] fitness) {
        int best = 0;
        for (int i = 1; i < fitness.length; i++) {
            if (fitness[i] > fitness[best]) {
                best = i;
            }
        }
        return best;
    }

    private static List<String> tournamentSelect(List<List<String>> population, double[] fitness) {
        int best = RNG.nextInt(population.size());
        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            int candidate = RNG.nextInt(population.size());
            if (fitness[candidate] > fitness[best]) {
                best = candidate;
            }
        }
        return population.get(best);
    }

    private static List<String> crossover(List<String> firstParent, List<String> secondParent) {
        List<int[]> childSpans = subtreeSpans(firstParent);
        List<int[]> donorSpans = subtreeSpans(secondParent);
        if (childSpans.size() <= 1) {
            return copy(firstParent);
        }

        int[] target = nonRootSpan(childSpans, firstParent.size());
        int[] donor = donorSpans.get(RNG.nextInt(donorSpans.size()));
        List<String> child = new ArrayList<String>();
        child.addAll(firstParent.subList(0, target[0]));
        child.addAll(secondParent.subList(donor[0], donor[1]));
        child.addAll(firstParent.subList(target[1], firstParent.size()));

        if (depth(child) > MAX_OFFSPRING_DEPTH) {
            return copy(firstParent);
        }
        return child;
    }

    private static void mutate(List<String> tree) {
        int index = RNG.nextInt(tree.size());
        String token = tree.get(index);
        if (isOperator(token)) {
            char replacement = token.charAt(0);
            while (replacement == token.charAt(0)) {
                replacement = OPERATORS[RNG.nextInt(OPERATORS.length)];
            }
            tree.set(index, Character.toString(replacement));
        } else if (token.startsWith("x")) {
            int current = Integer.parseInt(token.substring(1));
            int replacement = current;
            while (replacement == current) {
                replacement = RNG.nextInt(FEATURE_COUNT);
            }
            tree.set(index, "x" + replacement);
        } else {
            tree.set(index, Double.toString(RNG.nextDouble() * 10.0 - 5.0));
        }
    }

    private static double[] evaluate(List<String> tree, double[][] features, int[] labels) {
        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;
        for (int i = 0; i < features.length; i++) {
            int[] index = {0};
            int predicted = evaluateExpression(tree, features[i], index) > 0.0 ? 1 : 0;
            int actual = labels[i];
            if (predicted == 1 && actual == 1) {
                tp++;
            } else if (predicted == 0 && actual == 0) {
                tn++;
            } else if (predicted == 1) {
                fp++;
            } else {
                fn++;
            }
        }
        return metrics(tp, tn, fp, fn);
    }

    private static double evaluateExpression(List<String> tree, double[] features, int[] index) {
        String token = tree.get(index[0]++);
        if (isOperator(token)) {
            double left = evaluateExpression(tree, features, index);
            double right = evaluateExpression(tree, features, index);
            char op = token.charAt(0);
            if (op == '+') {
                return left + right;
            }
            if (op == '-') {
                return left - right;
            }
            if (op == '*') {
                return left * right;
            }
            return Math.abs(right) < 0.001 ? 1.0 : left / right;
        }
        if (token.startsWith("x")) {
            return features[Integer.parseInt(token.substring(1))];
        }
        return Double.parseDouble(token);
    }

    private static double[] metrics(int tp, int tn, int fp, int fn) {
        int total = tp + tn + fp + fn;
        double accuracy = total == 0 ? 0.0 : (tp + tn) / (double) total;
        double precision = tp + fp == 0 ? 0.0 : tp / (double) (tp + fp);
        double recall = tp + fn == 0 ? 0.0 : tp / (double) (tp + fn);
        double fMeasure = precision + recall == 0.0 ? 0.0 : 2.0 * precision * recall / (precision + recall);
        return new double[] {accuracy, precision, recall, fMeasure};
    }

    private static List<int[]> subtreeSpans(List<String> tree) {
        List<int[]> spans = new ArrayList<int[]>();
        collectSpan(tree, 0, spans);
        return spans;
    }

    private static int[] nonRootSpan(List<int[]> spans, int treeSize) {
        int[] span;
        do {
            span = spans.get(RNG.nextInt(spans.size()));
        } while (span[0] == 0 && span[1] == treeSize);
        return span;
    }

    private static int collectSpan(List<String> tree, int start, List<int[]> spans) {
        String token = tree.get(start);
        int end = start + 1;
        if (isOperator(token)) {
            end = collectSpan(tree, end, spans);
            end = collectSpan(tree, end, spans);
        }
        spans.add(new int[] {start, end});
        return end;
    }

    private static int depth(List<String> tree) {
        int[] index = {0};
        return depth(tree, index);
    }

    private static int depth(List<String> tree, int[] index) {
        String token = tree.get(index[0]++);
        if (!isOperator(token)) {
            return 1;
        }
        return 1 + Math.max(depth(tree, index), depth(tree, index));
    }

    private static String toExpression(List<String> tree) {
        int[] index = {0};
        return toExpression(tree, index);
    }

    private static String toExpression(List<String> tree, int[] index) {
        String token = tree.get(index[0]++);
        if (isOperator(token)) {
            String left = toExpression(tree, index);
            String right = toExpression(tree, index);
            return "(" + left + " " + token + " " + right + ")";
        }
        if (token.startsWith("x")) {
            return token;
        }
        return String.format(Locale.US, "%.4f", Double.parseDouble(token));
    }

    private static boolean isOperator(String token) {
        return token.length() == 1
                && (token.charAt(0) == '+'
                || token.charAt(0) == '-'
                || token.charAt(0) == '*'
                || token.charAt(0) == '%');
    }

    private static List<String> copy(List<String> source) {
        return new ArrayList<String>(source);
    }

    private static String trim(String text) {
        return text.length() <= 200 ? text : text.substring(0, 197) + "...";
    }

    private static double[][] loadFeatures(Path path) throws IOException {
        List<double[]> rows = new ArrayList<double[]>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("CSV file is empty: " + path);
            }
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != FEATURE_COUNT + 1) {
                    throw new IllegalArgumentException("Expected 10 columns in " + path + " but found " + parts.length);
                }
                double[] row = new double[FEATURE_COUNT];
                for (int i = 0; i < FEATURE_COUNT; i++) {
                    row[i] = Double.parseDouble(parts[i + 1].trim());
                }
                rows.add(row);
            }
        }
        return rows.toArray(new double[rows.size()][]);
    }

    private static int[] loadLabels(Path path) throws IOException {
        List<Integer> labels = new ArrayList<Integer>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("CSV file is empty: " + path);
            }
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    labels.add(Integer.parseInt(line.split(",")[0].trim()));
                }
            }
        }
        int[] result = new int[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            result[i] = labels.get(i);
        }
        return result;
    }

    private static void saveModelSection(String section, List<String> model) throws IOException {
        List<String> lines = Files.exists(Paths.get(MODEL_FILE))
                ? Files.readAllLines(Paths.get(MODEL_FILE), StandardCharsets.UTF_8)
                : new ArrayList<String>();
        List<String> updated = new ArrayList<String>();
        boolean skipping = false;
        for (String line : lines) {
            if (line.startsWith("[") && line.endsWith("]")) {
                skipping = line.equals(section);
            }
            if (!skipping) {
                updated.add(line);
            }
        }
        if (!updated.isEmpty() && !updated.get(updated.size() - 1).trim().isEmpty()) {
            updated.add("");
        }
        updated.add(section);
        updated.add(join(model));
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(MODEL_FILE), StandardCharsets.UTF_8)) {
            for (String line : updated) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static List<String> loadModelSection(String section) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(MODEL_FILE), StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals(section)) {
                if (i + 1 >= lines.size() || lines.get(i + 1).trim().isEmpty()) {
                    throw new IllegalArgumentException("Missing model data for section " + section);
                }
                String[] tokens = lines.get(i + 1).trim().split("\\s+");
                List<String> model = new ArrayList<String>();
                for (String token : tokens) {
                    model.add(token);
                }
                return model;
            }
        }
        throw new IllegalArgumentException("Section not found in " + MODEL_FILE + ": " + section);
    }

    private static String join(List<String> tokens) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(tokens.get(i));
        }
        return builder.toString();
    }
}
