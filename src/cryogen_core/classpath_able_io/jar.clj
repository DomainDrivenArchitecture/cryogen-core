;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.jar
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [cryogen-core.classpath-able-io.this :as this]
            [cryogen-core.classpath-able-io.fs :as fs])
  (:import [java.net URI]
           [java.util.jar JarFile JarEntry]
           [java.nio.file Paths Files FileSystems]))

(defn is-from-classpath-jar?
  [uri ;:- JavaUri
   ]
  (= (.getScheme uri) "jar"))

(defn
  filesystem-uri
  [resource-uri ;:- JavaUri
   ]
  (URI. (first (st/split (.toString resource-uri) #"!"))))

(defn init-file-system
  [resource-uri ;:- JavaUri
   ]
  (try
    (FileSystems/getFileSystem (filesystem-uri resource-uri))
    (catch Exception e
      (FileSystems/newFileSystem (filesystem-uri resource-uri) {}))))

(defn path-if-exists ;:- JavaPath
  [& path-elements ;:- VirtualPath
   ]
  (try
    (let [resource-uri
          (.toURI (io/resource
                   (apply this/virtual-path-from-elements path-elements)))]
      (when (is-from-classpath-jar? resource-uri)
        (do
          (init-file-system resource-uri)
          (when (Files/exists (Paths/get resource-uri) fs/no-link-option)
            (Paths/get resource-uri)))))
    (catch Exception e
      nil)))

(defn create-resource
  ([virtual-path ;:- VirtualPath
    java-path ;:- JavaPath
    ]
   (if (nil? java-path)
     nil
     (let [java-uri (.toUri java-path)]
       {:virtual-path  virtual-path
        :java-uri      java-uri
        :java-path     java-path
        :source-type   :java-classpath-jar
        :resource-type (cond
                         (Files/isDirectory java-path fs/no-link-option) :dir
                         (Files/isRegularFile java-path fs/no-link-option) :file
                         :else :unknown)}))))

(defn filter-and-remove-for-dir
  [base-path-to-filter-for
   elements-list]
  (let [norm-path-to-filter-for  (str base-path-to-filter-for "/")
        start (count norm-path-to-filter-for)]
     (map
      (fn [el]
        (let [end (if (st/ends-with? el "/") (dec (count el)) (count el))]
          (subs el start end)))
      (filter
       (fn [element] (and (st/starts-with? element norm-path-to-filter-for)
                          (not (= element norm-path-to-filter-for))))
       elements-list))))

(defn filter-and-remove-for-path
  [path-to-filter-for
   virtual-paths-list]
  (filter
   #(st/starts-with? % path-to-filter-for)
   virtual-paths-list))

(defn jar-file-for-resource
  [resource]
  (JarFile.
   (.toFile
    (Paths/get
     (URI.
      (.getSchemeSpecificPart
       (filesystem-uri (:java-uri resource))))))))

(defn list-entries ;:- [s/Str]
  [resource ;:- Resource
   ]
  (map #(.getName ^JarEntry %)
       (enumeration-seq
        (.entries
         (jar-file-for-resource resource)))))

(defn list-entries-for-dir ;:- [VirtualPath]
  [base-path ;:- VirtualPath
   resource ;:- Resource
   ]
  (if (nil? resource)
    []
    (filter-and-remove-for-dir
     base-path
     (list-entries resource))))

(defn get-resources;:- [Resource]
  "base-path is sensible for getting the right jar from classpath. So base-path 
   should be specific enough for the jar desired. Paths must not be empty."
  [base-path ;:- VirtualPath
   paths ;:- [VirtualPath]
   ]
  (let [entry-list (flatten 
                    (map
                     (fn [p]
                       (filter-and-remove-for-path
                        p
                        (list-entries-for-dir
                         base-path
                         (create-resource p (path-if-exists base-path p)))))
                     paths))]
    (map (fn [entry]
           (create-resource entry (path-if-exists base-path entry)))
         entry-list)))
