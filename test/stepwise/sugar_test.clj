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

(test/deftest desugar-task
  (test/is (= {::mdl/type       ::mdl/task
               ::mdl/transition "next-state"
               ::mdl/catchers   [{::mdl/error-equals #{"transition-error"}
                                  ::mdl/transition   "after-transition-error"}]
               ::mdl/retriers   [{::mdl/error-equals #{"another-retry-error"
                                                       "retry-error"}}]}
              (main/desugar-state {:type     :task
                                   :next     :next-state
                                   :catchers [{:error-equals :transition-error
                                               :next         :after-transition-error}]
                                   :retriers [{:error-equals #{:another-retry-error
                                                               :retry-error}}]}))))

(test/deftest sugar-task
  (test/is (= {:type     :task
               :next     :next-state
               :catchers [{:error-equals :transition-error
                           :next         :after-transition-error}]
               :retriers [{:error-equals #{:another-retry-error
                                           :retry-error}}]}
              (main/sugar-state {::mdl/type       ::mdl/task
                                 ::mdl/transition "next-state"
                                 ::mdl/catchers   [{::mdl/error-equals #{"transition-error"}
                                                    ::mdl/transition   "after-transition-error"}]
                                 ::mdl/retriers   [{::mdl/error-equals #{"another-retry-error"
                                                                         "retry-error"}}]}))))


(test/deftest desugar-task
  (test/is (= {::mdl/type       ::mdl/task
               ::mdl/transition "next-state"
               ::mdl/catchers   [{::mdl/error-equals #{"transition-error"}
                                  ::mdl/transition   "after-transition-error"}]
               ::mdl/retriers   [{::mdl/error-equals #{"another-retry-error"
                                                       "retry-error"}}]}
              (main/desugar-state {:type     :task
                                   :next     :next-state
                                   :catchers [{:error-equals :transition-error
                                               :next         :after-transition-error}]
                                   :retriers [{:error-equals #{:another-retry-error
                                                               :retry-error}}]}))))

(test/deftest desugar-parallel
  (test/is (= {::mdl/type       ::mdl/parallel
               ::mdl/transition "next-state"
               ::mdl/catchers   [{::mdl/error-equals #{"transition-error"}
                                  ::mdl/transition   "after-transition-error"}]
               ::mdl/retriers   [{::mdl/error-equals #{"another-retry-error"
                                                       "retry-error"}}]
               ::mdl/branches   [{::mdl/start-at "start-at"
                                  ::mdl/states   {"start-at" {::mdl/type       ::mdl/pass
                                                              ::mdl/transition ::mdl/end}}}]}
              (main/desugar-state {:type     :parallel
                                   :next     :next-state
                                   :catchers [{:error-equals :transition-error
                                               :next         :after-transition-error}]
                                   :retriers [{:error-equals #{:another-retry-error
                                                               :retry-error}}]
                                   :branches [{:start-at :start-at
                                               :states   {:start-at {:type :pass
                                                                     :end  true}}}]}))))

(test/deftest sugar-parallel
  (test/is (= {:type     :parallel
               :next     :next-state
               :catchers [{:error-equals :transition-error
                           :next         :after-transition-error}]
               :retriers [{:error-equals #{:another-retry-error
                                           :retry-error}}]
               :branches [{:start-at :start-at
                           :states   {:start-at {:type :pass
                                                 :end  true}}}]}
              (main/sugar-state {::mdl/type       ::mdl/parallel
                                 ::mdl/transition "next-state"
                                 ::mdl/catchers   [{::mdl/error-equals #{"transition-error"}
                                                    ::mdl/transition   "after-transition-error"}]
                                 ::mdl/retriers   [{::mdl/error-equals #{"another-retry-error"
                                                                         "retry-error"}}]
                                 ::mdl/branches   [{::mdl/start-at "start-at"
                                                    ::mdl/states   {"start-at" {::mdl/type       ::mdl/pass
                                                                                ::mdl/transition ::mdl/end}}}]}))))

