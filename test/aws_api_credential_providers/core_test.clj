(ns aws-api-credential-providers.core-test
  (:require [clojure.test :refer :all]
            [aws-api-credential-providers.core :as providers]
            [cognitect.aws.client.api :as aws]))


(comment
  (def opts {:api :sts :credentials-provider (providers/credentials-provider)})
  (def client (aws/client opts))
  (aws/invoke client {:op :GetCallerIdentity}))

