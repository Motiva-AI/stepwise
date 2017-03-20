(ns stepwise.interceptors
  (:refer-clojure :exclude [compile]))

; cribbed from re-frame -- thanks re-frame!

(defn- invoke-interceptor-fn
  ; TODO clean up names
  [context [_ interceptor] direction]
  (if-let [f (get interceptor direction)]
    (do (f context))
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

(defn well-formed-interceptor? [interceptor]
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
  "Returns a fn that exercises a queue of interceptors against a task and returns a result"
  [queue]
  (doseq [[index interceptor] (map vector
                                   (range 0 (count queue))
                                   queue)]
    (when-not (well-formed-interceptor? interceptor)
      (throw (ex-info "Malformed interceptor"
                      {:index index
                       :form  interceptor}))))
  (fn execute [input send-heartbeat]
    (-> {:input   input
         :output  nil
         :context {:send-heartbeat send-heartbeat}
         :stack   []
         :queue   (into [] (reverse queue))}
        (invoke-interceptors :before)
        change-direction
        (invoke-interceptors :after)
        :output)))

