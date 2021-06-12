(ns stepwise.interceptors.core
  (:refer-clojure :exclude [compile])
  (:require [sieppari.core :as s]))

(defn well-formed-interceptor-tuple?
  "Interceptor-tuple should be a tuple of the form,
   [:name {:enter (fn [ctx] ... (update ctx :request myfn1))
           :leave (fn [ctx] ... (update ctx :response myfn2))}]

   A few notes:

   1. `:enter` and `:leave` functions need to return `ctx` or an updated
      version of `ctx` for the next interceptor in the chain
   2. either `:enter` or `:leave` fn can be nil if not used
   3. incoming data are tucked away in `:request` for `:enter` fn
   4. outgoing data are in `:response` for `:leave` fn.

   Reference:
   https://github.com/metosin/sieppari"
  [interceptor-tuple]
  (let [[interceptor-name interceptor-map] interceptor-tuple]
    (and (vector? interceptor-tuple)
         (= (count interceptor-tuple) 2)
         (keyword? interceptor-name)
         (map? interceptor-map)
         (or (nil? (:enter interceptor-map))
             (fn? (:enter interceptor-map)))
         (or (nil? (:leave interceptor-map))
             (fn? (:leave interceptor-map))))))

(defn assert-named-chain
  [named-chain]
  (doseq [[index interceptor-tuple] (map vector
                                         (range 0 (count named-chain))
                                         named-chain)]
    (when-not (well-formed-interceptor-tuple? interceptor-tuple)
      (throw (ex-info "Malformed interceptor-tuple. See (doc stepwise.interceptors.core/well-formed-interceptor-tuple?) for example."
                      {:index index
                       :form  interceptor-tuple})))))

(defn- assoc-send-heartbeat-fn-to-context-interceptor
  [send-heartbeat-fn]
  {:enter (fn [ctx] (assoc ctx :send-heartbeat-fn send-heartbeat-fn))})

(defn- interceptor-tuples->interceptors [interceptor-tuples]
  (map second interceptor-tuples))

(defn- prepend-this-interceptor-to-interceptor-chain [this-interceptor chain]
  (cons this-interceptor
        chain))

(defn- form-interceptor-chain [handler-fn interceptors]
  (concat interceptors [handler-fn]))

(defn compile
  "Returns a fn that exercises a chain of interceptor-tuples against a task
   and returns a result. Uses metosin/siepppari under the hood."
  [named-chain handler-fn]
  (assert-named-chain named-chain)

  (with-meta (fn [input send-heartbeat-fn]
               (s/execute
                 (->> named-chain
                      (interceptor-tuples->interceptors)
                      (prepend-this-interceptor-to-interceptor-chain (assoc-send-heartbeat-fn-to-context-interceptor send-heartbeat-fn))
                      (form-interceptor-chain handler-fn))
                 input))
             {:heartbeat? true}))
