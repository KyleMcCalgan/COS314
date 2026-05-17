JAVAC = javac
JAVA = java
JAR = jar

SRC_DIR = src
BUILD_DIR = build

SOURCES = $(SRC_DIR)/ArithmeticGP.java $(SRC_DIR)/DecisionTreeGP.java $(SRC_DIR)/RunExperiment.java

.PHONY: all build jars arithmetic decision experiment clean

all: jars

build:
	mkdir -p $(BUILD_DIR)
	$(JAVAC) -d $(BUILD_DIR) $(SOURCES)

jars: build
	$(JAR) cfe ArithmeticGP.jar ArithmeticGP -C $(BUILD_DIR) ArithmeticGP.class
	$(JAR) cfe DecisionTreeGP.jar DecisionTreeGP -C $(BUILD_DIR) DecisionTreeGP.class

arithmetic: jars
	$(JAVA) -jar ArithmeticGP.jar

decision: jars
	$(JAVA) -jar DecisionTreeGP.jar

experiment: build
	$(JAVA) -cp $(BUILD_DIR) RunExperiment

clean:
	rm -rf $(BUILD_DIR) ArithmeticGP.jar DecisionTreeGP.jar
