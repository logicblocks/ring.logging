(ns ring.logging-test
  (:require [clojure.test :refer :all]
            [ring.logging :as sut]
            [cartus.test :as log]))

(deftest wrap-request-logging-test
  (let [test-response {:body "Hi" :status 200}
        base-handler (fn
                       ([_req] test-response)
                       ([_req _response _raise] test-response))
        request {:url "some-url", :body "some-request-body"}]
    (testing "sync request is logged"
      (let [logger (log/logger)
            wrapped-handler (sut/wrap-request-logging base-handler logger)
            response (wrapped-handler request)]
        (is (= response
               test-response))
        (is (= [{:context {:request request}
                 :level   :info
                 :type    :service.rest/request.starting}]
               (->> (log/events logger)
                    (map #(dissoc % :meta)))))))
    (testing "async request is logged"
      (let [logger (log/logger)
            wrapped-handler (sut/wrap-request-logging base-handler logger)
            response (wrapped-handler request nil nil)]
        (is (= response
               test-response))
        (is (= [{:context {:request request}
                 :level   :info
                 :type    :service.rest/request.starting}]
               (->> (log/events logger)
                    (map #(dissoc % :meta)))))))))
