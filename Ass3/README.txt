COS314 Assignment 3 - GP Classifier

Run all commands below from inside the Ass3 folder:
  cd Ass3

Files:
  src/ArithmeticGP.java
  src/DecisionTreeGP.java
  src/RunExperiment.java
  csv/Breast_train.csv
  csv/Breast_test.csv
  best_models.txt

Compilation:
  make

Interactive training/testing:
  make arithmetic
  make decision

When running either interactive program, enter:
  Enter seed: 1
  Enter train CSV path: csv/Breast_train.csv
  Enter test CSV path: csv/Breast_test.csv
  Mode (train/test): train

Use train when you want to evolve a new classifier and save it into best_models.txt.

To test a classifier that is already saved in best_models.txt, enter:
  Enter seed: 1
  Enter train CSV path: csv/Breast_train.csv
  Enter test CSV path: csv/Breast_test.csv
  Mode (train/test): test

When prompted, use:
  csv/Breast_train.csv
  csv/Breast_test.csv

30-run experiment and paired t-test:
  make experiment

When running make experiment, enter:
  Enter train CSV path: csv/Breast_train.csv
  Enter test CSV path: csv/Breast_test.csv

The experiment runs both algorithms 30 times, prints the comparison table,
performs the paired t-test, and saves the best arithmetic and decision-tree
models into best_models.txt.

Cleanup:
  make clean

Saved models after training:
  best_models.txt
  [ARITHMETIC] stores the best arithmetic expression tree.
  [DECISION_TREE] stores the best decision-tree classifier.

Dataset files:
  csv/Breast_train.csv is the labelled data used during evolution.
  csv/Breast_test.csv is unseen labelled data used only to measure how well the saved classifier generalizes.

Design Decisions:
  Initial tree depth:       4
  Max offspring depth:      8
  Selection:                Tournament (size 5)
  Crossover rate:           90%
  Mutation rate:            10%
  Function set (Arithmetic): +, -, *, % (protected divide)
  Function set (DecisionTree): binary split node using <= threshold

Best seed for ArithmeticGP:
  18

Best seed for DecisionTreeGP:
  11
