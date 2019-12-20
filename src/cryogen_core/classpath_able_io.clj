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
            [schema.core :as s]))

(def SourceType (s/enum :classpath :filesystem))

(def ResourceType (s/enum :file :dir :unknown))

(def Prefix s/Str)

(def Uri s/Any) ; java.net.URI

(def Path s/Str)

(def File s/Any) ; java.io.File

(def Resource
  {:path          Path
   :uri           Uri
   :file          File
   :source-type   SourceType
   :resource-type ResourceType})

(def public "resources/public")

(defn path
  "Creates path from given parts, ignore empty elements"
  [& path-parts]
  (->> path-parts
       (remove st/blank?)
       (st/join "/")
       (#(st/replace % #"/+" "/"))))

(defn filter-for-ignore-patterns
  [ignore-patterns source-list]
  (filter #(not (re-matches ignore-patterns %)) source-list))

(s/defn create-resource :- Resource
  ([path :- Path
    uri :- Uri
    file :- File
    source-type :- SourceType
    resource-type :- ResourceType]
   {:path          path
    :uri           uri
    :file          file
    :source-type   source-type
    :resource-type resource-type})
  ([path :- Path
    file :- File
    source-type :- SourceType]
   {:path          path
    :uri           (.toURI file)
    :file          file
    :source-type   source-type
    :resource-type (cond
                     (.isDirectory file) :dir
                     (.isFile file) :file
                     :else :unknown)}))

(s/defn is-file? :- s/Bool
  [resource :- Resource]
  (= :file (:resource-type resource)))

(s/defn file-from-cp ;  :- File
  [resource-path :- Path]
  (let [file-from-cp (io/file (io/resource resource-path))]
    (try
      (when (.exists file-from-cp)
        file-from-cp)
      (catch Exception e
        nil))))

(s/defn file-from-fs ;  :- File
  [fs-prefix :- Prefix
   resource-path :- Path]
  (let [file-from-fs (io/file (str fs-prefix resource-path))]
    (try
      (when (.exists file-from-fs)
        file-from-fs)
      (catch Exception e
        nil))))

(defn resource-from-cp-or-fs ; :- Resource 
  [fs-prefix ; :- Prefix
   base-path ; :- Path
   resource-path ; :- Path
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [full-path    (if (empty? base-path)
                       resource-path
                       (str base-path "/" resource-path))
        file-from-fs (if from-fs
                       (file-from-fs fs-prefix full-path)
                       nil)
        file-from-cp (if from-cp
                       (file-from-cp full-path)
                       nil)]
    (cond
      (some? file-from-fs)
      (create-resource resource-path file-from-fs :filesystem)
      (some? file-from-cp)
      (create-resource resource-path file-from-cp :classpath)
      :else nil)))

(defn file-from-cp-or-fs ; :- File
  [fs-prefix ; :- Prefix
   base-path ; :- Path
   resource-path; :- Path
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [resource (resource-from-cp-or-fs
                  fs-prefix base-path resource-path
                  :from-cp from-cp
                  :from-fs from-fs)]
    (when (some? resource)
      (:file resource))))

(defn get-resources-recursive ;:- [Resource]
  [fs-prefix ;:- Prefix
   base-path ;:- Path
   paths ;:- [Path]
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
          (cond
            (nil? resource-to-work-with) []
            (is-file? resource-to-work-with)
            (recur (drop 1 paths) result)
            :else
            (recur (into (drop 1 paths)
                         (map #(str path-to-work-with "/" %)
                              (.list (:file resource-to-work-with))))
                   result))))
      result)))

(defn get-resource-paths-recursive ;:- [Path]
  [fs-prefix ;:- Prefix
   base-path ;:- Path
   paths ;:- [Path]
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (map #(:path %)
       (get-resources-recursive
        fs-prefix base-path paths
        :from-cp from-cp
        :from-fs from-fs)))

; TODO: Add files to keep
(s/defn delete-resource-recursive!
  [path :- s/Str]
  (let [resource-paths
        (reverse (get-resource-paths-recursive
                  "" path [""] :from-cp false))]
    (doseq [resource-path resource-paths]
      (io/delete-file (str path resource-path)))))

; TODO: add ignore patterns filtering
(defn copy-resources!
  [fs-prefix ;:- Prefix
   base-path ;:- Path
   source-paths ;:- [Path]
   target-path  ;:- Path
   ignore-patterns ;:- s/Str
   ]
  (let [resource-paths
        (get-resource-paths-recursive fs-prefix base-path source-paths)]
    (if (empty? resource-paths)
      (throw (IllegalArgumentException. (str "resource " resource-paths ", "
                                             source-paths " not found")))
      (doseq [resource-path resource-paths]
        (let [target-file (io/file target-path resource-path)
              source-file (io/file (file-from-cp-or-fs
                                    fs-prefix
                                    base-path
                                    resource-path))]
          (io/make-parents target-file)
          (when (.isFile source-file)
            (io/copy source-file target-file)))))))

(defn copy-resources-from-user!
  [fs-prefix resources target-path ignore-patterns]
  (let [resource-path "templates"]
    (copy-resources! fs-prefix resource-path resources
                     target-path ignore-patterns)))

(defn copy-resources-from-theme!
  [fs-prefix theme target-path ignore-patterns]
  (let [theme-path (str "templates/themes/" theme)]
    (copy-resources! fs-prefix theme-path ["css" "js"]
                     target-path ignore-patterns)
    (copy-resources! fs-prefix (str theme-path "/html") ["404.html"]
                     target-path ignore-patterns)))

(defn distinct-resources-by-path
  [resources]
  (loop [paths (set (map :path resources))
         resources resources
         acc []]
    (cond (empty? resources) acc
          (contains? paths (:path (first resources))) (recur (disj paths (:path (first resources)))
                                                             (rest resources)
                                                             (conj acc (first resources)))
          :else (recur paths (rest resources) acc))))

(defn get-distinct-markup-dirs
  [fs-prefix posts pages ignore-patterns]
  (let [base-path "templates/md"
        resources (get-resources-recursive
                   fs-prefix base-path [pages posts])
        filtered-resources (->> (filter #(= (:resource-type %) :dir) resources)
                                (distinct-resources-by-path))]
    filtered-resources))

(defn create-dirs-from-markup-folders!
  "Copy resources from markup folders. This does not copy the markup entries."
  [fs-prefix posts pages target-path ignore-patterns]
  (let [resources (get-distinct-markup-dirs fs-prefix posts pages
                                            ignore-patterns)]
    (doseq [resource resources]
      (io/make-parents (io/file (str target-path "/" (:path resource))))
      (.mkdir (io/file (str target-path "/" (:path resource)))))))
