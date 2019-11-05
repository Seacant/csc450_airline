You need to manually install ojdbc8 because Oracle is a twat.

Get the file from a legal-ish place and put it in `~/airline/lib/ojdbc8.java`. DO NOT COMMIT IT. I don't want sued

Then, 
```
    cd airline/
    mvn install:install-file -Dfile="lib/ojdbc8.jar" -DgroupId="com.oracle" -DartifactId=ojdbc8 -Dversion="12.2.0.1" -Dpackaging=jaro
```

Then, you need to install the project through maven. Make sure you're in `airline/` for any maven command
```
    mvn install
```

To run, either use your IDE's built-in method or use 
```
    mvn compile exec:java
```