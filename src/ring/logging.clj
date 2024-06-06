(ns ring.logging
  (:require [cartus.core :as log]))

(def default-request-logging-options
  {:update-context-fn identity})

(defn wrap-request-logging
  ([handler logger]
   (wrap-request-logging handler logger default-request-logging-options))
  ([handler logger {:keys [update-context-fn]}]
   (let [update-context-fn
         (or update-context-fn
           (:update-context-fn default-request-logging-options))]
     (letfn
      [(log-request
         [request]
         (->> {:request request}
           (update-context-fn)
           (log/info logger :service.rest/request.starting))
         request)]
       (fn
         ([request]
          (handler (log-request request)))
         ([request respond raise]
          (handler (log-request request) respond raise)))))))

(def default-response-logging-options
  {:current-time-millis-fn
   #(System/currentTimeMillis)
   :update-context-fn
   identity})

(defn wrap-response-logging
  ([handler logger]
   (wrap-response-logging handler logger default-response-logging-options))
  ([handler logger {:keys [current-time-millis-fn update-context-fn]}]
   (let [current-time-millis-fn
         (or current-time-millis-fn
           (:current-time-millis-fn default-request-logging-options))
         update-context-fn
         (or update-context-fn
           (:update-context-fn default-request-logging-options))]
     (letfn
      [(log-response
         [request response metadata]
         (let [start-ms (get metadata :start-ms)
               latency (- (current-time-millis-fn) start-ms)]
           (->> {:request  request
                 :response response
                 :latency  latency}
             (update-context-fn)
             (log/info logger :service.rest/request.completed))))]
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
  ([handler logger]
   (wrap-logging
     handler
     logger
     default-request-logging-options
     default-response-logging-options))
  ([handler logger request-logging-options response-logging-options]
   (-> handler
     (wrap-request-logging logger request-logging-options)
     (wrap-response-logging logger response-logging-options))))
