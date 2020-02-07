;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.fs-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [schema.core :as s]
            [cryogen-core.file-test-tools :as ftt]
            [cryogen-core.classpath-able-io.fs :as sut]))

(def fs-root "fs_root")

(deftest test-absolut-path
  (is
   (= (str (sut/user-dir) "/" fs-root "/dummy/dummy_from_fs")
      (.toString (sut/absolut-path fs-root "/dummy/dummy_from_fs"))))
  (is
   (= (str (sut/user-dir) "/" fs-root "/not-existing")
      (.toString (sut/absolut-path (str fs-root "/not-existing"))))))

(deftest test-path-if-exists
  (is
   (sut/path-if-exists (str fs-root "/dummy/dummy_from_fs")))
  (is
   (= nil 
      (sut/path-if-exists (str fs-root "/not-existing")))))

(deftest test-list-entries-for-dir
  (is 
   (= ["dummy2" "dummy_from_fs"]
      (seq
       (sut/list-entries-for-dir
        (sut/create-resource "dummy" (sut/path-if-exists fs-root "dummy")))))))

(deftest test-get-resources
  ;(is
  ; (= [{:virtual-path "dummy" :source-type :filesystem :resource-type :dir}
  ;     {:virtual-path "dummy/dummy_from_fs" :source-type :filesystem :resource-type :file}]
  ;    (map ftt/filter-object
  ;         (sut/get-resources fs-root "" ["dummy"]))))
  (is
   (= [{:virtual-path "dummy_from_fs" :source-type :filesystem :resource-type :file}]
      (map ftt/filter-object
           (sut/get-resources fs-root "dummy" ["dummy_from_fs"]))))
  (is
   (= [{:virtual-path "dummy_from_fs" :source-type :filesystem :resource-type :file}]
      (map ftt/filter-object
           (sut/get-resources fs-root "dummy" ["dummy_from_fs" "not_existing"])))))