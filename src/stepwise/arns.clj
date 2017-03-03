(ns stepwise.arns
  (:require [stepwise.serialization :as ser])
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

(defn get-state-machine-arn [env-name name]
  (str "arn:aws:states:" @region
       ":" @account-number
       ":stateMachine:" (make-name env-name name)))

