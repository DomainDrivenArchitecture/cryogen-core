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