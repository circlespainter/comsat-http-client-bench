(ns FiberHttpKitEnv
  (:use AutoCloseableHttpKitRequestExecutor)
  (:gen-class
    :implements [co.paralleluniverse.comsat.bench.http.client.Env]))

(defn -newRequestExecutor [_ _ _ timeout]
  (AutoCloseableHttpKitRequestExecutor.
    timeout))

(defn -newRequest [_ url]
  {:method :get
   :url url
   :throw-exceptions false
   :follow-redirects false})
