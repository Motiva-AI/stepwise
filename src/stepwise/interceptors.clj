(ns stepwise.interceptors
  (:refer-clojure :exclude [compile]))

; cribbed from re-frame -- thanks re-frame!

(defn- invoke-interceptor-fn
  [context interceptor direction]
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

(defn compile
  "Returns a fn that exercises a queue of interceptors against a task and returns a result"
  [queue]
  (fn execute [task]
    (-> {:task  task
         :queue queue}
        (invoke-interceptors :before)
        change-direction
        (invoke-interceptors :after)
        :result)))

