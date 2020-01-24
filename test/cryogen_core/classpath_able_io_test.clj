;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [schema.core :as s]
            [cryogen-core.file-test-tools :as ftt]
            [cryogen-core.classpath-able-io :as sut])
  (:import [java.nio.file FileSystems Paths Files LinkOption StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(s/set-fn-validation! true)

(def theme "bootstrap4-test")

(def target "target/tmp")

(deftest test-file-from-cp
  (is
   (sut/path-from-cp "dummy")))

(deftest test-resource-from-cp-or-fs
  (is
   (some? (sut/resource-from-cp-or-fs
           "not-existing-filesystem-path"
           ""
            "dummy")))
  (is
   (Files/exists        
    (:java-path
     (sut/resource-from-cp-or-fs
      "./test-resources"
      "templates/themes/bootstrap4-test"
      "js")) sut/no-link-option))
  (is
   (Files/exists
    (:java-path
     (sut/resource-from-cp-or-fs
      "./" "" ".gitkeep")) sut/no-link-option))
  (is
   (some? (sut/resource-from-cp-or-fs
           "./test-resources"
           "templates/themes/bootstrap4-test"
           "js")))
  (is
   (some? (sut/resource-from-cp-or-fs
           "./not-existing-so-load-from-cp" "" ".gitkeep")))
  (is (=
       {:virtual-path    "js/subdir"
        :source-type   :java-classpath-filesystem
        :resource-type :dir}
       (ftt/filter-object
        (sut/resource-from-cp-or-fs
         "./not-existing-so-load-from-cp"
         "templates/themes/bootstrap4-test"
         "js/subdir")))))

(deftest test-list-entries-for-dir
  (is (= ["subdummy.js", "test.js"]
         (seq
          (sut/list-entries-for-dir (sut/resource-from-cp-or-fs
                                     "./not-existing-so-load-from-cp"
                                     "templates/themes/bootstrap4-test"
                                     "js/subdir")))))
  (is (= ["dummy_from_jar"]
         (sut/list-entries-for-dir (sut/resource-from-cp-or-fs
                                    "not-existing-filesystem-path"
                                    ""
                                    "dummy")))))


(deftest test-get-resources-recursive
  (is (=
       []
       (sut/get-resources-recursive "" "templates/themes/bootstrap4-test" ["not-existing"])))
  ; TODO: one dummy from jar and one dummy from cp-filesystem and one from filesystem
  ; get resources and see all
  ; add test here
  ; TODO: fix dir.list on jar
  (is (=
       [{:virtual-path "dummy", :source-type :java-classpath-jar, :resource-type :dir}
        {:virtual-path "dummy/dummy_from_jar", :source-type :java-classpath-jar, :resource-type :file}]
       (map ftt/filter-object
            (sut/get-resources-recursive "not-existing" "" ["dummy"]))))
  (is (=
       [{:virtual-path   "js/dummy.js"
         :source-type   :java-classpath-filesystem
         :resource-type :file}]
       (map ftt/filter-object
            (sut/get-resources-recursive
             "" "templates/themes/bootstrap4-test" ["js/dummy.js"]))))
  (is (=
       []
       (sut/get-resources-recursive
        "" "templates/themes/bootstrap4-test" ["js/dummy.js"] :from-cp false)))
  (is (=
       ["js/subdir"
        "js/subdir/subdummy.js"
        "js/subdir/test.js"]
       (sort (map :virtual-path
                  (sut/get-resources-recursive
                   "" "templates/themes/bootstrap4-test" ["js/subdir"])))))
  (is (=
       ["."
        "./css"
        "./css/dummy.css"
        "./html"
        "./html/403.html"
        "./html/404.html"
        "./js"
        "./js/dummy.js"
        "./js/subdir"
        "./js/subdir/subdummy.js"
        "./js/subdir/test.js"]
       (sort (map :virtual-path
                  (sut/get-resources-recursive
                   "" "templates/themes/bootstrap4-test" ["."]))))))

(deftest test-distinct-resources-by-path
  (is (= [{:virtual-path "pages/test"}
          {:virtual-path "pages/test1"}
          {:virtual-path "pages/test2"}]
         (sut/distinct-resources-by-path [{:virtual-path "pages/test"}
                                          {:virtual-path "pages/test1"}
                                          {:virtual-path "pages/test2"}
                                          {:virtual-path "pages/test1"}]))))

(deftest test-filter-for-ignore-patterns
  (is (=
       ["file.js"]
       (sut/filter-for-ignore-patterns #".*\.ignore" ["file.js" "file.ignore"]))))

(deftest test-delete-resource-recursive!
  (is
   (do
     (.mkdir (io/file target))
     (sut/delete-resource-recursive! target)
     (not (ftt/verify-dir-exists target)))))
