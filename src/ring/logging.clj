(ns ring.logging
  (:require [cartus.core :as log]))

(defn wrap-request-logging
  [handler logger]
  (letfn
   [(log-request
      [request]
      (log/info logger :service.rest/request.starting
        {:request request})
      request)]
    (fn
      ([request]
       (handler (log-request request)))
      ([request respond raise]
       (handler (log-request request) respond raise)))))

(def default-current-time-millis-fn #(System/currentTimeMillis))
(def default-response-logging-opts
  {:current-time-millis-fn
   default-current-time-millis-fn})

(defn wrap-response-logging
  ([handler logger]
   (wrap-response-logging handler logger default-response-logging-opts))
  ([handler logger {:keys [current-time-millis-fn]}]
   (let [current-time-millis-fn
         (or current-time-millis-fn default-current-time-millis-fn)]
     (letfn
      [(log-response
         [request response metadata]
         (let [start-ms (get metadata :start-ms)
               latency (- (current-time-millis-fn) start-ms)]
           (log/info logger :service.rest/request.completed
             {:request  request
              :response response
              :latency  latency})))]
       (fn
         ([request]
          (let [metadata {:start-ms (current-time-millis-fn)}
                response (handler request)]
            (log-response request response metadata)
            response))
         ([request respond raise]
          (let [metadata {:start-ms (current-time-millis-fn)}]
            (handler request
              (fn [response]
                (log-response request response metadata)
                (respond response))
              raise))))))))

(defn wrap-logging
  [handler logger current-time-millis-fn]
  (-> handler
    (wrap-request-logging logger)
    (wrap-response-logging logger current-time-millis-fn)))
