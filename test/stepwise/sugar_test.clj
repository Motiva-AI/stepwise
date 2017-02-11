(ns stepwise.sugar-test
  (:require [clojure.test :as test]
            [stepwise.model :as mdl]
            [stepwise.sugar :as main]))

(test/deftest desugar-condition
  (test/is (= [::mdl/bool-eq {::mdl/variable               "$.my-bool?"
                              ::mdl/expected-value-boolean true}]
              (main/desugar-condition "$.my-bool?")))

  (test/is (= [::mdl/not [::mdl/bool-eq {::mdl/variable               "$.my-bool?"
                                         ::mdl/expected-value-boolean true}]]
              (main/desugar-condition [:not "$.my-bool?"])))

  (test/is (= [::mdl/str-eq {::mdl/variable              "$.my-str"
                             ::mdl/expected-value-string "hello"}]
              (main/desugar-condition [:= "$.my-str" "hello"]))))

(test/deftest sugar-condition
  (test/is (= "$.my-bool?"
              (main/sugar-condition [::mdl/bool-eq {::mdl/variable               "$.my-bool?"
                                                    ::mdl/expected-value-boolean true}])))

  (test/is (= [:not "$.my-bool?"]
              (main/sugar-condition [::mdl/not [::mdl/bool-eq {::mdl/variable               "$.my-bool?"
                                                               ::mdl/expected-value-boolean true}]])))

  (test/is (= [:= "$.my-str" "hello"]
              (main/sugar-condition [::mdl/str-eq {::mdl/variable              "$.my-str"
                                                   ::mdl/expected-value-string "hello"}]))))

(test/deftest desugar-transition
  (test/is (= {::mdl/transition ::mdl/end}
              (main/desugar-transition {:end true})))

  (test/is (= {::mdl/transition "state"}
              (main/desugar-transition {:next :state}))))

(test/deftest sugar-transition
  (test/is (= {:end true}
              (main/sugar-transition {::mdl/transition ::mdl/end})))

  (test/is (= {:next :state}
              (main/sugar-transition {::mdl/transition "state"}))))

(test/deftest desugar-wait
  (test/is (= {::mdl/type       ::mdl/wait
               ::mdl/wait-for   {::mdl/seconds 10}
               ::mdl/transition "state"}
              (main/desugar-state {:type    :wait
                                   :seconds 10
                                   :next    :state}))))

(test/deftest sugar-wait
  (test/is (= {:type    :wait
               :seconds 10
               :next    :state}
              (main/sugar-state {::mdl/type       ::mdl/wait
                                 ::mdl/wait-for   {::mdl/seconds 10}
                                 ::mdl/transition "state"}))))

(test/deftest desugar-choice
  (test/is (= {::mdl/type    ::mdl/choice
               ::mdl/choices [{::mdl/condition  [::mdl/bool-eq {::mdl/variable               "$.foo"
                                                                ::mdl/expected-value-boolean true}]
                               ::mdl/transition "bar"}]}
              (main/desugar-state {:type    :choice
                                   :choices [{:condition "$.foo"
                                              :next      :bar}]}))))

(test/deftest sugar-choice
  (test/is (= {:type    :choice
               :choices [{:condition "$.foo"
                          :next      :bar}]}
              (main/sugar-state {::mdl/type    ::mdl/choice
                                 ::mdl/choices [{::mdl/condition  [::mdl/bool-eq {::mdl/variable               "$.foo"
                                                                                  ::mdl/expected-value-boolean true}]
                                                 ::mdl/transition "bar"}]}))))

(test/deftest desugar-pass
  (test/is (= {::mdl/type       ::mdl/pass
               ::mdl/transition "next-state"}
              (main/desugar-state {:type :pass
                                   :next :next-state}))))

(test/deftest sugar-pass
  (test/is (= {:type :pass
               :next :next-state}
              (main/sugar-state {::mdl/type       ::mdl/pass
                                 ::mdl/transition "next-state"}))))

#_(test/deftest desugar-task
    (test/is (= {::mdl/type ::mdl/task
                 })))
