(ns cryogen-core.classpath-able-io-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [cryogen-core.io :as sut]))

(def theme "bootstrap4-test")

(def target "target/tmp")

(deftest test-copy-resources-from-theme
  (is (do
        (sut/copy-resources-from-theme theme target)
        (verify-dir-exists (str target "/js")
        (verify-file-exists (str target "/js/dummy.js")))))