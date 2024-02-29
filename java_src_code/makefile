# Define the Java compiler and compiler flags
JAVAC = javac
JAVA = java
JAVAC_FLAGS = 
JAVA_FLAGS =   

# Target to compile both IndexEngine and DocFinder classes
all: IndexEngine.class GetDoc.class BooleanAnd.class BM25Ranking.class PorterStemmer.class Retrieval.class 

SRC_DIR = component

# Rule to compile IndexEngine
IndexEngine.class: $(SRC_DIR)/IndexEngine/IndexEngine.java
	$(JAVAC) $(JAVAC_FLAGS) $<

# Rule to compile GetDoc
GetDoc.class: $(SRC_DIR)/GetDoc/GetDoc.java
	$(JAVAC) $(JAVAC_FLAGS) $<

# Rule to compile BooleanAnd
BooleanAnd.class: $(SRC_DIR)/BooleanAnd/BooleanAnd.java
	$(JAVAC) $(JAVAC_FLAGS) $^

# Rule to compile BM25Ranking
BM25Ranking.class: $(SRC_DIR)/Ranking/BM25Ranking.java
	$(JAVAC) $(JAVAC_FLAGS) $^

# Rule to compile Stemmer
PorterStemmer.class: $(SRC_DIR)/Stemmer/PorterStemmer.java
	$(JAVAC) $(JAVAC_FLAGS) $^

# Rule to compile Retrieval
Retrieval.class: $(SRC_DIR)/Retrieval/Retrieval.java
	$(JAVAC) $(JAVAC_FLAGS) $^
	
# Rule to clean up compiled .class files
clean:
	rm -f $(SRC_DIR)/IndexEngine/*.class
	rm -f $(SRC_DIR)/GetDoc/*.class
	rm -f $(SRC_DIR)/BooleanAnd/*.class
	rm -f $(SRC_DIR)/Ranking/*.class
	rm -f $(SRC_DIR)/Stemmer/*.class
	rm -f $(SRC_DIR)/Retrieval/*.class
