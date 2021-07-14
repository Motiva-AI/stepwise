(ns stepwise.client.s3
  (:require [clojure.string]
            [clojure.java.io]
            [taoensso.nippy :as nippy]))

(def bucket-key-separator "/")

(defn parse-s3-bucket-and-key [s]
  (-> s
      (clojure.string/split (re-pattern bucket-key-separator))
      (some->> (remove empty?)
               (zipmap [:Bucket :Key]))))

(defn unparse-s3-bucket-and-key [{bucket-name :Bucket object-key :Key}]
  (when (and bucket-name object-key)
    (str bucket-name bucket-key-separator object-key)))

(defn slurp-bytes
  "Slurp bytes from any one of InputStream, File, URI, URL, Socket, byte array, or String."
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)
              in  (clojure.java.io/input-stream x)]
    (clojure.java.io/copy in out)
    (.toByteArray out)))

(defn serialize [coll]
  (nippy/freeze coll))

(defn deseralize [bytes-array]
  (nippy/thaw bytes-array))

