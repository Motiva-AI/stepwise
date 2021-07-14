(ns stepwise.client.s3
  (:require [clojure.string]
            [clojure.java.io]
            [taoensso.nippy :as nippy]))

(defn parse-s3-bucket-and-key-from-arn [arn]
  (-> arn
      (clojure.string/split #":::")
      (last)
      (clojure.string/split #"/")
      (some->> (remove empty?)
               (zipmap [:Bucket :Key]))))

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

