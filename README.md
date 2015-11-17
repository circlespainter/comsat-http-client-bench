# `comsat-http-client-bench`

This is a set of highly concurrent load test clients built with [JBender](https://github.com/pinterest/jbender) (which uses [Quasar](http://www.paralleluniverse.co/quasar/)) and a test server.

## Quickstart

1. Build all the executables with `gradle fatCapsule` (uses [Capsule](http://capsule.io)).
2. In one terminal/machine run the server with `java -jar server/build/libs/server-fatcap.jar server server/conf.yml`.
3. In another terminal/machine run the load tester with `java -jar ${TECH}/build/libs/${TECH}-fatcap.jar` to see the available options. At the very one of `-v`, `-r` or `-n` must be provided.

Currently `${TECH}` can be one of the following:

* `thread-apache`
* `fiber-apache`
* `thread-jersey`
* `fiber-jersey`
* `thread-okhttp`
* `fiber-okhttp`

## Notes

* `jersey` support configuring the HTTP connector with the additional `-Dcapsule.jvm.args="-Djersey.provider=<class name>`. The following short identifiers are recognised as well: `jetty` (default), `grizzly`, `jdk`, `apache`.
* Beside the built-in system resources monitoring (disabled by default), JFR can be used ad es. with the additional `-Dcapsule.jvm.args="-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,dumponexitpath=${TECH}.jfr"` flag to the `java command`.
