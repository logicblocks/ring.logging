(ns ring.logging-test
  (:require [clojure.test :refer :all]
            [ring.logging :as sut]
            [cartus.test :as log]))

(deftest wrap-request-logging-test
  (testing "request is logged"
    (let [test-response {:body "Hi" :status 200}
          base-handler (fn [_req] {:body "Hi" :status 200})
          logger (log/logger)
          wrapped-handler (sut/wrap-request-logging base-handler logger)
          request {:url "some-url", :body "some-request-body"}
          response (wrapped-handler request)]
      (is (= response
             test-response))
      (is (= [{:context {:request request}
               :level   :info
               :type    :service.rest/request.starting}]
             (->> (log/events logger)
                  (map #(dissoc % :meta))
                  )
             ))
      ))
  )
