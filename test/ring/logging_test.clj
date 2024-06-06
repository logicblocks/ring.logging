(ns ring.logging-test
  (:require [clojure.test :refer :all]
            [ring.logging :as sut]
            [cartus.test :as log :refer [logged?]]))

(defn- mk-handler [response-to-return]
  (fn
    ([req]
     (-> response-to-return
       (with-meta {::received-request req})))
    ([req respond _raise]
     (-> response-to-return
       (with-meta {::received-request req})
       respond))))

(defn- run-async-handler [handler request]
  (let [captured-response (atom nil)
        respond (fn [response]
                  (reset! captured-response response))
        raise (fn [err]
                (throw (ex-info "Raised error" {:err err})))]
    (handler request respond raise)
    @captured-response))

(deftest wrap-request-logging-test
  (let [test-response {:body "Hi" :status 200}
        base-handler (mk-handler test-response)
        request {:url "some-url", :body "some-request-body"}]
    (testing "sync request is logged"
      (let [logger (log/logger)
            options {:update-context-fn identity}
            wrapped-handler
            (sut/wrap-request-logging base-handler logger options)
            response (wrapped-handler request)]
        (is (= response
              test-response))
        (is (= (-> response meta ::received-request)
              request))
        (is (logged?
              logger
              #{:only}
              {:context {:request request}
               :level   :info
               :type    :service.rest/request.starting}))))
    (testing "async request is logged"
      (let [logger (log/logger)
            options {:update-context-fn identity}
            wrapped-handler
            (sut/wrap-request-logging base-handler logger options)
            response (run-async-handler wrapped-handler request)]
        (is (= response
              test-response))
        (is (= (-> response meta ::received-request)
              request))
        (is (logged?
              logger
              #{:only}
              {:context {:request request}
               :level   :info
               :type    :service.rest/request.starting}))))
    (testing "can provide function for updating context data"
      (let [logger (log/logger)
            hide-url #(assoc-in % [:request :url] "****")
            options {:update-context-fn hide-url}
            wrapped-handler
            (sut/wrap-request-logging base-handler logger options)
            _ (wrapped-handler request)]
        (is (logged?
              logger
              #{:only}
              {:context {:request {:url "****"}}
               :level   :info
               :type    :service.rest/request.starting}))))))

(deftest wrap-response-logging-test
  (let [test-response {:body "Hi" :status 200}
        base-handler (mk-handler test-response)
        request {:url "some-url", :body "some-request-body"}]
    (testing "sync response is logged"
      (let [logger (log/logger)
            time (atom 0)
            duration 10
            current-time-millis-fn (fn [] (swap! time #(+ % 10)))
            options {:current-time-millis-fn current-time-millis-fn
                     :update-context-fn identity}
            wrapped-handler (sut/wrap-response-logging
                              base-handler
                              logger
                              options)
            response (wrapped-handler request)]
        (is (= response
              test-response))
        (is (= (-> response meta ::received-request)
              request))
        (is (logged?
              logger
              #{:only}
              {:context {:latency  duration
                         :request  request
                         :response test-response}
               :level   :info
               :type    :service.rest/request.completed}))))
    (testing "async response is logged"
      (let [logger (log/logger)
            time (atom 0)
            duration 10
            current-time-millis-fn (fn [] (swap! time #(+ % 10)))
            options {:current-time-millis-fn current-time-millis-fn
                     :update-context-fn identity}
            wrapped-handler (sut/wrap-response-logging
                              base-handler
                              logger
                              options)
            response (run-async-handler wrapped-handler request)]
        (is (= response
              test-response))
        (is (= (-> response meta ::received-request)
              request))
        (is (logged?
              logger
              #{:only}
              {:context {:latency  duration
                         :request  request
                         :response test-response}
               :level   :info
               :type    :service.rest/request.completed}))))
    (testing "can provide functions for updating request/response data"
      (testing "sync response is logged"
        (let [logger (log/logger)
              time (atom 0)
              duration 10
              current-time-millis-fn (fn [] (swap! time #(+ % 10)))
              redact-bodies
              (fn [context]
                (-> context
                  (assoc-in [:request :body] "<request-body>")
                  (assoc-in [:response :body] "<response-body>")))
              options {:current-time-millis-fn current-time-millis-fn
                       :update-context-fn      redact-bodies}
              wrapped-handler (sut/wrap-response-logging
                                base-handler
                                logger
                                options)
              _ (wrapped-handler request)]
          (is (logged?
                logger
                #{:only}
                {:context {:latency  duration
                           :request  {:body "<request-body>"}
                           :response {:body "<response-body>"}}
                 :level   :info
                 :type    :service.rest/request.completed})))))))
