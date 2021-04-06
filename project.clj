(defproject org.clojars.rutledgepaulv/aws-api-credential-providers "0.1.1"

  :description
  "A library providing more featureful implementations of aws authentication providers for cognitect's aws-api library."

  :url
  "https://github.com/rutledgepaulv/aws-api-credential-providers"

  :license
  {:name "MIT License" :url "http://opensource.org/licenses/MIT" :year 2020 :key "mit"}

  :scm
  {:name "git" :url "https://github.com/rutledgepaulv/aws-api-credential-providers"}

  :pom-addition
  [:developers
   [:developer
    [:name "Paul Rutledge"]
    [:url "https://github.com/rutledgepaulv"]
    [:email "rutledgepaulv@gmail.com"]
    [:timezone "-5"]]]

  :deploy-repositories
  [["releases" :clojars]
   ["snapshots" :clojars]]

  :dependencies
  [[org.clojure/clojure "1.10.3"]
   [com.cognitect.aws/api "0.8.505"]
   [com.cognitect.aws/sts "809.2.784.0"]
   [com.cognitect.aws/endpoints "1.1.11.976"]]

  :repl-options
  {:init-ns aws-api-credential-providers.core})
