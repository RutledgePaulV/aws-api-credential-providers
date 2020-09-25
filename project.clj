(defproject org.clojars.rutledgepaulv/aws-api-credential-providers "0.1.0-SNAPSHOT"

  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [com.cognitect.aws/api "0.8.474"]
   [com.cognitect.aws/sts "798.2.678.0"]
   [com.cognitect.aws/endpoints "1.1.11.842"]]

  :repl-options
  {:init-ns aws-api-credential-providers.core})
