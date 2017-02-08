(ns stepwise.client
  (:require [stepwise.model :as model])
  (:import (com.amazonaws.services.stepfunctions AWSStepFunctionsClient)))

(def default-client
  (delay (AWSStepFunctionsClient.)))

(defn syms->pairs [syms]
  (into []
        (mapcat #(vector (keyword "stepwise.model" (name %))
                         %))
        syms))

(defmacro syms->map [& symbols]
  `(hash-map ~@(syms->pairs symbols)))

(defn create-activity
  ([name]
   (create-activity @default-client name))
  ([^AWSStepFunctionsClient client name]
   (model/CreateActivityResult->map
     (.createActivity client
                      (model/map->CreateActivityRequest (syms->map name))))))

(defn create-state-machine
  ([name]
   (create-activity @default-client name))
  ([^AWSStepFunctionsClient client name definition role-arn]
   (model/->mapCreateStateMachineResult
     (.createStateMachine client
                          (model/map->CreateStateMachineRequest (syms->map name
                                                                           definition
                                                                           role-arn))))))

