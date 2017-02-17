(ns stepwise.json
  (:require [clojure.data.json :as json]))

; TODO Date sniff test+deser(?)
(defn deser-io-doc [json-str]
  (json/read-str json-str :key-fn keyword))

; TODO Date serialization
(defn ser-io-doc [data]
  (json/write-str data))


