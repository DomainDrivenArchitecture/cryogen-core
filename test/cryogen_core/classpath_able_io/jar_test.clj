;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.jar-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [schema.core :as s]
            [cryogen-core.file-test-tools :as ftt]
            [cryogen-core.classpath-able-io.jar :as sut])
  (:import [java.net URI]))

(deftest test-is-from-classpath-jar?
  (is
   (sut/is-from-classpath-jar? (.toURI (io/resource "dummy")))))

(deftest test-path-if-exists
  (is
   (sut/path-if-exists "dummy/dummy_from_jar")))
(is
 (= nil
    (sut/path-if-exists "not-existing")))

(deftest test-get-resources
  (is
   (= [{:virtual-path "dummy_from_jar" :source-type :java-classpath-jar :resource-type :file}]
      (map ftt/filter-object
           (sut/get-resources "dummy" ["dummy_from_jar"]))))
  (is
   (= [{:virtual-path "dummy2", :source-type :java-classpath-jar, :resource-type :dir}
       {:virtual-path "dummy2/dummy_common", :source-type :java-classpath-jar, :resource-type :file}
       {:virtual-path "dummy2/dummy2_from_jar", :source-type :java-classpath-jar, :resource-type :file}]
      (map ftt/filter-object
           (sut/get-resources "dummy" ["dummy2"])))))
