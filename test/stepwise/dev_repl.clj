(ns stepwise.dev-repl
  (:require [stepwise.model :as mdl]
            [stepwise.sugar :as sgr]
            [stepwise.specs.sugar]
            [clojure.tools.namespace.repl :as ctn]
            [stepwise.specs.model]
            [clojure.spec :as s]))

(defn go [])

(defn reset []
  (ctn/refresh :after 'stepwise.dev-repl/go))

(defn sandbox []
  (s/explain-data ::sgr/state-machine
                  {:start-at :my-choice
                   :states   {:my-choice {:type    :choice
                                          :choices [{:condition [:and
                                                                 [:= "$.my-string" "hello"]
                                                                 [:not "$.my-bool"]]
                                                     :next      :foo-the-bar}]}}}))

