;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.file-test-tools
  (:require [clojure.java.io :as io]
            [schema.core :as s])
  (:import [java.nio.file Files LinkOption]))

(def no-link-option (into-array [LinkOption/NOFOLLOW_LINKS]))

(defn verify-path-exists [path]
  (Files/exists path no-link-option))

(defn verify-file-exists [path]
  (io/file path))

(defn verify-dir-exists [path]
  (and (verify-file-exists path)
       (.isDirectory (io/file path))))

(defn filter-object
  [e]
  {:virtual-path  (:virtual-path e)
   :source-type   (:source-type e)
   :resource-type (:resource-type e)})

(defn filter-path
  [e]
  (:virtual-path e))
