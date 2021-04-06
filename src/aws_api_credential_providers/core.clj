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

(declare assume-role-credentials-provider)

(defn profile-credentials-provider
  ([] (profile-credentials-provider
        (get-aws-profile)))
  ([profile-name]
   (profile-credentials-provider
     (or profile-name (get-aws-profile))
     (aws/default-http-client)))
  ([profile-name http-client]
   (profile-credentials-provider
     (or profile-name (get-aws-profile))
     (or http-client (aws/default-http-client))
     (get-credential-file)))
  ([profile-name http-client ^File f]
   (let [delegate-assume-role
         (memoize (fn [profile]
                    (let [role-arn      (get profile "role_arn")
                          session-name  (or (get profile "role_session_name") (str (gensym "aws-api-session-")))
                          cred-provider (reify creds/CredentialsProvider
                                          (fetch [_]
                                            (creds/valid-credentials
                                              {:aws/access-key-id     (get profile "aws_access_key_id")
                                               :aws/secret-access-key (get profile "aws_secret_access_key")
                                               :aws/session-token     (get profile "aws_session_token")}
                                              "aws profile")))
                          client        (aws/client {:api :sts :http-client http-client :credentials-provider cred-provider})]
                      (assume-role-credentials-provider client role-arn session-name))))
         delegate-direct-auth
         (memoize (fn [profile]
                    (reify creds/CredentialsProvider
                      (fetch [_]
                        (creds/valid-credentials
                          {:aws/access-key-id     (get profile "aws_access_key_id")
                           :aws/secret-access-key (get profile "aws_secret_access_key")
                           :aws/session-token     (get profile "aws_session_token")}
                          "aws profile")))))]
     (creds/cached-credentials-with-auto-refresh
       (reify creds/CredentialsProvider
         (fetch [_]
           (when (.exists f)
             (try
               (let [profile (get (parse f) profile-name)]
                 (if (assume-role-profile? profile)
                   (creds/fetch (delegate-assume-role profile))
                   (creds/fetch (delegate-direct-auth profile))))
               (catch Throwable t
                 (log/error t (format "Error fetching credentials from aws profiles file for profile %s" profile-name)))))))))))

(defn default-credentials-provider
  ([] (default-credentials-provider (aws/default-http-client)))
  ([http-client]
   (creds/chain-credentials-provider
     [(creds/environment-credentials-provider)
      (creds/system-property-credentials-provider)
      (profile-credentials-provider nil http-client)
      (creds/container-credentials-provider http-client)
      (creds/instance-profile-credentials-provider http-client)])))

(def ^:private shared-credential-provider
  (delay (default-credentials-provider)))

(defn credentials-provider []
  (force shared-credential-provider))

(defn assume-role-credentials-provider
  ([role-arn]
   (let [client (aws/client {:api :sts :credentials-provider (credentials-provider)})]
     (assume-role-credentials-provider client role-arn)))
  ([client role-arn]
   (assume-role-credentials-provider client role-arn (str (gensym "aws-api-session-"))))
  ([client role-arn role-session-name]
   (creds/cached-credentials-with-auto-refresh
     (reify creds/CredentialsProvider
       (fetch [_]
         (let [response (aws/invoke client {:op :AssumeRole :request {:RoleArn role-arn :RoleSessionName role-session-name}})]
           (if (anomaly? response)
             (throw (ex-info (format "Error assuming role %s" role-arn) {:response response}))
             (creds/valid-credentials
               {:aws/access-key-id             (get-in response [:Credentials :AccessKeyId])
                :aws/secret-access-key         (get-in response [:Credentials :SecretAccessKey])
                :aws/session-token             (get-in response [:Credentials :SessionToken])
                :cognitect.aws.credentials/ttl (creds/calculate-ttl (get-in response [:Credentials]))}
               "aws assume role"))))))))