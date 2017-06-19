(ns stepwise.activities
  (:refer-clojure :exclude [ensure compile])
  (:require [clojure.walk :as walk]
            [stepwise.arns :as arns]
            [stepwise.interceptors :as interceptors]
            [stepwise.model :as mdl]
            [stepwise.client :as client])
  (:import (clojure.lang MapEntry)))

(defn kw-entry? [node]
  (and (instance? MapEntry node)
       (= (key node) ::mdl/resource)
       (keyword? (val node))))

(defn get-names [definition]
  (into #{}
        (comp (filter kw-entry?)
              (map val))
        (tree-seq #(or (map? %) (vector? %))
                  identity
                  definition)))

(defn resolve-names [old->new definition]
  (walk/prewalk (fn [node]
                  (if (kw-entry? node)
                    (MapEntry. ::mdl/resource (old->new (val node)))
                    node))
                definition))

(defn ensure [env-name kw-name]
  (->> kw-name
       (arns/make-name env-name)
       client/create-activity))

(defn ensure-all [env-name kw-names]
  (into {}
        (map (fn [kw-name]
               [kw-name (ensure env-name kw-name)]))
        kw-names))

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

