(defproject fiber-cljhttp "0.1.0-SNAPSHOT"
  :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/snapshots"}]]
  :plugins [[lein-capsule "0.2.1"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [co.paralleluniverse/quasar-core "0.7.4"]
                 [co.paralleluniverse/pulsar "0.7.4"]
                 [co.paralleluniverse/comsat-httpkit "0.7.0"]
                 [co.paralleluniverse.comsat.bench.http.client/clients "0.1.0-SNAPSHOT"]]
  :java-agents [[co.paralleluniverse/quasar-core "0.7.4"]]
  :jvm-opts ["-XX:+AggressiveOpts" "-XX:-UseGCOverheadLimit"]
  :main Main
  :aot :all
  :capsule {
    :types {
      :fat {}
    }
    :application {
      :name "fiber-cljhttp"
    }
    :execution {
      :runtime {
        :system-properties {
          "co.paralleluniverse.fibers.detectRunawayFibers" "false"
        }
      }
    }
  }
)
