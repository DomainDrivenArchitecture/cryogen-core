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

(defn filter-object
  [e]
  {:path          (:path e)
   :source-type   (:source-type e)
   :resource-type (:resource-type e)})

(deftest test-uri-from-cp
  (is
   (sut/file-from-cp ".gitkeep")))

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
       (filter-object
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
       (map filter-object
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

(deftest test-get-distinct-markup-dirs
  (is (=
       ["test_pages"
        "test_pages/home"
        "test_posts"
        "test_posts/home"]
       (sort (map :path
                  (sut/get-distinct-markup-dirs
                   "not-existing-get-from-cp"
                   "test_posts" "test_pages"
                   ""))))))

(deftest test-distinct-resources-by-path
  (is (= [{:path "pages/test"}
          {:path "pages/test1"}
          {:path "pages/test2"}]
         (sut/distinct-resources-by-path [{:path "pages/test"}
                                          {:path "pages/test1"}
                                          {:path "pages/test2"}
                                          {:path "pages/test1"}]))))

(deftest test-create-dirs-from-markup-folders!
  (is (do
        (sut/delete-resource-recursive! (str target "2"))
        (sut/create-dirs-from-markup-folders!
         "not-existing-get-from-cp" "test_posts" "test_pages"
         (str target "2") "")
        (and (verify-dir-exists
              (str (str target "2") "/test_pages"))
             (verify-dir-exists
              (str (str target "2") "/test_posts"))
             (verify-dir-exists
              (str (str target "2") "/test_pages/home"))))))

(deftest test-delete-resource-recursive!
  (is
   (do
     (.mkdir (io/file target))
     (sut/delete-resource-recursive! target)
     (not (verify-dir-exists target)))))

(deftest test-filter-for-ignore-patterns
  (is (=
       ["file.js"]
       (sut/filter-for-ignore-patterns #".*\.ignore" ["file.js" "file.ignore"]))))

(deftest test-copy-resources-from-theme!  (is (do
                                                (sut/delete-resource-recursive! target)
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
                                                      (str target "/404.html"))))))
