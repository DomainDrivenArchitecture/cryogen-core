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
            [schema.core :as s])
  (:import [java.net URI]
           [java.util.jar JarFile JarEntry]
           [java.nio.file FileSystems Paths Files SimpleFileVisitor LinkOption StandardCopyOption]
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

; ----------------------- Domain functions ------------------------
(def no-link-option (into-array [LinkOption/NOFOLLOW_LINKS]))

(s/defn is-from-classpath-jar? :- s/Bool
  [uri ;:- JavaUri
   ]
  (= (.getScheme uri) "jar"))

(s/defn create-fs-resource :- Resource
  ([virtual-path :- VirtualPath
    java-path :- JavaPath]
   {:virtual-path  virtual-path
    :java-uri      (.toUri java-path)
    :java-path     java-path
    :source-type   :filesystem
    :resource-type (cond
                     (Files/isDirectory java-path no-link-option) :dir
                     (Files/isRegularFile java-path no-link-option) :file
                     :else :unknown)}))


(s/defn create-cp-resource :- Resource
  ([virtual-path :- VirtualPath
    java-path :- JavaPath]
   (let [java-uri (.toUri java-path)]
     {:virtual-path  virtual-path
      :java-uri      java-uri
      :java-path     java-path
      :source-type   (cond (is-from-classpath-jar? java-uri)
                           :java-classpath-jar
                           :else :java-classpath-filesystem)
      :resource-type (cond
                       (Files/isDirectory java-path no-link-option) :dir
                       (Files/isRegularFile java-path no-link-option) :file
                       :else :unknown)})))

(s/defn is-file? :- s/Bool
  [resource :- Resource]
  (= :file (:resource-type resource)))

(defn filter-for-ignore-patterns
  [ignore-patterns source-list]
  (filter #(not (re-matches ignore-patterns %)) source-list))

; ------------------- infra ---------------------------------
(defn current-path [])

(defn user-dir []
  (java.lang.System/getProperty "user.dir"))

(defn absolut-path
  [& path-elements]
  (let [path (Paths/get (first path-elements) (into-array String (rest path-elements)))]
    (if (.isAbsolute path)
      path
      (Paths/get (user-dir) (into-array String path-elements)))))

(defn path
  "Creates path from given parts, ignore empty elements"
  [& path-parts]
  (->> path-parts
       (remove st/blank?)
       (st/join "/")
       (#(st/replace % #"/+" "/"))))

(s/defn
  filesystem-uri
  [resource-uri :- JavaUri]
  (URI. (first (st/split (.toString resource-uri) #"!"))))

(s/defn init-file-system
  [resource-uri :- JavaUri]
  (try 
      (FileSystems/getFileSystem (filesystem-uri resource-uri))
      (catch Exception e
        (FileSystems/newFileSystem (filesystem-uri resource-uri) {}))))

; contains either a jar or a file
(s/defn path-from-cp ;:- JavaPath
  [resource-path :- VirtualPath]
  (try
    (let [resource-uri (.toURI (io/resource resource-path))]
      (when (is-from-classpath-jar? resource-uri)
        (init-file-system resource-uri))
      (when (Files/exists (Paths/get resource-uri) no-link-option)
        (Paths/get resource-uri)))
    (catch Exception e
      nil)))

(s/defn path-from-fs ;:- JavaPath
  [full-path :- VirtualPath]
  (let [path-from-fs (Paths/get (URI. (str "file://" full-path)))] ;fragile
    (try
      (when (Files/exists path-from-fs no-link-option)
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
  (let [full-path    (.toString (absolut-path fs-prefix base-path resource-path))
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

(s/defn
  list-entries-for-dir ;:- [VirtualPath]
  [resource :- Resource]
  (if (= :java-classpath-jar (:source-type resource))
    (filter
     (fn [je] (and (st/starts-with? je (:virtual-path resource))
                   (not (= je (str (:virtual-path resource) "/")))))
     (map #(.getName ^JarEntry %)
          (enumeration-seq
           (.entries
            (JarFile.
             (.toFile
              (Paths/get
               (URI.
                (.getSchemeSpecificPart
                 (filesystem-uri (:java-uri resource)))))))))))
    ; Bsp-Code: https://github.com/clojure/java.classpath/blob/c10fc96a8ff98db4eb925a13ef0f5135ad8dacc6/src/main/clojure/clojure/java/classpath.clj#L50
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
            (is-file? resource-to-work-with)
            (recur (drop 1 paths) result)
            :else
            (recur (into (drop 1 paths)
                         (map #(str path-to-work-with "/" %)
                              (list-entries-for-dir resource-to-work-with))) 
                   result))))
      result)))

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
                  (str (user-dir) "/")
                  virtual-path 
                  [""] 
                  :from-cp false))]
    (doseq [resource-path resource-paths]
      (Files/delete (absolut-path virtual-path resource-path))
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
        (let [target-file (absolut-path target-path resource-path)
              source-file (path-from-cp-or-fs
                           fs-prefix
                           base-path
                           resource-path)]
          (when (Files/isDirectory source-file no-link-option)
            (Files/createDirectories target-file (into-array FileAttribute [])))
          (when (Files/isRegularFile source-file no-link-option)
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
