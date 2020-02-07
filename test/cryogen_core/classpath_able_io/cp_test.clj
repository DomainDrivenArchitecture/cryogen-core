;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.cp-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [schema.core :as s]
            [cryogen-core.file-test-tools :as ftt]
            [cryogen-core.classpath-able-io.cp :as sut]))

(deftest should-find-path-on-cp
  (is
   (sut/path-if-exists "dummy"))
  (is
   (sut/path-if-exists "dummy" "dummy_from_jar"))
  (is
   (sut/path-if-exists "dummy_only_in_cp_fs")))


(deftest should-get-resources-from-jar-and-fs-classpath
  (is (=
       []
       (sut/get-resources "" ["not-existing"])))
  (is (=
       [{:virtual-path "dummy_from_jar", :source-type :java-classpath-jar, :resource-type :file}]
       (map ftt/filter-object
            (sut/get-resources "dummy" ["dummy_from_jar"]))))
  (is (=
       [{:virtual-path "test_pages/home" :source-type :java-classpath-filesystem :resource-type :dir}
        {:virtual-path "test_pages/home/.gitkeep" :source-type :java-classpath-filesystem :resource-type :file}
        {:virtual-path "test_posts/home" :source-type :java-classpath-filesystem :resource-type :dir}
        {:virtual-path "test_posts/home/.gitkeep" :source-type :java-classpath-filesystem :resource-type :file}]
       (map ftt/filter-object
            (sut/get-resources "templates/md" ["test_pages/home" "test_posts/home"])))))