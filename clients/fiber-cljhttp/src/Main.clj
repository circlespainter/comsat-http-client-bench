(ns Main
  (:use FiberHttpKitEnv)
  (:import (co.paralleluniverse.comsat.bench.http.client ClientBase))
  (:gen-class
    :extends co.paralleluniverse.comsat.bench.http.client.ClientBase
    :main
    true))

(defn -setupEnv [_ _]
  (FiberHttpKitEnv.))

(defn -main [& args]
  (.run (Main.) (into-array String args) (ClientBase/DEFAULT_FIBERS_SF)))
