;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [cryogen-core.classpath-able-io.fs :as fs]
            [cryogen-core.classpath-able-io.jar :as jar]
            [cryogen-core.classpath-able-io.cp :as cp]
            [cryogen-core.classpath-able-io.this :as this]
            [schema.core :as s])
  (:import [java.net URI]
           [java.util.jar JarFile JarEntry]
           [java.nio.file FileSystems Paths Files LinkOption StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def SourceType this/SourceType)
(def ResourceType this/ResourceType)
(def Prefix this/Prefix)
(def JavaUri this/JavaUri) ; java.net.URI
(def VirtualPath this/VirtualPath)
(def JavaPath this/JavaPath) ; java.nio.Path
(def Resource this/Resource)

(s/defn create-fs-resource :- this/Resource
  ([virtual-path :- this/VirtualPath
    java-path :- this/JavaPath]
   {:virtual-path  virtual-path
    :java-uri      (.toUri java-path)
    :java-path     java-path
    :source-type   :filesystem
    :resource-type (cond
                     (Files/isDirectory java-path fs/no-link-option) :dir
                     (Files/isRegularFile java-path fs/no-link-option) :file
                     :else :unknown)}))


(s/defn create-cp-resource :- this/Resource
  ([virtual-path :- this/VirtualPath
    java-path :- this/JavaPath]
   (let [java-uri (.toUri java-path)]
     {:virtual-path  virtual-path
      :java-uri      java-uri
      :java-path     java-path
      :source-type   (cond (jar/is-from-classpath-jar? java-uri)
                           :java-classpath-jar
                           :else :java-classpath-filesystem)
      :resource-type (cond
                       (Files/isDirectory java-path fs/no-link-option) :dir
                       (Files/isRegularFile java-path fs/no-link-option) :file
                       :else :unknown)})))

(defn filter-for-ignore-patterns
  [ignore-patterns source-list]
  (filter #(not (re-matches ignore-patterns %)) source-list))

; ------------------- infra ---------------------------------

; TODO replace this fn ?
(defn path
  "Creates path from given parts, ignore empty elements"
  [& path-parts]
  (->> path-parts
       (remove st/blank?)
       (st/join "/")
       (#(st/replace % #"/+" "/"))))

; contains either a jar or a file
(s/defn path-from-cp ;:- JavaPath
  [resource-path :- this/VirtualPath]
  (try
    (let [resource-uri (.toURI (io/resource resource-path))]
      (when (jar/is-from-classpath-jar? resource-uri)
        (jar/init-file-system resource-uri))
      (when (Files/exists (Paths/get resource-uri) fs/no-link-option)
        (Paths/get resource-uri)))
    (catch Exception e
      nil)))

(s/defn path-from-fs ;:- JavaPath
  [full-path :- this/VirtualPath]
  (let [path-from-fs (Paths/get (URI. (str "file://" full-path)))] ;fragile
    (try
      (when (Files/exists path-from-fs fs/no-link-option)
        path-from-fs)
      (catch Exception e
        nil))))

(defn resource-from-cp-or-fs ;:- Resource 
  [fs-prefix ;:- Prefix
   base-path ;:- VirtualPath
   resource-path ;:- VirtualPath
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [full-path    (.toString (fs/absolut-path fs-prefix base-path resource-path))
        cp-path      (if (empty? base-path)
                        resource-path
                        (str base-path "/" resource-path))
        path-from-fs (if from-fs
                       (path-from-fs full-path)
                       nil)
        path-from-cp (if from-cp
                       (path-from-cp cp-path)
                       nil)]
    (cond
      (some? path-from-fs)
      (create-fs-resource resource-path path-from-fs)
      (some? path-from-cp)
      (create-cp-resource resource-path path-from-cp)
      :else nil)))

(defn path-from-cp-or-fs ; :- JavaPath
  [fs-prefix ; :- Prefix
   base-path ; :- VirtualPath
   resource-path; :- VirtualPath
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [resource (resource-from-cp-or-fs
                  fs-prefix base-path resource-path
                  :from-cp from-cp
                  :from-fs from-fs)]
    (when (some? resource)
      (:java-path resource))))

(defn filter-and-remove-for-dir
  [path-to-filter-for
   elements-list]
  (let [norm-path-to-filter-for  (str path-to-filter-for "/")]
    (map 
     #(subs % (count norm-path-to-filter-for))
     (filter
      (fn [element] (and (st/starts-with? element norm-path-to-filter-for)
                         (not (= element norm-path-to-filter-for))))
      elements-list))))

(s/defn
  list-entries-for-dir ;:- [VirtualPath]
  [resource :- this/Resource]
  (if (= :java-classpath-jar (:source-type resource))
    (filter-and-remove-for-dir
     (:virtual-path resource)
     (map #(.getName ^JarEntry %)
          (enumeration-seq
           (.entries
            (jar/jar-file-for-resource resource)))))
    (.list (.toFile (:java-path resource)))))

(defn get-resources-recursive ;:- [Resource]
  [fs-prefix ;:- Prefix
   base-path ;:- VirtualPath
   paths ;:- [VirtualPath]
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (loop [paths  paths
         result []]
    (if (not (empty? paths))
      (do
        (let [path-to-work-with     (first paths)
              resource-to-work-with (resource-from-cp-or-fs
                                     fs-prefix
                                     base-path
                                     path-to-work-with
                                     :from-cp from-cp
                                     :from-fs from-fs)
              result                (into result
                                          [resource-to-work-with])]
          ; (println path-to-work-with)
          ; (println (:java-path resource-to-work-with))
          (cond
            (nil? resource-to-work-with) []
            (this/is-file? resource-to-work-with)
            (recur (drop 1 paths) result)
            :else
            (recur (into (drop 1 paths)
                         (map #(str path-to-work-with "/" %)
                              (list-entries-for-dir resource-to-work-with)))
                   result))))
      result)))

; TODO: rename? Allow base-path to be ""?
; base-path must not be ""
(defn get-resources-recursive-new ;:- [Resource]
  [fs-prefix ;:- Prefix
   base-path ;:- VirtualPath
   paths ;:- [VirtualPath]
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [fs-resources (fs/get-resources fs-prefix base-path paths)
        cp-resources (cp/get-resources base-path paths)])
  )

 (defn get-resource-paths-recursive ;:- [VirtualPath]
   [fs-prefix ;:- Prefix
    base-path ;:- VirtualPath
    paths ;:- [VirtualPath]
    & {:keys [from-cp from-fs]
       :or   {from-cp true
              from-fs true}}]
   (map #(:virtual-path %)
        (get-resources-recursive
         fs-prefix base-path paths
         :from-cp from-cp
         :from-fs from-fs)))

; TODO: Add files to keep
 (s/defn delete-resource-recursive!
   [virtual-path :- s/Str]
   (let [resource-paths
         (reverse (get-resource-paths-recursive
                   (str (fs/user-dir) "/")
                   virtual-path
                   [""]
                   :from-cp false))]
     (doseq [resource-path resource-paths]
      (Files/delete (fs/absolut-path virtual-path resource-path))
      )))
 
; TODO: add ignore patterns filtering
 (defn copy-resources!
   [fs-prefix ;:- Prefix
    base-path ;:- VirtualPath
    source-paths ;:- [VirtualPath]
    target-path  ;:- VirtualPath
    ignore-patterns ;:- s/Str
    ]
   (let [resource-paths
         (get-resource-paths-recursive fs-prefix base-path source-paths)]
     (if (empty? resource-paths)
       (throw (IllegalArgumentException. (str "resource " resource-paths ", "
                                              source-paths " not found")))
       (doseq [resource-path resource-paths]
         (let [target-file (fs/absolut-path target-path resource-path)
               source-file (path-from-cp-or-fs
                            fs-prefix
                            base-path
                            resource-path)]
           (when (Files/isDirectory source-file fs/no-link-option)
             (Files/createDirectories target-file (into-array FileAttribute [])))
           (when (Files/isRegularFile source-file fs/no-link-option)
             (Files/copy source-file target-file (into-array StandardCopyOption [StandardCopyOption/COPY_ATTRIBUTES StandardCopyOption/REPLACE_EXISTING]))
            ))))))
 
 (defn distinct-resources-by-path
   [resources]
   (loop [paths (set (map :virtual-path resources))
          resources resources
          acc []]
     (cond (empty? resources) acc
           (contains? paths (:virtual-path (first resources))) (recur (disj paths (:virtual-path (first resources)))
                                                                      (rest resources)
                                                                      (conj acc (first resources)))
           :else (recur paths (rest resources) acc))))
