JVMFLAGS = -cp build
JCFLAGS = -cp src -d build
JC = javac
JVM= java 
JVMFLAGS = -cp build
CLASSES=$(wildcard *.java)

MAIN=$(PKG).MNKGame 
M:=4
N:=4
K:=3
BEST:=S

.PHONY: build

random: build
	$(JVM) $(JVMFLAGS) $(MAIN) $(M) $(N) $(K) mnkgame.$(BEST)

build: $(CLASSES)
	mkdir -p build
	$(JC) $(JCFLAGS) src/*.java src/*.java

clean:
	rm -rf build
