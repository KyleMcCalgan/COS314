# COS314 Assignment 3 Demo Guide

This document is a short explanation of what the system does, how the main functions work, and how to explain it during a demo.

## 1. What the project is doing

The assignment compares two forms of Genetic Programming for breast-cancer recurrence classification:

1. `ArithmeticGP`
   - Evolves mathematical expressions such as `((x5 * x8) - 2.1)`.
   - If the expression result is greater than `0`, it predicts class `1`.
   - Otherwise it predicts class `0`.

2. `DecisionTreeGP`
   - Evolves decision trees such as `if x8 <= 0.4 then 0 else 1`.
   - It follows the tree until it reaches a leaf labelled `0` or `1`.

Both algorithms solve the same classification task using the same dataset. The point of the assignment is to compare whether evolved arithmetic expressions or evolved decision trees classify the data better.

## 2. What the CSV files are for

The CSV files are the dataset:

- `csv/Breast_train.csv`
  - Used during evolution.
  - Every candidate model is scored on this file.

- `csv/Breast_test.csv`
  - Not used to evolve the models.
  - Used after training to measure how well the saved model generalizes to unseen data.

Each row contains:

- column 1: the true class label
  - `0` = no recurrence
  - `1` = recurrence
- columns 2-10: the 9 input features

Quick explanation for a demo:

> The training CSV teaches the evolutionary process what a good classifier looks like. The test CSV checks whether the final classifier works on data it did not evolve against.

## 3. How the models are stored

The trained models are stored in `best_models.txt`.

It contains two labelled sections:

- `[ARITHMETIC]`
- `[DECISION_TREE]`

The text after each label is a compact prefix representation of the best evolved model. This lets `test` mode reload the saved model without evolving it again.

Example idea:

```text
[ARITHMETIC]
+ * x5 x8 -2.1
```

This means the arithmetic expression tree is stored as tokens rather than as Java objects.

## 4. Why Genetic Programming is used

Normal genetic algorithms often evolve fixed-length values, such as bit strings or parameter arrays.

Genetic Programming instead evolves actual programs or tree structures:

- an arithmetic expression tree
- or a decision tree

The GP loop is:

1. Create many random candidate trees.
2. Score them using training accuracy.
3. Prefer better trees when choosing parents.
4. Create new trees with crossover and mutation.
5. Keep repeating for 100 generations.
6. Save the best tree found.

The important idea is that the system is not directly programmed with the final classifier. It searches for a classifier by evolving candidate programs.

## 5. The main training flow

Both `ArithmeticGP` and `DecisionTreeGP` use the same high-level flow.

### `main(...)`

This reads the user's inputs:

- seed
- train CSV path
- test CSV path
- mode: `train` or `test`

If mode is `train`, it calls:

```java
runTraining(...)
```

If mode is `test`, it loads the saved model from `best_models.txt` and evaluates it on the test CSV.

### `runTraining(...)`

This is the heart of the program.

It:

1. sets the random seed so results can be repeated
2. loads the train and test CSV files
3. creates the initial population
4. runs 100 generations
5. tracks the best model seen
6. evaluates final training and test performance
7. saves the best model into `best_models.txt`

Quick explanation:

> `runTraining` manages the entire evolutionary process from random starting trees to the final saved classifier.

## 6. How one generation works

Inside `runTraining`, each generation does this:

1. `evaluatePopulation(...)`
   - Scores every candidate on training accuracy.

2. `bestIndex(...)`
   - Finds the strongest candidate in the current generation.

3. Keep a copy of that best candidate
   - This is elitism.
   - It prevents the best solution from being lost in the next generation.

4. Build the next generation
   - `tournamentSelect(...)` chooses strong parents.
   - `crossover(...)` combines parts of two parent trees.
   - `mutate(...)` randomly changes a node.

5. Repeat until a new population of 200 individuals exists.

### Why this works

- Selection pushes the population toward better solutions.
- Crossover mixes useful building blocks from different trees.
- Mutation introduces new variation so the search does not become too narrow.
- Elitism protects the best result found so far.

## 7. Important GP functions

### `initializePopulation()`

Creates the first 200 random trees.

It uses ramped half-and-half initialization:

- some trees are deliberately full
- some trees are allowed to stop earlier
- several starting depths are used

Purpose:

> Start with a varied population instead of 200 near-identical trees.

### `generateTree(...)`

Builds one random tree recursively.

For `ArithmeticGP`:

- internal nodes are operators: `+`, `-`, `*`, `%`
- leaves are variables such as `x3` or random constants

For `DecisionTreeGP`:

- internal nodes are split rules such as `S:8:0.42`
- leaves are class labels such as `L:0` or `L:1`

### `tournamentSelect(...)`

Randomly samples 5 individuals and returns the best one.

Purpose:

> Better models are more likely to become parents, but weaker models still sometimes survive, which keeps diversity in the search.

### `crossover(...)`

Takes one subtree from parent A and swaps in a subtree from parent B.

Purpose:

> Recombine useful partial solutions. For example, one tree may contain a helpful condition on `x8`, while another may contain a useful condition on `x5`.

If the child becomes too deep, the code rejects it and keeps a copy of the parent instead.

### `mutate(...)`

Changes one random node.

For `ArithmeticGP`, mutation may:

- change an operator
- change a feature index
- change a constant

For `DecisionTreeGP`, mutation may:

- change the split feature
- change the threshold
- flip a leaf class

Purpose:

> Mutation creates new possibilities that crossover alone may never produce.

### `evaluate(...)`

Tests one candidate model on every row in a dataset and calculates:

- accuracy
- precision
- recall
- F-measure

The assignment uses accuracy as the fitness value during evolution.

## 8. How the two model types differ

### Arithmetic GP

Representation:

- mathematical expression tree

Prediction rule:

```text
expression result > 0 -> class 1
otherwise -> class 0
```

Strength:

- can build flexible mathematical relationships between variables

Weakness:

- expressions can become large and difficult to interpret

### Decision Tree GP

Representation:

- sequence of feature splits and leaf labels

Prediction rule:

```text
follow each split until a leaf is reached
```

Strength:

- easier to explain because it behaves like a chain of if-statements

Weakness:

- may need many branches to express more complex relationships

## 9. What `RunExperiment` does

`RunExperiment` performs the assignment's 30 independent runs.

For seeds `1` to `30`, it:

1. trains `ArithmeticGP`
2. trains `DecisionTreeGP`
3. records both test accuracies

After all 30 runs, it:

1. prints the best run for each method
2. performs a paired t-test using `pairedTStatistic(...)`
3. retrains the best seed for each method
4. saves those two best models into `best_models.txt`

### `pairedTStatistic(...)`

This compares the paired test accuracies from the two methods.

For each seed:

```text
difference = arithmetic accuracy - decision-tree accuracy
```

It then calculates whether the average difference is large enough to be considered statistically significant.

In the current experiment:

- Arithmetic best seed: `18`
- Decision tree best seed: `11`
- Paired t-statistic: `-4.5559`
- Conclusion: statistically significant difference at `p < 0.05`

Because the statistic is negative, the decision-tree method performed better overall across the paired runs.

## 10. Demo-ready explanation

If someone asks, "What happens when you run the system?", a good short answer is:

> We load the breast-cancer training data, create 200 random candidate classifiers, and evolve them for 100 generations. Each generation scores candidates by training accuracy, selects strong parents with tournament selection, creates offspring through crossover and mutation, and keeps the best model found so far. We do this once for arithmetic-expression classifiers and once for decision-tree classifiers, then compare their test accuracy on unseen data.

If someone asks, "Why run it 30 times?", say:

> GP is stochastic, so one run can be lucky or unlucky. Running 30 different seeds gives a fairer comparison, and the paired t-test tells us whether the observed difference is likely meaningful rather than random noise.

If someone asks, "Why save the models?", say:

> Training is expensive. Saving the best evolved tree lets test mode reload the final classifier immediately instead of evolving it again.

## 11. Commands for the demo

From inside `Ass3`:

```bash
make experiment
```

Inputs:

```text
csv/Breast_train.csv
csv/Breast_test.csv
```

To train one arithmetic model:

```bash
make arithmetic
```

Example inputs:

```text
18
csv/Breast_train.csv
csv/Breast_test.csv
train
```

To test the saved arithmetic model:

```bash
make arithmetic
```

Example inputs:

```text
18
csv/Breast_train.csv
csv/Breast_test.csv
test
```

The same pattern applies to `make decision`.
