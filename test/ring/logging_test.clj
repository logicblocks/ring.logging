(ns ring.logging-test
  (:require [clojure.test :refer :all]
            [ring.logging :as sut]
            [cartus.test :as log]))

(deftest wrap-request-logging-test
  (let [test-response {:body "Hi" :status 200}
        base-handler (fn
                       ([req] (assoc test-response ::received-request req))
                       ([req _response _raise]
                        (assoc test-response ::received-request req)))
        request {:url "some-url", :body "some-request-body"}
        now (System/currentTimeMillis)
        get-current-time-ms (constantly now)]
    (testing "sync request is logged"
      (let [logger (log/logger)
            wrapped-handler (sut/wrap-request-logging base-handler logger get-current-time-ms)
            response (wrapped-handler request)]
        (is (= response
               (merge
                 {::received-request (assoc-in request [:metadata :start-ms] now)}
                 test-response)))
        (is (= [{:context {:request request}
                 :level   :info
                 :type    :service.rest/request.starting}]
               (->> (log/events logger)
                    (map #(dissoc % :meta)))))))
    (testing "async request is logged"
      (let [logger (log/logger)
            wrapped-handler (sut/wrap-request-logging base-handler logger get-current-time-ms)
            response (wrapped-handler request nil nil)]
        (is (= response
               (merge
                 {::received-request (assoc-in request [:metadata :start-ms] now)}
                 test-response)))
        (is (= [{:context {:request request}
                 :level   :info
                 :type    :service.rest/request.starting}]
               (->> (log/events logger)
                    (map #(dissoc % :meta)))))))))

(deftest wrap-response-logging-test
  (let [test-response {:body "Hi" :status 200 }
        base-handler (fn
                       ([_req] test-response)
                       ([_req _response _raise] test-response))
        request {}]
    (testing "sync response is logged"
      (let [logger (log/logger)
            wrapped-handler (sut/wrap-response-logging base-handler logger)
            response (wrapped-handler request)]
        (is (= response
               {:body   "Hi"
                :status 200}))
        (is (=
              [{:context {:latency  0
                          :request  {}
                          :response {:body   "Hi"
                                     :status 200}}
                :level   :info
                :type    :service.rest/request.completed}]
              (->> (log/events logger)
                   (map #(dissoc % :meta)))))))
    (testing "async response is logged"
      (let [logger (log/logger)
            wrapped-handler (sut/wrap-response-logging base-handler logger)
            response (wrapped-handler request nil nil)]
        (is (= response
               {:body   "Hi"
                :status 200}))
        (is (=
              [{:context {:latency  0
                          :request  {}
                          :response {:body   "Hi"
                                     :status 200}}
                :level   :info
                :type    :service.rest/request.completed}]
              (->> (log/events logger)
                   (map #(dissoc % :meta))))))))
  )
