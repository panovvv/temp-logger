![screenshot](https://github.com/panovvv/temp-logger/raw/master/img/temp_logger_util_3.PNG?raw=true)


To build an executable jar, run
```bash
mvn clean compile assembly:single
```

The jar executable will be created in _/temp-logger/target/temp-logger-1.0-SNAPSHOT-jar-with-dependencies.jar_

You can now run the resulting jar file with
```bash
java -jar target/temp-logger-1.0-SNAPSHOT-jar-with-dependencies.jar
```
