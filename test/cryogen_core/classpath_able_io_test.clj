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
            [cryogen-core.classpath-able-io :as sut]))

(s/set-fn-validation! true)

(def theme "bootstrap4-test")

(def target "target/tmp")

; TODO: Fix this test!
(deftest test-file-from-cp
  (is
   (sut/file-from-cp
    "dummy")))

(deftest test-resource-from-cp-or-fs
  (is
   (.exists
    (:file
     (sut/resource-from-cp-or-fs
      "./test-resources/"
      "templates/themes/bootstrap4-test"
      "js"))))
  (is
   (.exists
    (:file
     (sut/resource-from-cp-or-fs
      "./" "" ".gitkeep"))))
  (is
   (some? (sut/resource-from-cp-or-fs
           "./test-resources/"
           "templates/themes/bootstrap4-test"
           "js")))
  (is
   (some? (sut/resource-from-cp-or-fs
           "./not-existing-so-load-from-cp" "" ".gitkeep")))
  (is (=
       {:path          "js/subdir"
        :source-type   :classpath
        :resource-type :dir}
       (ftt/filter-object
        (sut/resource-from-cp-or-fs
         "./not-existing-so-load-from-cp"
         "templates/themes/bootstrap4-test"
         "js/subdir")))))

(deftest test-get-resources-recursive
  (is (=
       []
       (sut/get-resources-recursive "" "templates/themes/bootstrap4-test" ["not-existing"])))
  (is (=
       [{:path          "js/dummy.js"
         :source-type   :classpath
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
       (sort (map :path
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
       (sort (map :path
                  (sut/get-resources-recursive
                   "" "templates/themes/bootstrap4-test" ["."]))))))

(deftest test-distinct-resources-by-path
  (is (= [{:path "pages/test"}
          {:path "pages/test1"}
          {:path "pages/test2"}]
         (sut/distinct-resources-by-path [{:path "pages/test"}
                                          {:path "pages/test1"}
                                          {:path "pages/test2"}
                                          {:path "pages/test1"}]))))

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
