(ns stepwise.dev-repl
  (:require [stepwise.model :as mdl]
            [stepwise.sugar :as sgr]
            [clojure.repl :refer :all]
            [stepwise.specs.sugar]
            [clojure.tools.namespace.repl :as ctn]
            [stepwise.specs.model]
            [clojure.spec :as s]))

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

  (-> {:start-at :my-choice
       :states   {:my-choice {:type               :choice
                              :default-state-name :ender
                              :choices            [{:condition [:and
                                                                [:= "$.my-string" "hello"]
                                                                [:not "$.my-bool"]]
                                                    :next      :ender}]}
                  :ender     {:type :pass
                              :end  true}}}
      (sgr/desugar-state-machine)
      (mdl/map->StateMachine)))

