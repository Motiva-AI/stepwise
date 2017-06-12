(ns stepwise.sugar-test
  (:require [clojure.test :as test]
            [com.gfredericks.test.chuck.clojure-test :as chuck]
            [stepwise.sugar :as main]
            [stepwise.specs.sugar :as sgrs]))

(test/deftest sugar-test
  (chuck/checking "sugar is the inverse of desugar" 3
    [sugared (sgrs/get-gen)]
    (test/is (= sugared (-> sugared main/desugar main/sugar)))))

(test/deftest get-non-model-keys
  (test/is (= (main/get-non-model-keys {:foo [{:bar :bam}]
                                        :bim {:boom :bap}})
              #{[:foo]
                [:bim]
                [:foo 0 :bar]
                [:bim :boom]})))

