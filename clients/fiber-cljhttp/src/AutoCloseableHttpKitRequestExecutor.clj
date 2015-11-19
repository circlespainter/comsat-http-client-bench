(ns AutoCloseableHttpKitRequestExecutor
   (:require
      [co.paralleluniverse.fiber.httpkit.client :as http]
      [co.paralleluniverse.pulsar.core :as p])
   (:gen-class
      :extends co.paralleluniverse.comsat.bench.http.client.AutoCloseableRequestExecutor
      :state s
      :init init
      :constructors {[java.lang.Integer] []}))

(defn -init [timeout] [[] timeout])

(p/defsfn -execute0 [this _ req]
   (let [timeout (.s this)
         res (http/request (merge req {:timeout timeout :follow-redirects false}))
         status (:status res)]
      (when-not (contains? #{200 204} status)
         (throw (AssertionError. (str "\"Request didn't complete successfully: " status))))
      res))
