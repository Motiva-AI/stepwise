(ns stepwise.arns
  (:require [stepwise.serialization :as ser]
            [stepwise.client :as client]
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

(defn make-name [env-name name-kw]
  (str env-name "_" (ser/ser-keyword-name name-kw)))

(defn get-arn-stem [env-name state-machine-name resource]
  (str "arn:aws:states:" @region
       ":" @account-number
       ":" resource ":"
       (make-name env-name state-machine-name)))

(defn get-state-machine-arn [env-name state-machine-name]
  (get-arn-stem env-name state-machine-name "stateMachine"))

(defn get-execution-arn [env-name state-machine-name execution-name]
  (str (get-arn-stem env-name
                     state-machine-name
                     "execution")
       ":" execution-name))

(defn get-activity-arn [env-name activity-name]
  (get-arn-stem env-name activity-name "activity"))

(defn get-role-arn [path role-name]
  (str "arn:aws:iam::" @account-number ":role" path role-name))

