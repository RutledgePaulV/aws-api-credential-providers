[![Build Status](https://travis-ci.com/rutledgepaulv/aws-api-credential-providers.svg?branch=master)](https://travis-ci.com/rutledgepaulv/aws-api-credential-providers)
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.rutledgepaulv/aws-api-credential-providers.svg)](https://clojars.org/org.clojars.rutledgepaulv/aws-api-credential-providers)

An extension library for [aws-api](https://github.com/cognitect-labs/aws-api) providing credential providers suitable
for more complex AWS authentication configurations. You might find this helpful if you need to assume IAM roles or use
tools like saml2aws or AWS SSO.

---


### `assume-role-credentials-provider`

Handles sts role assumptions by using an existing credential provider to make sts:AssumeRole calls.

```clojure

(require '[aws-api-credential-providers.core :as providers])
(require '[cognitect.aws.client.api :as aws])

(def role-to-assume "arn:aws:iam::111111111111:role/MoopsNotMoors")

; if you're comfortable using the default credential chain you can just do this
(def credential-provider (providers/assume-role-credentials-provider role-to-assume))
(def assume-opts {:api :sts :credentials-provider credential-provider)
(def assumed-role-client (aws/client assume-opts))

; this will represent the role you assumed                      
(def whoami (aws/invoke assumed-role-client {:op :GetCallerIdentity}))


; if you need a custom credential provider to even issue the assume role calls
(def delegate-credential-provider (providers/credentials-provider))
(def delegate-client-opts {:api :sts :credentials-provider delegate-credential-provider)
(def delegate-client (aws/client delegate-client-opts))

; now make the credential provider that wraps that delegate credential provider to assume a role
(def credential-provider (providers/assume-role-credentials-provider delegate-client role-to-assume))
(def assume-opts {:api :sts :credentials-provider credential-provider)
(def assumed-role-client (aws/client assume-opts))

; this will represent the role you assumed                        
(def whoami (aws/invoke assumed-role-client {:op :GetCallerIdentity}))

```


### `profile-credentials-provider` 

Handles `role_arn` and `source_profile` attributes in your ~/.aws/credentials file like the Java sdk would.
This is intended to be a more complete version of the existing profile-credentials-provider bundled with aws-api.

```clojure

(require '[aws-api-credential-providers.core :as providers])
(require '[cognitect.aws.client.api :as aws])

; uses your AWS_PROFILE environment variable by default, or you may pass in a profile name
(def opts {:api :sts :credentials-provider (providers/profile-credentials-provider)})
(def client (aws/client opts))
(def whoami (aws/invoke client {:op :GetCallerIdentity}))

```
