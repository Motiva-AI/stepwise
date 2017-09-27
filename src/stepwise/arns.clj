(ns stepwise.arns
  (:require [stepwise.serialization :as ser]
            [clojure.string :as strs])
  (:import (com.amazonaws.regions DefaultAwsRegionProviderChain)
           (com.amazonaws.services.securitytoken AWSSecurityTokenServiceClientBuilder)
           (com.amazonaws.services.securitytoken.model GetCallerIdentityRequest)))

(def region
  (delay (.getRegion (DefaultAwsRegionProviderChain.))))

(def account-number
  (delay (-> (AWSSecurityTokenServiceClientBuilder/defaultClient)
             (.getCallerIdentity (GetCallerIdentityRequest.))
             (.getAccount))))

(defn make-name [name]
  (ser/ser-keyword-name name))

(defn get-arn-stem [state-machine-name resource]
  (str "arn:aws:states:" @region
       ":" @account-number
       ":" resource ":"
       (make-name state-machine-name)))

(defn get-state-machine-arn [state-machine-name]
  (get-arn-stem state-machine-name "stateMachine"))

(defn get-execution-arn [state-machine-name execution-name]
  (str (get-arn-stem state-machine-name
                     "execution")
       ":" execution-name))

(defn get-activity-arn [activity-name]
  (get-arn-stem activity-name "activity"))

(defn get-role-arn [path role-name]
  (str "arn:aws:iam::" @account-number ":role" path role-name))

(defn resource-arn->kw [arn]
  (-> (strs/split arn #":")
      (last)
      (ser/deser-keyword-name)))

