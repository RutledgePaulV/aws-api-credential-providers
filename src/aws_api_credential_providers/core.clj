(ns aws-api-credential-providers.core
  (:require [cognitect.aws.credentials :as creds]
            [cognitect.aws.client.api :as aws]
            [cognitect.aws.util :as u]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [cognitect.aws.config :as c])
  (:import [java.io File]))

(defn get-aws-profile []
  (or (u/getenv "AWS_PROFILE")
      (u/getProperty "aws.profile")
      "default"))

(defn get-credential-file []
  (or (io/file (u/getenv "AWS_CREDENTIAL_PROFILES_FILE"))
      (io/file (u/getProperty "user.home") ".aws" "credentials")))

(defn assume-role-profile? [profile]
  (contains? profile "role_arn"))

(defn anomaly? [response]
  (contains? response :cognitect.anomalies/category))

(defn expand-profile [profiles profile]
  (loop [this (get profiles profile) chain (list this)]
    (if-some [pointer (get this "source_profile")]
      (if-some [source (get profiles pointer)]
        (recur source (cons source chain))
        (reduce merge {} chain))
      (reduce merge {} chain))))

(defn parse [file]
  (let [base (c/parse file)]
    (into {} (map (fn [k] [k (expand-profile base k)])) (keys base))))

(defn profile-credentials-provider
  ([]
   (profile-credentials-provider (aws/default-http-client)))
  ([http-client]
   (profile-credentials-provider http-client (get-aws-profile)))
  ([http-client profile-name]
   (profile-credentials-provider http-client profile-name (get-credential-file)))
  ([http-client profile-name ^File f]
   (creds/cached-credentials-with-auto-refresh
     (reify creds/CredentialsProvider
       (fetch [_]
         (when (.exists f)
           (try
             (let [profile (get (parse f) profile-name)]
               (if (assume-role-profile? profile)
                 (let [source-profile-name (get profile "source_profile")
                       role-arn            (get profile "role_arn")
                       session-name        (or (get profile "role_session_name") (str (gensym "aws-api-session-")))
                       client              (aws/client
                                             {:api                  :sts
                                              :http-client          http-client
                                              :credentials-provider (profile-credentials-provider http-client source-profile-name f)})
                       response            (aws/invoke client
                                             {:op      :AssumeRole
                                              :request {:RoleArn         role-arn
                                                        :RoleSessionName session-name}})]
                   (if (anomaly? response)
                     (throw (ex-info (format "Error assuming role %s using source profile %s" role-arn source-profile-name) {:response response}))
                     (creds/valid-credentials
                       {:aws/access-key-id             (get-in response [:Credentials :AccessKeyId])
                        :aws/secret-access-key         (get-in response [:Credentials :SecretAccessKey])
                        :aws/session-token             (get-in response [:Credentials :SessionToken])
                        :cognitect.aws.credentials/ttl (creds/calculate-ttl (get-in response [:Credentials]))}
                       "aws assume role using profiles file")))
                 (creds/valid-credentials
                   {:aws/access-key-id     (get profile "aws_access_key_id")
                    :aws/secret-access-key (get profile "aws_secret_access_key")
                    :aws/session-token     (get profile "aws_session_token")}
                   "aws profiles file")))
             (catch Throwable t
               (log/error t "Error fetching credentials from aws profiles file")))))))))


(defn default-credentials-provider
  ([] (default-credentials-provider (aws/default-http-client)))
  ([http-client]
   (creds/chain-credentials-provider
     [(creds/environment-credentials-provider)
      (creds/system-property-credentials-provider)
      (profile-credentials-provider http-client)
      (creds/container-credentials-provider http-client)
      (creds/instance-profile-credentials-provider http-client)])))

(def ^:private shared-credential-provider
  (delay (default-credentials-provider)))

(defn credentials-provider []
  (force shared-credential-provider))
