LIB_PATH=/home/cs557-inst/local/lib/libthrift-0.13.0.jar:/home/cs557-inst/local/lib/slf4j-api-1.7.30.jar:/home/cs557-inst/loca/lib/slf4j-log4j12-1.7.12.jar:/home/cs557-inst/local/lib/javax.annotation-api-1.3.2.jar
all: clean
	mkdir bin
	mkdir bin/client_classes
	mkdir bin/server_classes
	javac -classpath $(LIB_PATH) -d bin/client_classes/ src/JavaClient.java src/KeyValueHandler.java gen-java/*
	javac -classpath $(LIB_PATH) -d bin/server_classes/ src/JavaServer.java src/KeyValueHandler.java gen-java/*


clean:
	rm -rf bin/