# `comsat-http-client-bench`

This is a set of highly concurrent load test clients built with [JBender](https://github.com/pinterest/jbender) (which uses [Quasar](http://www.paralleluniverse.co/quasar/)) and a test server.

The code in this project complements [the corresponding benchmarking blog post](http://blog.paralleluniverse.co/2015/12/02/http-clients/).

## Quickstart - Java

1. Build all the executables with `./gradlew fatCapsule` (on Unix, uses [Capsule](http://capsule.io)).
2. In one terminal/machine run the server with `java -jar server/build/libs/server-fatcap.jar server server/conf.yml`.
3. In another terminal/machine run the load tester with `java -jar clients/${TECH}/build/libs/${TECH}-fatcap.jar` to see the available options. At the very one of `-v`, `-r` or `-n` must be provided.

`${TECH}` can be one of the following:

* `thread-apache`
* `fiber-apache`
* `thread-jersey`
* `fiber-jersey`
* `thread-okhttp`
* `fiber-okhttp`

## Quickstart - Clojure

1. Install the other Gradle-based project modules with `./gradlew install`.
2. Build the Clojure capsules (on Unix):

``` bash
cd clients/thread-cljhttp
./lein capsule
cd ../fiber-cljhttp
./lein capsule
cd ../..
```

3. In one terminal/machine run the server with `java -jar server/build/libs/server-fatcap.jar server server/conf.yml`.

4. In another terminal/machine run the load tester with `java -jar clients/${TECH}/target/capsules/${TECH}-0.1.0-SNAPSHOT-capsule.jar` to see the available options. At the very one of `-v`, `-r` or `-n` must be provided.

`${TECH}` can be one of the following:

* `thread-cljhttp`
* `fiber-cljhttp`

## Notes

* `jersey` (Java) support configuring the HTTP connector with the additional `-Dcapsule.jvm.args="-Djersey.provider=<class name>"`. The following short identifiers are recognised as well: `jetty` (default), `grizzly`, `apache`, `jdk`.
* Besides the built-in system resources monitoring (disabled by default), JFR can be used ad es. with the additional `-Dcapsule.jvm.args="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=${TECH}.jfr"` flag to the `java command`.

## Examples

* `-u http://${IP}:9000/?sleepMS=0 -c 11000 -r 100000` will push the client to reach the highest rate up to 100k rps.
* `-u http://${IP}:9000/?sleepMS=3600000 -c 46000 -n 45000` will fire up up to 45k connections that will be answered by the server only after 1h (sustainable only by fiber-based testers).
