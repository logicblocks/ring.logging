(ns ring.logging
  (:require [cartus.core :as log]))

(defn wrap-request-logging
  ([handler logger]
   (letfn
     [(log-request
        [request]
        (let [start-ms (System/currentTimeMillis)]
          (log/info logger :service.rest/request.starting
                    {:request request})
          (assoc request :metadata {:start-ms start-ms})))]
     (fn [request]
       (handler request))
     (fn
       ([request]
        (handler (log-request request)))
       ([request respond raise]
        (handler (log-request request) respond raise))))))

(defn wrap-response-logging
  ([handler logger]
   (letfn
     [(log-response
        [request response metadata]
        (let [start-ms (get metadata :start-ms)
              latency (- (System/currentTimeMillis) start-ms)]
          (log/info logger :service.rest/request.completed
                    {:request  request
                     :response response
                     :latency  latency})))]
     (fn
       ([request]
        (let [metadata
              (get request :metadata
                   {:start-ms (System/currentTimeMillis)})
              response (handler request)]
          (log-response request response metadata)
          response))
       ([request respond raise]
        (let [metadata (get request :metadata
                            {:start-ms (System/currentTimeMillis)})]
          (handler request
                   (fn [response]
                     (log-response request response metadata)
                     (respond response))
                   raise)))))))

(defn wrap-logging
  [handler logger]
  (-> handler
      (wrap-request-logging logger)
      (wrap-response-logging logger)))

