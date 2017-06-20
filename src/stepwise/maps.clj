(ns stepwise.maps)

(defn syms->pairs [syms]
  (into []
        (mapcat #(vector (keyword (name %))
                         %))
        syms))

(defmacro syms->map [& symbols]
  `(hash-map ~@(syms->pairs symbols)))

