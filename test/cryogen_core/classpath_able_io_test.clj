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
            [cryogen-core.classpath-able-io :as sut]))

(s/set-fn-validation! true)

(def theme "bootstrap4-test")

(def target "target/tmp")

(defn verify-file-exists [path]
  (.exists (io/file path)))

(defn verify-dir-exists [path]
  (and (verify-file-exists path)
       (.isDirectory (io/file path))))

(deftest test-file-from-cp-or-filesystem
  (is
   (some? (sut/file-from-cp-or-filesystem
           "./test-resources/"
           "templates/themes/bootstrap4-test/js")))
  (is
   (some? (sut/file-from-cp-or-filesystem
             "./not-existing-so-load-from-cp" ".gitkeep"))))

(deftest test-get-resource-paths-recursive
  (is (=
       []
       (sut/get-resource-paths-recursive "" "templates/themes/bootstrap4-test" ["not-existing"])))
  (is (=
       ["js/dummy.js"]
       (sut/get-resource-paths-recursive 
        "" "templates/themes/bootstrap4-test" ["js/dummy.js"])))
  (is (=
       []
       (sut/get-resource-paths-recursive 
        "" "templates/themes/bootstrap4-test" ["js/dummy.js"]
        :from-cp false)))
  (is (=
       ["js/subdir" 
        "js/subdir/test.js" 
        "js/subdir/subdummy.js"]
       (sut/get-resource-paths-recursive 
        "" "templates/themes/bootstrap4-test" ["js/subdir"])))
  (is (=
       ["."
        "./css"
        "./css/dummy.css"
        "./js"
        "./js/subdir"
        "./js/subdir/test.js"
        "./js/subdir/subdummy.js"
        "./js/dummy.js"
        "./html"
        "./html/403.html"
        "./html/404.html"]
       (sut/get-resource-paths-recursive "" "templates/themes/bootstrap4-test" ["."])))
  )

(deftest test-delete-resource-recursive
  (is
   (do
     (.mkdir (io/file (str "target/tmp" target)))
     (sut/delete-resource-recursive! (str "target/tmp" target))
     (not (verify-dir-exists (str "target/tmp" target))))))

(deftest test-filter-for-ignore-patterns
  (is (=
       ["file.js"]
       (sut/filter-for-ignore-patterns #".*\.ignore" ["file.js" "file.ignore"]))))

(deftest test-file-from-cp
  (is 
   (sut/file-from-cp ".gitkeep")))

(deftest test-file-from-cp-or-filesystem
  (is
   (.exists (sut/file-from-cp-or-filesystem 
             "./test-resources/" "templates/themes/bootstrap4-test/js")))
  (is
   (.exists (sut/file-from-cp-or-filesystem 
             "./" ".gitkeep"))))

(deftest test-copy-resources-from-theme!  (is (do
        (sut/delete-resource-recursive! (str "target/tmp" target))
        (sut/copy-resources-from-theme! "./" theme target "")
        (and (verify-dir-exists
              (str target "/js"))
             (verify-file-exists
              (str target "/js/dummy.js"))
             (verify-dir-exists
              (str target "/js/subdir"))
             (verify-file-exists
              (str target "/js/subdir/subdummy.js"))
             (verify-file-exists
              (str target "/css/dummy.css"))
             (verify-file-exists
              (str target "/html/404.html"))
             ))))
