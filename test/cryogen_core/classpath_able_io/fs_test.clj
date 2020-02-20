;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.fs-test
  (:require [clojure.test :refer :all]
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

(deftest should-return-nil-on-not-existing-path
  (is
   (= nil
      (sut/path-if-exists fs-root "not-existing"))))

(deftest should-find-subdir-path
  (is
   (sut/path-if-exists fs-root "/dummy/dummy_from_fs")))

(deftest should-find-path-with-space
  (is
   (sut/path-if-exists fs-root "/File With Space")))

(deftest should-find-path-with-empty-base-path
  (is
   (sut/path-if-exists fs-root nil "/dummy/dummy_from_fs")))

(deftest test-list-entries-for-dir
  (is
   (= ["dummy2"
       "dummy_from_fs"]
      (sort
       (seq
        (sut/list-entries-for-dir
         (sut/create-resource "dummy" (sut/path-if-exists fs-root "dummy") :filesytem)))))))

(deftest test-get-resources
  (is
   (= ["dummy"
       "dummy/dummy2"
       "dummy/dummy2/dummy2_from_fs"
       "dummy/dummy2/dummy_common"
       "dummy/dummy_from_fs"]
      (sort (map ftt/filter-path
                 (sut/get-resources fs-root "" ["dummy"])))))
  (is
   (= [{:virtual-path "dummy_from_fs" :source-type :filesystem :resource-type :file}]
      (map ftt/filter-object
           (sut/get-resources fs-root "dummy" ["dummy_from_fs"]))))
  (is
   (= [{:virtual-path "dummy_from_fs" :source-type :filesystem :resource-type :file}]
      (map ftt/filter-object
           (sut/get-resources fs-root "dummy" ["dummy_from_fs" "not_existing"])))))
