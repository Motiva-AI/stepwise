(ns stepwise.serialization
  (:require [clojure.data.json :as json]
            [clojure.string :as strs]
            [clojure.pprint :as pprint]))

; TODO Date(Time) sniff test+deser(?)
(defn deser-io-doc [json-str]
  (json/read-str json-str :key-fn keyword))

; TODO Date(Time) serialization
(defn ser-io-doc [data]
  (json/write-str data))

(defn ser-keyword-name [kw-name]
  (-> kw-name
      str
      (strs/replace #"^:" "")
      (strs/replace #"/" "__")))

(defn deser-keyword-name [kw-name]
  (-> kw-name
      (strs/replace #"__" "/")
      keyword))

(defn ser-exception [e]
  (pprint/write (Throwable->map e) :stream nil))

(defn ser-error-val [error]
  (if (keyword? error)
    (strs/replace (str error) #"^:" "")
    (str error)))

