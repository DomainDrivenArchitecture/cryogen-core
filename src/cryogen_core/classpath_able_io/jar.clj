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
            [cryogen-core.classpath-able-io.type :as type]
            [cryogen-core.classpath-able-io.fs :as fs])
  (:import [java.net URI]
           [java.util.jar JarFile JarEntry]
           [java.nio.file Paths Files FileSystems]))

(defn is-from-classpath-jar?
  [uri ;:- JavaUri
   ]
  (= (.getScheme uri) "jar"))

(defn create-resource
  ([virtual-path ;:- VirtualPath
    java-path ;:- JavaPath
    ]
   (let [java-uri (.toUri java-path)]
     {:virtual-path  virtual-path
      :java-uri      java-uri
      :java-path     java-path
      :source-type   :java-classpath-jar
      :resource-type (cond
                       (Files/isDirectory java-path fs/no-link-option) :dir
                       (Files/isRegularFile java-path fs/no-link-option) :file
                       :else :unknown)})))

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

; (path-if-exists "") => ".../test"
(defn path-if-exists ;:- JavaPath
  [& path-elements ;:- VirtualPath
   ]
  (try
    (let [resource-uri 
          (.toURI (io/resource 
                   (st/join "/" 
                            (filter #(not (empty? %)) 
                                    path-elements))))]
      (when (is-from-classpath-jar? resource-uri)
        (init-file-system resource-uri))
      ;; TODO: hier steckt auch eine "from-fs-cp" funktionalität drinne
      (when (Files/exists (Paths/get resource-uri) fs/no-link-option)
        (Paths/get resource-uri)))
    (catch Exception e
      nil)))

(defn filter-and-remove-for-dir
  [base-path-to-filter-for
   elements-list]
     (map
      (fn [el]
        (if (st/ends-with? el "/")
          (st/join "" (drop-last el))
          el))
      (filter
       (fn [element] (and (st/starts-with? element base-path-to-filter-for)
                          (not (= element base-path-to-filter-for))))
       elements-list)))

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
  [resource ;:- Resource
   ]
  (filter-and-remove-for-dir
   (:virtual-path resource)
   (list-entries resource)))

(defn get-resources;:- [Resource]
  [base-path ;:- VirtualPath
   paths ;:- [VirtualPath]
   ]
  (let [entry-list (flatten 
                    (map
                     (fn [p]
                       (let [p (if (empty? base-path) p (str base-path "/" p))]
                         (list-entries-for-dir
                          (create-resource p
                                           (path-if-exists p)))))
                     paths))]
    (map (fn [entry]
           (create-resource entry (path-if-exists entry)))
         entry-list)))
