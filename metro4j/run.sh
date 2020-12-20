mvn clean package
cp target/metro4j-1.0-SNAPSHOT.jar .
java -Djava.library.path=/Users/jschultz/Source/wxde/metro4j -classpath "json-20160212.jar:target/metro4j-1.0-SNAPSHOT.jar" metro4j.MetroSelfTest
