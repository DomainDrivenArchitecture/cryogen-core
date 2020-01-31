;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.fs-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [schema.core :as s]
            [cryogen-core.file-test-tools :as ftt]
            [cryogen-core.classpath-able-io.fs :as sut]))

(def fs-root "fs_root")

(deftest test-path
  (is
   (sut/path (str fs-root "/dummy/dummy_from_fs")))
  (is
   (= nil 
      (sut/path (str fs-root "/not-existing")))))
