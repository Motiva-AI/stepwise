(ns stepwise.activities
  (:refer-clojure :exclude [ensure])
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

(defn compile-interceptors [activity->handler]
  (into {}
        (map (fn [[activity handler]]
               [activity
                (if (fn? handler)
                  handler
                  (interceptors/compile (into (if (:handler-fn handler)
                                                [(make-handler-interceptor (:handler-fn handler))]
                                                [])
                                              (:interceptors handler))))]))
        activity->handler))

