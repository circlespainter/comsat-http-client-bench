(ns AutoCloseableCljHttpRequestExecutor
   (:require [clj-http.client :as http])
   (:gen-class
      :extends co.paralleluniverse.comsat.bench.http.client.AutoCloseableRequestExecutor
      :state s
      :init init
      :constructors
      {[org.apache.http.impl.conn.PoolingClientConnectionManager java.lang.Integer] []}))

(defn -init [cm timeout] [[] [cm timeout]])

(defn -execute0 [this _ req]
   (let [cm (first (.s this))
         timeout (second (.s this))
         res (http/request (merge req
                                  {:connection-manager cm
                                   :socket-timeout timeout
                                   :conn-timeout timeout}))
         status (:status res)]
      (when-not (contains? #{200 204} status)
         (throw (AssertionError. (str "\"Request didn't complete successfully: " status))))
      res))
