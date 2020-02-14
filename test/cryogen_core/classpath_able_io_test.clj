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

(deftest test-get-resources-recursive
  (is (=
       []
       (sut/get-resources-recursive "" "templates/themes/bootstrap4-test" ["not-existing"])))
  (is (=
       [{:virtual-path "dummy_from_jar", :source-type :java-classpath-jar, :resource-type :file}]
       (map ftt/filter-object
            (sut/get-resources-recursive "not-existing" "dummy" ["dummy_from_jar"]))))
  (is (=
       (sort ["dummy2/dummy2_from_jar" "dummy2/dummy_common" "dummy2" "dummy_from_jar" "dummy_from_fs"
              "dummy2/dummy2_from_fs"])
       (sort (map ftt/filter-path
                  (sut/get-resources-recursive "fs_root" "dummy" 
                                               ["dummy_from_jar" "dummy_from_fs" "dummy2"])))))
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
                   "test-resources" "templates/themes/bootstrap4-test" ["."]))))))

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
   (let [target-tmp "target/tmp3"]
     (.mkdir (io/file target-tmp))
     (.createNewFile (io/file (str target-tmp "/file-tmp")))
     (sut/delete-resource-recursive! target-tmp)
     (not (ftt/verify-dir-exists target-tmp)))))
