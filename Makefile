.PHONY: clean jar tag outdated install deploy tree repl classes javac

CLJCP := $(shell clojure -Spath)

# all java sources (if any)
JAVA_SOURCES := $(shell find src/java -type f -name '*.java' 2>/dev/null)

clean:
	rm -rf target
	rm -rf classes

# compile java sources into classes/
classes: javac

javac:
	@mkdir -p classes
ifneq ($(strip $(JAVA_SOURCES)),)
	@echo "Compiling Java sources..."
	javac -d classes -cp "$(CLJCP):classes" $(JAVA_SOURCES)
else
	@echo "No Java sources found under src/java; skipping javac."
endif

jar: clean classes tag
	clojure -A:jar

outdated:
	clojure -M:outdated

tag:
	clojure -A:tag

install: jar
	clojure -A:install

deploy: jar
	clojure -A:deploy

tree:
	mvn dependency:tree

repl: classes
	clojure -A:dev -A:repl
