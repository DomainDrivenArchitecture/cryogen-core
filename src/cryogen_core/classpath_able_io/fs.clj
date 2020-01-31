;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.fs
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [schema.core :as s])
  (:import [java.net URI]
           [java.util.jar JarFile JarEntry]
           [java.nio.file FileSystems Paths Files LinkOption StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

; ----------------------- Domain functions ------------------------
(def no-link-option (into-array [LinkOption/NOFOLLOW_LINKS]))
(def follow-link-option (into-array []))

(defn user-dir []
  (java.lang.System/getProperty "user.dir"))

(s/defn path
  [full-path]
  (let [path-from-fs (Paths/get (URI. (str "file://" (user-dir) "/" full-path)))]
    (try
      (when (Files/exists path-from-fs follow-link-option)
        path-from-fs)
      (catch Exception e
        nil))))

(defn create-resource
  ([virtual-path
    java-path]
   {:virtual-path  virtual-path
    :java-uri      (.toUri java-path)
    :java-path     java-path
    :source-type   :filesystem
    :resource-type (cond
                     (Files/isDirectory java-path no-link-option) :dir
                     (Files/isRegularFile java-path no-link-option) :file
                     :else :unknown)}))

; ------------------- infra ---------------------------------
(defn user-dir []
  (java.lang.System/getProperty "user.dir"))

(defn path-from-fs
  [full-path]
  (let [path-from-fs (Paths/get (URI. (str "file://" full-path)))] ;fragile
    (try
      (when (Files/exists path-from-fs no-link-option)
        path-from-fs)
      (catch Exception e
        nil))))

 (defn
   list-entries-for-dir
   [resource]
   (.list (.toFile (:java-path resource))))
