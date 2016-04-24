(defproject thread-cljhttp "0.1.0-SNAPSHOT"
  :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/snapshots"}]]
  :plugins [[lein-capsule "0.2.0"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http/clj-http "2.0.0"]
                 [co.paralleluniverse.comsat.bench.http.client/clients "0.1.0-SNAPSHOT"]]
  :jvm-opts ["-XX:+AggressiveOpts" "-XX:-UseGCOverheadLimit"]
  :main Main
  :aot :all
  :capsule {
    :capsule-version "1.0.2-SNAPSHOT"
    :types {
      :fat {}
    }
    :application {
      :name "thread-cljhttp"
    }
  }
)
