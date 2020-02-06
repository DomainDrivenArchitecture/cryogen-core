;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.fs
  (:require [cryogen-core.classpath-able-io.type :as type])
  (:import [java.net URI]
           [java.nio.file Paths Files LinkOption]))

; ----------------------- Domain functions ------------------------
(def no-link-option (into-array [LinkOption/NOFOLLOW_LINKS]))
(def follow-link-option (into-array LinkOption []))

(defn user-dir []
  (java.lang.System/getProperty "user.dir"))

(defn absolut-path
  [& path-elements]
  (let [path (.normalize
              (Paths/get (first path-elements)
                         (into-array String (rest path-elements))))]
    (if (.isAbsolute path)
      path
      (Paths/get (user-dir) (into-array String path-elements)))))

(defn path-if-exists
  [& path-elements]
  (let [path-from-fs (Paths/get (URI. (str "file://" (apply absolut-path path-elements))))]
    (when (Files/exists path-from-fs follow-link-option)
        path-from-fs)))

(defn create-resource
  ([virtual-path
    java-path]
   (if (nil? java-path)
     nil
     {:virtual-path  virtual-path
      :java-uri      (.toUri java-path)
      :java-path     java-path
      :source-type   :filesystem
      :resource-type (cond
                       (Files/isDirectory java-path follow-link-option) :dir
                       (Files/isRegularFile java-path follow-link-option) :file
                       :else :unknown)}))
  ([fs-prefix
    base-path
    virtual-path]
   (create-resource virtual-path (path-if-exists fs-prefix base-path virtual-path))))

 (defn
   list-entries-for-dir
   [resource]
   (.list (.toFile (:java-path resource))))

 (defn get-resources ;:- [Resource]
   [fs-prefix ;:- Prefix
    base-path ;:- VirtualPath
    paths ;:- [VirtualPath]
    ]
   (loop [paths  paths
          result []]
     (if (not (empty? paths))
       (let [path-to-work-with     (first paths)
             resource-to-work-with (create-resource
                                    fs-prefix
                                    base-path
                                    path-to-work-with)
             result                (into result
                                         [resource-to-work-with])]
           (cond
             (nil? resource-to-work-with) (recur (drop 1 paths) result)
             (type/is-file? resource-to-work-with)
             (recur (drop 1 paths) result)
             (type/is-dir? resource-to-work-with)
             (recur (into (drop 1 paths)
                          (map #(str path-to-work-with "/" %)
                               (list-entries-for-dir resource-to-work-with)))
                    result)
             :else []))
       (remove nil? result))))
