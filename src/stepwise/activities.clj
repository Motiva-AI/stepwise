(ns stepwise.activities
  (:refer-clojure :exclude [ensure compile])
  (:require [clojure.walk :as walk]
            [stepwise.arns :as arns]
            [stepwise.interceptors.core :as interceptors]
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

(defn- ensure-interceptor-map [handler]
  (if (fn? handler)
    {:handler-fn handler
     :interceptors []}
    handler))

(defn compile [handler]
  (interceptors/compile
    (vec (:interceptors handler))
    (get handler
         :handler-fn
         identity)))

(defn compile-all [activity->handler]
  (into {}
        (map (fn [[activity handler]]
               [activity (-> handler (ensure-interceptor-map) (compile))]))
        activity->handler))

(defn invoke [activity->handler activity-name input]
  (if (contains? activity->handler activity-name)
    (let [compiled (compile (get activity->handler activity-name))]
      (compiled input (fn [])))
    (throw (ex-info "Activity name missing in handler map"
                    {:activity-name activity-name
                     :handled-keys  (set (keys activity->handler))}))))

