# COS314 Assignment 3 — GP Implementation Guide

> This document explains the full implementation plan for both GP variants required by the assignment.  
> Target language: **Java** (single JAR, no external libraries).  
> The AI implementing this should produce clean, minimal-comment, single-file-per-program Java code consistent with a university submission.

---

## 0. Big Picture

You are building **two separate GP systems** that both solve the same binary classification problem (breast cancer recurrence: 0 or 1).

| System | What evolves | How it classifies |
|---|---|---|
| **GP-Arithmetic** | A mathematical expression over the 9 input features | `expression(x) > 0 → class 1, else class 0` |
| **GP-DecisionTree** | A decision tree built from if/then splits on features | Walk the tree, reach a leaf, output 0 or 1 |

Both systems share the same GP engine skeleton (population, selection, crossover, mutation, fitness). What differs is the **node types** and **tree interpretation**.

Each system needs **two runnable modes**:
- `--train` → run evolution, print best individual + metrics each generation, save best model
- `--test` → load saved model, classify test CSV, print metrics

---

## 1. Dataset

**Files:** `Breast_train.csv` (183 rows) and `Breast_test.csv` (86 rows).  
**Columns (in order):** `class, age, menopause, tumor_size, inv_nodes, node_caps, deg_malig, breast, breast_quad, irradiat`  
- First row is a header — skip it.
- `class` is column index 0. Features are columns 1–9.
- All values are integers (already encoded per the appendix).
- No missing/NaN values present (? already mapped to integers).

**Loading:** Read with a simple `BufferedReader`, split on comma, parse to `double[]` for features and `int` for class label.

```
double[][] X_train   // shape [183][9]
int[]      y_train   // shape [183]
double[][] X_test    // shape [86][9]
int[]      y_test    // shape [86]
```

Feature indices (0-based into the feature array, i.e., column - 1):
- 0 = age, 1 = menopause, 2 = tumor_size, 3 = inv_nodes, 4 = node_caps
- 5 = deg_malig, 6 = breast, 7 = breast_quad, 8 = irradiat

---

## 2. Shared GP Infrastructure

Both variants share this core. Build it once as reusable abstract structure.

### 2.1 Node (Abstract Tree Node)

Every tree is composed of `Node` objects.

```java
abstract class Node {
    Node left, right;          // children (null for leaves)
    abstract double evaluate(double[] features);
    abstract Node deepCopy();
    abstract String toExpression();
    abstract int depth();
    abstract int size();        // number of nodes
}
```

### 2.2 Random Utilities

Use a single `Random rng` seeded from user input. Pass it everywhere — never use `Math.random()`.

```java
Random rng = new Random(seed);
```

### 2.3 Population

```java
Node[] population = new Node[200];
```

Initialize with **ramped half-and-half**: divide the 200 individuals evenly across depths 2..MAX_INIT_DEPTH. For each depth level, half use the **full** method (all leaves at exactly that depth), half use the **grow** method (leaves can terminate early with some probability).

```
MAX_INIT_DEPTH = 4  (design decision — reasonable default)
```

**Full method:** at each node, if current depth < max depth, always pick a function node; at max depth, always pick a terminal.  
**Grow method:** at each node, if current depth < max depth, randomly pick function or terminal; at max depth, always pick a terminal.

### 2.4 Fitness Function

Fitness = **accuracy** on the training set.

```
accuracy = correct_predictions / total_instances
```

Higher is better. Store as a `double` in [0.0, 1.0].

### 2.5 Selection

Use **tournament selection** with tournament size = 5 (design decision).

```
Pick 5 random indices from the population.
Return the individual with the highest fitness.
```

### 2.6 Crossover (Subtree Crossover)

Rate: **90%** (design decision).

```
1. Pick a random crossover point in parent1 (any node except root).
2. Pick a random crossover point in parent2.
3. Swap the subtrees rooted at those points.
4. If offspring depth > MAX_OFFSPRING_DEPTH, return a copy of the parent instead.
```

`MAX_OFFSPRING_DEPTH = 8` (design decision).

To pick a random node: collect all nodes into a list via tree traversal, pick a random index.

### 2.7 Mutation (Point Mutation)

Rate: **10%** (design decision). Type: **point**.

Point mutation: pick a random node in the tree. If it's a function node, replace it with a different random function node of the same arity. If it's a terminal, replace it with a different random terminal.

Do NOT regrow a subtree — just swap the node type in-place, children are preserved.

### 2.8 Generational Loop

```
for gen in 1..100:
    evaluate fitness of all individuals
    record best individual (highest fitness)
    print: generation, best fitness, best expression (trimmed if long)
    
    new_population = []
    while len(new_population) < 200:
        if rng.nextDouble() < crossover_rate:
            p1 = tournament_select()
            p2 = tournament_select()
            child = crossover(p1, p2)
        else:
            child = tournament_select().deepCopy()
        
        if rng.nextDouble() < mutation_rate:
            child = mutate(child)
        
        new_population.add(child)
    
    population = new_population

return best individual seen across all generations
```

Use **elitism**: always copy the best individual from the previous generation into the new population unchanged (prevents regression).

### 2.9 Metrics

```java
// After classifying all instances:
int TP = 0, TN = 0, FP = 0, FN = 0;
for each instance:
    predicted = classify(individual, features)
    actual    = label
    // update TP/TN/FP/FN accordingly

double accuracy  = (TP + TN) / (double) total;
double precision = TP / (double)(TP + FP);   // guard against /0
double recall    = TP / (double)(TP + FN);   // guard against /0
double fMeasure  = 2 * precision * recall / (precision + recall);
long   runtime   = endTime - startTime;      // milliseconds
```

---

## 3. GP-Arithmetic (Symbolic Classifier)

### 3.1 What It Is

Each individual is a **mathematical expression tree**. Leaf nodes are either feature references or constants. Internal nodes are arithmetic operators. The tree evaluates to a real number. Classification threshold: if result > 0 → class 1, else → class 0.

### 3.2 Node Types

**Function nodes (internal, arity 2):**
| Symbol | Operation | Note |
|---|---|---|
| `+` | left + right | |
| `-` | left - right | |
| `*` | left * right | |
| `%` | protected divide | if abs(right) < 0.001, return 1.0, else left/right |

**Terminal nodes (leaves):**
- **Feature terminal:** holds an index 0–8, evaluates to `features[index]`
- **Constant terminal:** holds a `double` constant, randomly chosen from range [-5.0, 5.0] at creation

```java
class FunctionNode extends Node {
    char op;  // '+', '-', '*', '%'
    // evaluate: apply op to left.evaluate() and right.evaluate()
}

class FeatureNode extends Node {
    int featureIndex;  // 0..8
    // evaluate: return features[featureIndex]
}

class ConstantNode extends Node {
    double value;
    // evaluate: return value
}
```

### 3.3 Random Node Generation

```java
Node randomFunctionNode() {
    char op = one of {'+', '-', '*', '%'} chosen uniformly
    return new FunctionNode(op)  // children assigned by caller
}

Node randomTerminalNode() {
    if rng.nextBoolean():
        return new FeatureNode(rng.nextInt(9))
    else:
        return new ConstantNode(rng.nextDouble() * 10 - 5)
}
```

### 3.4 Classification

```java
int classify(Node tree, double[] features) {
    double result = tree.evaluate(features);
    return result > 0.0 ? 1 : 0;
}
```

### 3.5 Expression Output (toExpression)

For printing the best individual each generation:

```
FunctionNode('+', A, B) → "(A + B)"
FeatureNode(2)          → "x2"
ConstantNode(3.14)      → "3.14"
```

Trim/truncate the string if it exceeds ~200 characters for readable console output.

---

## 4. GP-DecisionTree (Logical Classifier)

### 4.1 What It Is

Each individual is a **decision tree**. Internal nodes contain a **split condition** on a feature (feature[i] <= threshold). Leaf nodes contain a **class label** (0 or 1). Classification: walk the tree, at each internal node go left if condition is true, right if false, return the label at the leaf reached.

### 4.2 Node Types

**Internal node (function node, arity 2):**
```java
class SplitNode extends Node {
    int featureIndex;    // 0..8
    double threshold;    // a value appropriate for that feature's range
    // left child: taken when features[featureIndex] <= threshold
    // right child: taken otherwise
    // evaluate: not really used — classification uses walkTree()
}
```

**Leaf node (terminal):**
```java
class LeafNode extends Node {
    int label;  // 0 or 1
    // evaluate: return (double) label
}
```

> **Note:** `evaluate()` is less natural here. For the fitness function, implement a separate `walkTree(Node, features[])` method that returns `int` directly.

### 4.3 Threshold Generation

When creating a `SplitNode`, pick a sensible threshold for the chosen feature. Use the actual range of that feature observed in the training data, then pick a random value in that range.

Feature value ranges (from the dataset encoding):

| Feature index | Range |
|---|---|
| 0 (age) | 0–5 |
| 1 (menopause) | 0–2 |
| 2 (tumor_size) | 0–8 (approximate) |
| 3 (inv_nodes) | 0–8 (approximate) |
| 4 (node_caps) | 0–2 |
| 5 (deg_malig) | 1–3 |
| 6 (breast) | 0–1 |
| 7 (breast_quad) | 0–5 |
| 8 (irradiat) | 0–2 |

```java
double randomThreshold(int featureIndex) {
    double[] mins = {0,0,0,0,0,1,0,0,0};
    double[] maxs = {5,2,8,8,2,3,1,5,2};
    return mins[featureIndex] + rng.nextDouble() * (maxs[featureIndex] - mins[featureIndex]);
}
```

### 4.4 Random Node Generation

```java
Node randomFunctionNode() {
    int fi = rng.nextInt(9);
    return new SplitNode(fi, randomThreshold(fi))
}

Node randomTerminalNode() {
    return new LeafNode(rng.nextInt(2))  // class 0 or 1
}
```

### 4.5 Classification

```java
int classify(Node node, double[] features) {
    if (node instanceof LeafNode):
        return ((LeafNode) node).label;
    SplitNode split = (SplitNode) node;
    if features[split.featureIndex] <= split.threshold:
        return classify(split.left, features)
    else:
        return classify(split.right, features)
}
```

### 4.6 Expression Output (toExpression)

```
SplitNode(featureIndex=2, threshold=4.5):
    "(x2<=4.5 ? <left> : <right>)"

LeafNode(1):
    "1"
```

---

## 5. Point Mutation — Details per Variant

### Arithmetic Mutation

- Select a random node.
- If `FunctionNode`: replace `op` with a different random op from {+, -, *, %}.
- If `FeatureNode`: replace `featureIndex` with a different random index in 0–8.
- If `ConstantNode`: replace `value` with a new random value in [-5, 5].

### DecisionTree Mutation

- Select a random node.
- If `SplitNode`: either change `featureIndex` to a new random feature (and re-randomize threshold), or keep feature and re-randomize threshold only.
- If `LeafNode`: flip the label (0→1, 1→0).

---

## 6. Model Serialization (Save/Load)

After training, save the best individual to a text file so the test program can load it.

**Arithmetic:** serialize the expression tree in prefix notation (pre-order traversal):
```
+ x2 * x0 3.14
```
Parse back by reading tokens recursively: if token is an operator, read two subtrees; if "x[n]", it's a feature node; else parse as double constant.

**DecisionTree:** serialize in prefix notation:
```
SPLIT 2 4.5 <left_subtree> <right_subtree>
LEAF 1
```

The test program takes the model file path as a command-line argument, loads the tree, runs it on the test CSV, and prints accuracy, F-measure.

---

## 7. Program Entry Points

### 7.1 Command-line Interface

Both programs (ArithmeticGP and DecisionTreeGP) accept the same CLI pattern:

```
java -jar ArithmeticGP.jar
  → prompts: Enter seed: 
  → prompts: Enter train CSV path:
  → prompts: Enter test CSV path:
  → prompts: Mode (train/test):
```

If `train`: run evolution on train CSV, print per-generation stats, evaluate on test CSV at end, save model to `arithmetic_best.model`.  
If `test`: load `arithmetic_best.model`, evaluate on test CSV only.

(Same pattern for DecisionTreeGP, saving to `dtgp_best.model`.)

### 7.2 Per-Generation Output Format

```
Gen 1  | Best Fitness: 0.7158 | Expression: (x2 + (x5 * 3.14))
Gen 2  | Best Fitness: 0.7268 | Expression: ((x2 + x0) * (x5 % 2.1))
...
Gen 100| Best Fitness: 0.8197 | Expression: ...

=== FINAL RESULTS ===
Seed: 42
Training Accuracy: 81.97%
Test Accuracy:     76.74%
F-Measure:         0.7812
Runtime:           4231 ms
Best Expression:   (full expression here)
```

---

## 8. 30-Run Harness

You need 30 independent runs with unique seeds for each algorithm. Recommended approach: a simple shell script or a third main class `RunExperiment.java` that:

1. Iterates seeds 1..30.
2. Calls the GP train logic programmatically (not via CLI) for each seed.
3. Records: seed, train accuracy, test accuracy, F-measure, runtime.
4. Tracks the best run (highest test accuracy).
5. After all 30 runs, prints a summary table and the best seed.

This best seed is what you report and demo.

---

## 9. Statistical Significance Test

After 30 runs for each algorithm, you have two arrays of test-accuracy values (30 each). Perform a **Wilcoxon signed-rank test** or **paired t-test** to compare them.

For Java without external libraries, implement the **paired t-test** manually:

```
differences[i] = arithmetic_accuracy[i] - dt_accuracy[i]   for i in 0..29
mean_diff = sum(differences) / 30
std_diff  = sqrt(sum((d - mean_diff)^2) / 29)
t_stat    = mean_diff / (std_diff / sqrt(30))
degrees_of_freedom = 29

// Compare t_stat against critical value table for df=29, alpha=0.05
// Two-tailed critical value ≈ 2.045
if abs(t_stat) > 2.045:
    "Statistically significant difference (p < 0.05)"
else:
    "No statistically significant difference"
```

Include the t-statistic, degrees of freedom, and conclusion in the report.

---

## 10. File Structure

```
COS314_Assignment3/
├── src/
│   ├── ArithmeticGP.java       ← single file, contains all inner classes
│   └── DecisionTreeGP.java     ← single file, contains all inner classes
├── Breast_train.csv
├── Breast_test.csv
├── arithmetic_best.model       ← generated after training
├── dtgp_best.model             ← generated after training
├── README.txt
└── report.pdf
```

### README.txt Contents

```
COS314 Assignment 3 — GP Classifier
Authors: [team names]

Compilation:
  javac ArithmeticGP.java
  jar cfe ArithmeticGP.jar ArithmeticGP ArithmeticGP.class
  (same for DecisionTreeGP)

Run:
  java -jar ArithmeticGP.jar
  java -jar DecisionTreeGP.jar

Best seed for ArithmeticGP:  [seed]
Best seed for DecisionTreeGP: [seed]

Design Decisions:
  Initial tree depth:       4
  Max offspring depth:      8
  Selection:                Tournament (size 5)
  Crossover rate:           90%
  Mutation rate:            10%
  Function set (Arith):     +, -, *, % (protected divide)
  Function set (DT):        SplitNode (<=)
```

---

## 11. Design Decisions Summary

| Parameter | Arithmetic GP | Decision Tree GP |
|---|---|---|
| Initial tree depth | 4 | 4 |
| Max offspring depth | 8 | 8 |
| Selection | Tournament (k=5) | Tournament (k=5) |
| Crossover rate | 90% | 90% |
| Mutation rate | 10% | 10% |
| Function set | +, -, *, % | SplitNode (feature <= threshold) |
| Terminal set | Feature vars (x0–x8), constants [-5,5] | LeafNode (class 0 or 1) |
| Classification | expression > 0 → 1, else 0 | tree walk to leaf |

---

## 12. Common Pitfalls to Avoid

- **Deep copy must be truly deep.** Every `deepCopy()` must recursively copy the entire subtree — not just copy the reference. Failure here causes crossover to corrupt the population.
- **Protected division.** Never let a divide-by-zero crash the program. Use `abs(denominator) < 1e-6 → return 1.0`.
- **Depth enforcement.** After crossover, always check offspring depth. If it exceeds `MAX_OFFSPRING_DEPTH`, discard and return a parent copy.
- **Elitism.** Without it, the best individual can be lost between generations.
- **Single `Random` instance.** Create it once with the seed, use it everywhere. Never `new Random()` inside helper methods.
- **Label order.** `class` is column 0 in the CSV, not column 9. Features are columns 1–9 (mapped to indices 0–8).
- **F-measure edge case.** If TP=0, both precision and recall are 0, so F=0. Guard the division.
- **Model file path.** Make the model output path either hardcoded or a parameter — the test mode needs to find the file the train mode produced.
