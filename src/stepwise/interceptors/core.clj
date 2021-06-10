(ns stepwise.interceptors.core
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.logging :as log]))

; cribbed from re-frame -- thanks re-frame!

(defn- invoke-interceptor-fn
  ; TODO clean up names
  [context [id interceptor] direction]
  (log/trace (prn-str {:interceptor id
                       :direction   direction
                       :context     context}))
  (if-let [f (get interceptor direction)]
    (f context)
    context))

(defn- invoke-interceptors
  ([context direction]
   (loop [context context]
     (let [queue (:queue context)]                          ;; future interceptors
       (if (empty? queue)
         context
         (let [interceptor (peek queue)                     ;; next interceptor to call
               stack       (:stack context)]                ;; already completed interceptors
           (recur (-> context
                      (assoc :queue (pop queue)
                             :stack (conj stack interceptor))
                      (invoke-interceptor-fn interceptor direction)))))))))

(defn- change-direction [context]
  (-> context
      (dissoc :queue)
      (assoc :queue (:stack context))))

(defn well-formed-interceptor?
  "Interceptor should be a tuple of the form,
   [:name {:before (fn [env] ... env)
           :after  (fn [env] ... env)}]

   :before and :after fn needs to return env or an updated version of env for
   the next interceptor in the queue. Either fn can be nil if not used."
  [interceptor]
  (let [stage-map (second interceptor)]
    (and (vector? interceptor)
         (= (count interceptor) 2)
         (keyword? (first interceptor))
         (map? stage-map)
         (or (nil? (:before stage-map))
             (fn? (:before stage-map)))
         (or (nil? (:after stage-map))
             (fn? (:after stage-map))))))

(defn compile
  "Returns a fn that exercises a queue of interceptors against a task and returns a result.

   Reference:
   https://day8.github.io/re-frame/Interceptors/"
  [queue]
  (doseq [[index interceptor] (map vector
                                   (range 0 (count queue))
                                   queue)]
    (when-not (well-formed-interceptor? interceptor)
      (throw (ex-info "Malformed interceptor"
                      {:index index
                       :form  interceptor}))))
  (with-meta (fn execute [input send-heartbeat]
               (-> {:input   input
                    :output  nil
                    :context {:send-heartbeat send-heartbeat}
                    :stack   ()
                    :queue   (into () (reverse queue))}
                   (invoke-interceptors :before)
                   change-direction
                   (invoke-interceptors :after)
                   :output))
             {:heartbeat? true}))

