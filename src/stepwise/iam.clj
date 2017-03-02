(ns stepwise.iam
  (:require [bean-dip.core :as bd])
  (:import (com.amazonaws.services.identitymanagement AmazonIdentityManagementClientBuilder AmazonIdentityManagement)
           (com.amazonaws.services.identitymanagement.model CreateRoleRequest
                                                            PutRolePolicyRequest
                                                            GetRoleRequest NoSuchEntityException GetRolePolicyRequest)
           (com.amazonaws.regions DefaultAwsRegionProviderChain)))

(set! *warn-on-reflection* true)

(bd/def-translation GetRoleRequest #{::role-name})
(bd/def-translation CreateRoleRequest #{::path ::role-name})
(bd/def-translation GetRolePolicyRequest #{::role-name ::policy-name})
(bd/def-translation PutRolePolicyRequest #{::policy-document ::policy-name ::role-name})

(def client
  (delay (AmazonIdentityManagementClientBuilder/defaultClient)))

(def region
  (delay (.getRegion (DefaultAwsRegionProviderChain.))))

(def path "/service-role/")

(defn get-role-name []
  (str "StatesExecutionRole-" @region))
(defn get-policy-name []
  (str "StatesExecutionPolicy-" @region))

#_(defn ensure-role []
    (let [role-name   (get-role-name)
          policy-name (get-policy-name)
          client      ^AmazonIdentityManagement @client]
      (try
        (.getRole client (map->GetRoleRequest {::role-name role-name}))
        (catch NoSuchEntityException _
          (.createRole client (map->CreateRoleRequest {::path      path
                                                       ::role-name role-name}))))
      (try
        (.getPolicyDocument client (map->GetRolePolicyRequest {::role-name   role-name
                                                               ::policy-name policy-name})))))

; TODO return via ensure above
(defn ensure-execution-role []
  "arn:aws:iam::256212633204:role/service-role/StatesExecutionRole-us-west-2")

