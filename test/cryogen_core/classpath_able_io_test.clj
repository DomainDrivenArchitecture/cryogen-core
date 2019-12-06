;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [cryogen-core.classpath-able-io :as sut]))

(def theme "bootstrap4-test")

(def target "target/tmp")

(defn verify-file-exists [path]
  (.exists (io/file path)))

(defn verify-dir-exists [path]
  (and (verify-file-exists path)
       (.isDirectory (io/file path))))

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

(deftest test-copy-resources-from-theme
  (is (do
         (sut/copy-resources-from-theme "./" theme target)
         (and (verify-dir-exists
               (str target "/templates/themes/bootstrap4-test/js"))
              (verify-file-exists
               (str target "/templates/themes/bootstrap4-test/js/dummy.js"))
              (verify-dir-exists
               (str target "/templates/themes/bootstrap4-test/js/subdir"))
              (verify-file-exists
               (str target "/templates/themes/bootstrap4-test/js/subdir/subdummy.js"))
              ))))