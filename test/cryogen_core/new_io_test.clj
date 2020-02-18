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
       (sort (map :virtual-path
                  (sut/get-distinct-markup-dirs
                   "./not-existing-get-from-cp"
                   "test_posts" "test_pages"
                   ""))))))

(deftest test-create-dirs-from-markup-folders!
  (is 
   (let [target-tmp "target/tmp-test-create-dirs-from-markup-folders"]
     (sut/delete-resources! target-tmp)
     (sut/create-dirs-from-markup-folders!
      "./not-existing-get-from-cp" 
      "test_posts" 
      "test_pages"
      target-tmp "")
     (and (ftt/verify-dir-exists
           (str target-tmp "/test_pages"))
          (ftt/verify-dir-exists
           (str target-tmp "/test_posts"))
          (ftt/verify-dir-exists
           (str target-tmp "/test_pages/home"))))))

(deftest test-copy-resources-from-theme!  
  (is 
   (let [target-tmp "target/tmp-test-copy-resources-from-theme"]
     (sut/delete-resources! target-tmp)
     (sut/copy-resources-from-theme! "./" theme target-tmp "")
     (and (ftt/verify-dir-exists
           (str target-tmp "/js"))
          (ftt/verify-file-exists
           (str target-tmp "/js/dummy.js"))
          (ftt/verify-dir-exists
           (str target-tmp "/js/subdir"))
          (ftt/verify-file-exists
           (str target-tmp "/js/subdir/subdummy.js"))
          (ftt/verify-file-exists
           (str target-tmp "/css/dummy.css"))
          (ftt/verify-file-exists
           (str target-tmp "/404.html"))))))
