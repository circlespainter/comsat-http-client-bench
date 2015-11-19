(ns FiberCljHttpEnv
  (:use AutoCloseableCljHttpRequestExecutor)
  (:require [clj-http.conn-mgr :as cm])
  (:gen-class
    :implements [co.paralleluniverse.comsat.bench.http.client.Env]))

(defn -newRequestExecutor [_ _ maxConnections timeout]
  (AutoCloseableCljHttpRequestExecutor.
    (cm/make-reusable-conn-manager
      {:threads maxConnections
       :timeout timeout
       :default-per-route maxConnections})
    timeout))

(defn -newRequest [_ url]
  {:method :get
   :url url
   :throw-exceptions false
   :follow-redirects false})
