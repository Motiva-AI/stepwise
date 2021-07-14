(ns stepwise.s3
  (:require [clojure.string]
            [clojure.java.io]
            [cognitect.aws.client.api :as aws]
            [taoensso.nippy :as nippy]))

(def stock-s3-client (delay (aws/client {:api :s3})))
(def bucket-key-separator "/")

(defn get-s3-client []
  @stock-s3-client)


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

(defn load-from-s3
  ([s3-client source]
   (->> (parse-s3-bucket-and-key source)
        (hash-map :op :GetObject :request)
        (aws/invoke s3-client)
        (:Body)
        (slurp-bytes)
        (deseralize)))

  ([source-arn] (load-from-s3 (get-s3-client) source-arn)))

(defn- put-object-request [bucket-name key bytes-array]
  {:Bucket bucket-name
   :Key    key
   :Body   bytes-array})

(defn- generate-s3-object-key []
  (str (java.util.UUID/randomUUID) ".nippy"))

(defn offload-to-s3
  "Returns generated object-key"
  ([s3-client bucket-name coll]
   (let [object-key (generate-s3-object-key)]
     (->> coll
          (serialize)
          (put-object-request bucket-name object-key)
          (hash-map :op :PutObject :request)
          (aws/invoke s3-client))

     (unparse-s3-bucket-and-key {:Bucket bucket-name :Key object-key})))

  ([bucket-name coll] (offload-to-s3 (get-s3-client) bucket-name coll)))

