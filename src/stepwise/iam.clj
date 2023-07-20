(ns stepwise.iam
  (:require [bean-dip.core :as bd]
            [clojure.data.json :as json]
            [stepwise.arns :as arns])
  (:import (com.amazonaws.services.identitymanagement AmazonIdentityManagementClientBuilder
                                                      AmazonIdentityManagement)
           (com.amazonaws.services.identitymanagement.model CreateRoleRequest
                                                            PutRolePolicyRequest
                                                            GetRoleRequest
                                                            GetRolePolicyRequest
                                                            NoSuchEntityException)
           (com.amazonaws.regions DefaultAwsRegionProviderChain)))

(def assume-role-policy
  {"Version"   "2012-10-17",
   "Statement" [{"Effect"    "Allow",
                 "Principal" {"Service" "states.amazonaws.com"},
                 "Action"    "sts:AssumeRole"}]})

(def execution-policy
  {"Version"   "2012-10-17"
   "Statement" [; Permission to call Lambda functions
                {"Effect"   "Allow"
                 "Action"   ["lambda:InvokeFunction"]
                 "Resource" "*"}

                ; Permission to call another nested workflow execution
                ; Reference https://docs.aws.amazon.com/step-functions/latest/dg/stepfunctions-iam.html
                {"Effect"   "Allow"
                 "Action"   ["states:StartExecution"]
                 "Resource" "arn:aws:states:*:*"}
                {"Effect"   "Allow"
                 "Action"   ["states:DescribeExecution"
                             "states:StopExecution"]
                 "Resource" "*"}
                {"Effect"   "Allow"
                 "Action"   ["events:PutTargets"
                             "events:PutRule"
                             "events:DescribeRule"]
                 "Resource" "arn:aws:events:us-west-2:*:rule/StepFunctionsGetEventsForStepFunctionsExecutionRule"}]})

(set! *warn-on-reflection* true)

(defmethod bd/->bean-val ::policy-document [_ policy-document]
  (json/write-str policy-document))

(defmethod bd/->bean-val ::assume-role-policy-document [_ policy-document]
  (json/write-str policy-document))

(bd/def-translation GetRoleRequest #{::role-name})
(bd/def-translation CreateRoleRequest #{::path ::role-name ::assume-role-policy-document})
(bd/def-translation GetRolePolicyRequest #{::role-name ::policy-name})
(bd/def-translation PutRolePolicyRequest #{::policy-document ::policy-name ::role-name})

(def client
  (delay (AmazonIdentityManagementClientBuilder/defaultClient)))

(def region
  (delay (.getRegion (DefaultAwsRegionProviderChain.))))

(def path "/service-role/")

(defn get-role-name []
  (str "StepwiseStatesExecutionRole-" @region))
(defn get-policy-name []
  (str "StepwiseStatesExecutionPolicy-" @region))

(def execution-role-arn
  (delay
    (let [role-name   (get-role-name)
          policy-name (get-policy-name)
          client      ^AmazonIdentityManagement @client]
      (try
        (.getRole client (map->GetRoleRequest {::role-name role-name}))
        (catch NoSuchEntityException _
          (.createRole client (map->CreateRoleRequest {::path                        path
                                                       ::role-name                   role-name
                                                       ::assume-role-policy-document assume-role-policy}))))
      (try
        (.getRolePolicy client (map->GetRolePolicyRequest {::role-name   role-name
                                                           ::policy-name policy-name}))
        (catch NoSuchEntityException _
          (.putRolePolicy client (map->PutRolePolicyRequest {::role-name       role-name
                                                             ::policy-name     policy-name
                                                             ::policy-document execution-policy}))))
      (arns/get-role-arn path role-name))))

(defn ensure-execution-role []
  @execution-role-arn)

