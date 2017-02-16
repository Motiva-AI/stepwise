(ns stepwise.dev-repl
  (:require [stepwise.model :as mdl]
            [stepwise.sugar :as sgr]
            [clojure.spec.gen :as sgen]
            [clojure.repl :refer :all]
            [stepwise.specs.sugar :as sgrs]
            [alembic.still :refer [lein]]
            [clojure.tools.namespace.repl :as ctn]
            [clojure.walk :as walk]
            [stepwise.specs.model]
            [clojure.spec :as s]
            [clojure.test.check.generators :as gen]))

(defn go [])

(defn reset []
  (ctn/refresh :after 'stepwise.dev-repl/go))

(defn sandbox []
  #_(s/explain-data ::sgr/state-machine
                    {:start-at :my-choice
                     :states   {:my-choice {:type    :choice
                                            :choices [{:condition [:and
                                                                   [:= "$.my-string" "hello"]
                                                                   [:not "$.my-bool"]]
                                                       :next      :foo-the-bar}]}}})

  #_(-> {:start-at :my-choice
         :states   {:my-choice {:type               :choice
                                :default-state-name :ender
                                :choices            [{:condition [:and
                                                                  [:= "$.my-string" "hello"]
                                                                  [:not "$.my-bool"]]
                                                      :next      :ender}]}
                    :ender     {:type :pass
                                :end  true}}}
        (sgr/desugar)
        (sgr/sugar))
  ;(gen/sample (s/gen ::sgr/error))
  (lein test)
  #_(sgr/sugar-comparison :stepwise.model/numeric-eq #:stepwise.model{:variable "0", :expected-value-double 1.0}))

