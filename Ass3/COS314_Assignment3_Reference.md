# COS314 — Artificial Intelligence  
## Assignment Three (30 Marks)

**University of Pretoria — Engineering, Built Environment and IT  
Department of Computer Science**

**Due: 13 May 2026**

---

## Instructions

- A CSV file containing the data for this assignment is attached.
- Only Java or C++ may be used to complete this assignment.
- The programs must be executable (JAR) and be able to run without linking to libraries via the IDE (in the case of C++).
- Read-me instructions are to be included in your submission.
- **NB: Both the Report and Code need to be submitted.**
- Submission is through ClickUP — no email submissions will be allowed.
- For evaluation and replication, all code must be seeded (e.g., listing 1) and run by initially requesting the seed value and necessary filepath and parameters.
- **You will be expected to demo your submission (a schedule will be issued).**
- **Assignment to be done in teams. Maintain the same teams as in Assignment 2.**

---

## 1. Overview

In this assignment, you will implement two distinct variations of Genetic Programming (GP) to classify the Breast Cancer Wisconsin (Diagnostic) Dataset. You are required to compare a "Symbolic" approach (using arithmetic operators) against a "Logical" approach (using evolved decision trees) and report on their classification performance.

---

## 2. Model Specifications (20 Marks)

### Genetic Programming Classification Algorithms

- The GP classification algorithm should evolve:
  - Arithmetic classifiers. *(10 marks)*
  - Decision Trees. *(10 marks)*
- `dd` = **design decisions** — parameters are your decision.

### Table 1: Genetic Programming Parameters

| Parameter | Value |
|---|---|
| Population size | 200 |
| Initial tree generation | ramped half-and-half |
| Initial tree depth | dd |
| Max offspring depth | dd |
| Selection method | dd |
| Tournament size | dd |
| Function set | dd |
| Crossover rate | dd% |
| Mutation rate | dd% |
| Mutation type | point |
| Mutation offspring depth | dd |
| Fitness function | accuracy |
| Maximum generations | 100 |

---

## 3. Evaluation and Submission Report (5 Marks)

For both models, you must submit two programs:

1. **Training Demonstration:** The programs must display the best individual (tree/expression) and its metrics at each generation of the evolution.
2. **Testing/Classification:** The programs must load and test the unseen test file and classify the instances using the best pre-trained/evolved model.

### Required Metrics

You must report the following metrics for both GP approaches:

- Training accuracy and Test accuracy.
- F-measure.

### Table 2: Comparison of Classification Performance

| Algorithm | Training (%) | Test (%) | F-measure | Runtime |
|---|---|---|---|---|
| Decision Tree | 0.00 | 0.00 | 0.00 | 0.00 |
| GP Classifier | 0.00 | 0.00 | 0.00 | 0.00 |

- **Statistical Significance:** Include a statistical significance test (e.g., T-test or Wilcoxon) to determine if there is a meaningful difference in performance between the arithmetic classifier and the decision tree.

### Submission Requirements

- A report in PDF format describing your models, pre-processing steps, and final results.
- A seed value must be reported to ensure results can be replicated during the demo.
- A summary results table comparing the two GP models.
- A reminder that for each algorithm, you need to perform **30 independent runs**, i.e. each run must use a unique seed value and the best performing run must be recorded, and this is what will be demonstrated on demo day.
- Presentation is worth 5 Marks.

---

## Appendix — Dataset Summary

**File:** `Breast_train.csv` (183 instances) / `Breast_test.csv` (86 instances)  
**Features:** 9 input features + 1 class label (10 columns total)

| Feature | Encoding |
|---|---|
| class | no-recurrence-events = 0, recurrence-events = 1 |
| age | 20–29 = 0, 30–39 = 1, 40–49 = 2, 50–59 = 3, 60–69 = 4, 70–79 = 5 |
| menopause | premeno = 0, ge40 = 1, lt40 = 2 |
| tumor_size | Standardized integer mappings for binned ranges |
| inv_nodes | Standardized integer mappings for binned ranges |
| node_caps | no = 0, yes = 1, ? = 2 |
| deg_malig | 1, 2, or 3 (degree of malignancy) |
| breast | left = 0, right = 1 |
| breast_quad | left low = 0, right up = 1, left up = 2, right low = 3, central = 4, ? = 5 |
| irradiat | no = 0, yes = 1, ? = 2 |
