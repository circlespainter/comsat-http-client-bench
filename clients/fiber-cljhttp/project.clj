(defproject fiber-cljhttp "0.1.0-SNAPSHOT"
  :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/snapshots"}]]
  :plugins [[lein-capsule "0.2.0"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [co.paralleluniverse/quasar-core "0.7.4-SNAPSHOT"]
                 [co.paralleluniverse/pulsar "0.7.4-SNAPSHOT"]
                 [co.paralleluniverse/comsat-httpkit "0.5.0"]
                 [co.paralleluniverse.comsat.bench.http.client/clients "0.1.0-SNAPSHOT"]]
  :java-agents [[co.paralleluniverse/quasar-core "0.7.4-SNAPSHOT"]] ; TODO check lein-capsule
  :jvm-opts ["-XX:+AggressiveOpts" "-XX:-UseGCOverheadLimit"]
  :main Main
  :aot :all
  :capsule {
    :capsule-version "1.0.2-SNAPSHOT"
    :name "fiber-cljhttp-fatcap.jar" ; TODO check lein-capsule
    :types {
      :fat {}
    }
    :application {
      :name "fiber-cljhttp"
    }
    :runtime {
      :system-properties {
        "co.paralleluniverse.fibers.detectRunawayFibers" "false" ; TODO check lein-capsule
      }
    }
  }
)
