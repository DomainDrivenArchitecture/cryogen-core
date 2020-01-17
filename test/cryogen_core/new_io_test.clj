;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.new-io-test
  (:require [clojure.test :refer :all]
            [schema.core :as s]
            [cryogen-core.file-test-tools :as ftt]
            [cryogen-core.new-io :as sut]))

(s/set-fn-validation! true)

(def theme "bootstrap4-test")

(def target "target/tmp")

(deftest test-get-distinct-markup-dirs
  (is (=
       ["test_pages"
        "test_pages/home"
        "test_posts"
        "test_posts/home"]
       (sort (map :short-path
                  (sut/get-distinct-markup-dirs
                   "./not-existing-get-from-cp"
                   "test_posts" "test_pages"
                   ""))))))

(deftest test-create-dirs-from-markup-folders!
  (is (do
        (sut/delete-resource-recursive! (str target "2"))
        (sut/create-dirs-from-markup-folders!
         "./not-existing-get-from-cp" 
         "test_posts" 
         "test_pages"
         (str target "2") "")
        (and (ftt/verify-dir-exists
              (str (str target "2") "/test_pages"))
             (ftt/verify-dir-exists
              (str (str target "2") "/test_posts"))
             (ftt/verify-dir-exists
              (str (str target "2") "/test_pages/home"))))))

(deftest test-copy-resources-from-theme!  (is (do
                                                (sut/delete-resource-recursive! target)
                                                (sut/copy-resources-from-theme! "./" theme target "")
                                                (and (ftt/verify-dir-exists
                                                      (str target "/js"))
                                                     (ftt/verify-file-exists
                                                      (str target "/js/dummy.js"))
                                                     (ftt/verify-dir-exists
                                                      (str target "/js/subdir"))
                                                     (ftt/verify-file-exists
                                                      (str target "/js/subdir/subdummy.js"))
                                                     (ftt/verify-file-exists
                                                      (str target "/css/dummy.css"))
                                                     (ftt/verify-file-exists
                                                      (str target "/404.html"))))))
