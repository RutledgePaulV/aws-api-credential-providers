[![Build Status](https://travis-ci.com/rutledgepaulv/aws-api-credential-providers.svg?branch=master)](https://travis-ci.com/rutledgepaulv/aws-api-credential-providers)
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.rutledgepaulv/aws-api-credential-providers.svg)](https://clojars.org/org.clojars.rutledgepaulv/aws-api-credential-providers)

An improved implementation of `#'cognitect.aws.credentials/profile-credentials-provider` that handles
sts role assumptions and source_profile relationships in your ~/.aws/credentials file like the java sdk 
would.


```clojure

(require '[aws-api-credential-providers.core :as providers])
(require '[cognitect.aws.client.api :as aws])

(def opts {:api :sts :credentials-provider (providers/credentials-provider)})
(def client (aws/client opts))
(def whoami (aws/invoke client {:op :GetCallerIdentity}))


```
