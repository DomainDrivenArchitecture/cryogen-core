;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.type
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [schema.core :as s]
            [cryogen-core.classpath-able-io.fs :as fs])
  (:import [java.net URI]
           [java.util.jar JarFile JarEntry]
           [java.nio.file FileSystems Paths Files LinkOption StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

; -------------------- Domain Definition ------------------------------
(def SourceType (s/enum :java-classpath-filesystem :java-classpath-jar :filesystem))
(def ResourceType (s/enum :file :dir :unknown))
(def Prefix s/Str)
(def JavaUri s/Any) ; java.net.URI
(def VirtualPath s/Str)
(def JavaPath s/Any) ; java.nio.Path
(def Resource
  {:virtual-path  VirtualPath
   :source-type   SourceType
   :resource-type ResourceType
   :java-uri      JavaUri
   :java-path     JavaPath})

(defn create-resource
  ([virtual-path
    java-path]
   {:virtual-path  virtual-path
    :java-uri      (.toUri java-path)
    :java-path     java-path
    :source-type   (fs/source-type)
    :resource-type (fs/resource-type java-path)}))

(defn
  list-entries-for-dir
  [resource]
  (fs/list-entries-for-dir (:java-path resource)))