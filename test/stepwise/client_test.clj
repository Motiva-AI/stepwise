(ns stepwise.client-test
  (:require [clojure.test :refer :all]
            [stepwise.client :as c]
            [stepwise.client.s3 :as s3]))

(def bucket-name "stepwise-integration-test")

;; TODO setup integration envs on CI
#_(deftest ^:integration offload-to-s3-round-trip-test
  (let [coll {:foo 3
              :bar :soap}
        key (c/offload-to-s3 bucket-name coll)]
    (is (string? key))
    (is (= coll (c/load-from-s3 (str bucket-name s3/bucket-key-separator key))))))

