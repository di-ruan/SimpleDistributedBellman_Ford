JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	neighbor.java \
	distance.java \
    content.java \
    SenderThread.java \
    CommandThread.java \
    bfclient.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
