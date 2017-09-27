(ns stepwise.activities
  (:refer-clojure :exclude [ensure compile])
  (:require [clojure.walk :as walk]
            [stepwise.arns :as arns]
            [stepwise.interceptors :as interceptors]
            [stepwise.model :as mdl]
            [stepwise.client :as client])
  (:import (clojure.lang MapEntry)))

(defn resource-entry? [node]
  (and (instance? MapEntry node)
       (#{::mdl/resource :resource} (key node))))

(defn get-names [definition]
  (into #{}
        (comp (filter resource-entry?)
              (filter #(keyword? (val %)))
              (map val))
        (tree-seq #(or (map? %) (vector? %))
                  identity
                  definition)))

(defn resolve-resources [old->new definition]
  (walk/prewalk (fn [node]
                  (if (resource-entry? node)
                    (MapEntry. (key node) (old->new (val node)))
                    node))
                definition))

(defn resolve-kw-resources [old->new definition]
  (resolve-resources (fn [resource-name]
                       (if (keyword? resource-name)
                         (old->new resource-name)
                         resource-name))
                     definition))

(defn ensure [kw-name]
  (client/create-activity (arns/make-name kw-name)))

(defn ensure-all [activity-names]
  (into {}
        (map (fn [activity-name]
               [activity-name (ensure activity-name)]))
        activity-names))

(defn make-handler-interceptor [handler-fn]
  [:handler
   {:before (fn [env]
              (assoc env :output (handler-fn (select-keys env [:context :input]))))}])

(defn identity-handler-fn [{:keys [input]}] input)

(defn compile [handler]
  (if (fn? handler)
    handler
    (interceptors/compile (into (vec (:interceptors handler))
                                [(make-handler-interceptor (get handler
                                                                :handler-fn
                                                                identity-handler-fn))]))))

(defn compile-all [activity->handler]
  (into {}
        (map (fn [[activity handler]]
               [activity (compile handler)]))
        activity->handler))

(defn invoke [activity->handler activity-name input]
  (if (contains? activity->handler activity-name)
    (let [compiled (compile (get activity->handler activity-name))]
      (compiled input (fn [])))
    (throw (ex-info "Activity name missing in handler map"
                    {:activity-name activity-name
                     :handled-keys  (set (keys activity->handler))}))))

