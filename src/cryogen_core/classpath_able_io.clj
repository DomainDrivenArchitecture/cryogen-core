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
   (:import [java.nio.file FileSystems Paths Files LinkOption]))

(def SourceType (s/enum :classpath :filesystem))

(def ResourceType (s/enum :file :dir :unknown))

(def Prefix s/Str)

(def Uri s/Any) ; java.net.URI

(def ShortPath s/Str)

(def JavaPath s/Any) ; java.nio.Path

(def Resource
  {:short-path    ShortPath
   :uri           Uri
   :java-path     JavaPath
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
  ([short-path :- ShortPath
    uri :- Uri
    java-path :- JavaPath
    source-type :- SourceType
    resource-type :- ResourceType]
   {:short-path    short-path
    :uri           uri
    :java-path     java-path
    :source-type   source-type
    :resource-type resource-type})
  ([short-path :- ShortPath
    java-path :- JavaPath
    source-type :- SourceType]
   {:short-path    short-path
    :uri           (.toURI java-path)
    :java-path     java-path
    :source-type   source-type
    :resource-type (cond
                     (Files/isDirectory java-path (into-array [LinkOption/NOFOLLOW_LINKS])) :dir
                     (Files/isRegularFile java-path (into-array [LinkOption/NOFOLLOW_LINKS])) :java-path
                     :else :unknown)}))

(s/defn is-file? :- s/Bool
  [resource :- Resource]
  (= :java-path (:resource-type resource)))

(s/defn path-from-cp ;  :- JavaPath
  [resource-path :- ShortPath]
  (try
    (let [path-from-cp (Paths/get (java.net.URI. (.toString (io/resource resource-path))))]
      (when (.exists path-from-cp)
        path-from-cp))
    (catch Exception e
      nil)))

(s/defn path-from-fs ;  :- JavaPath
  [fs-prefix :- Prefix
   resource-path :- ShortPath]
  (let [path-from-fs (Paths/get (java.net.URI. (str fs-prefix resource-path)))] ;with this, you need to give the absolute path
    (try
      (when (.exists path-from-fs)
        path-from-fs)
      (catch Exception e
        nil))))

(defn resource-from-cp-or-fs ; :- Resource 
  [fs-prefix ; :- Prefix
   base-path ; :- ShortPath
   resource-path ; :- ShortPath
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [full-path    (if (empty? base-path)
                       resource-path
                       (str base-path "/" resource-path))
        path-from-fs (if from-fs
                       (path-from-fs fs-prefix full-path)
                       nil)
        path-from-cp (if from-cp
                       (path-from-cp full-path)
                       nil)]
    (cond
      (some? path-from-fs)
      (create-resource resource-path path-from-fs :filesystem)
      (some? path-from-cp)
      (create-resource resource-path path-from-cp :classpath)
      :else nil)))

(defn path-from-cp-or-fs ; :- JavaPath
  [fs-prefix ; :- Prefix
   base-path ; :- ShortPath
   resource-path; :- ShortPath
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [resource (resource-from-cp-or-fs
                  fs-prefix base-path resource-path
                  :from-cp from-cp
                  :from-fs from-fs)]
    (when (some? resource)
      (:java-path resource))))

(defn get-resources-recursive ;:- [Resource]
  [fs-prefix ;:- Prefix
   base-path ;:- ShortPath
   paths ;:- [ShortPath]
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
                              (.list (:java-path resource-to-work-with))))
                   result))))
      result)))

(defn get-resource-paths-recursive ;:- [ShortPath]
  [fs-prefix ;:- Prefix
   base-path ;:- ShortPath
   paths ;:- [ShortPath]
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (map #(:short-path %)
       (get-resources-recursive
        fs-prefix base-path paths
        :from-cp from-cp
        :from-fs from-fs)))

; TODO: Add files to keep
(s/defn delete-resource-recursive!
  [short-path :- s/Str]
  (let [resource-paths
        (reverse (get-resource-paths-recursive
                  "" short-path [""] :from-cp false))]
    (doseq [resource-path resource-paths]
      (io/delete-file (str short-path resource-path)))))

; TODO: add ignore patterns filtering
(defn copy-resources!
  [fs-prefix ;:- Prefix
   base-path ;:- ShortPath
   source-paths ;:- [ShortPath]
   target-path  ;:- ShortPath
   ignore-patterns ;:- s/Str
   ]
  (let [resource-paths
        (get-resource-paths-recursive fs-prefix base-path source-paths)]
    (if (empty? resource-paths)
      (throw (IllegalArgumentException. (str "resource " resource-paths ", "
                                             source-paths " not found")))
      (doseq [resource-path resource-paths]
        (let [target-file (io/file target-path resource-path)
              source-file (io/file (path-from-cp-or-fs
                                    fs-prefix
                                    base-path
                                    resource-path))]
          (io/make-parents target-file)
          (when (.isFile source-file)
            (io/copy source-file target-file)))))))

(defn distinct-resources-by-path
  [resources]
  (loop [paths (set (map :short-path resources))
         resources resources
         acc []]
    (cond (empty? resources) acc
          (contains? paths (:short-path (first resources))) (recur (disj paths (:short-path (first resources)))
                                                             (rest resources)
                                                             (conj acc (first resources)))
          :else (recur paths (rest resources) acc))))
