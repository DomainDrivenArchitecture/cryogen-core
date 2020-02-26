;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io
  (:require [clojure.string :as st]
            [cryogen-core.classpath-able-io.fs :as fs]
            [cryogen-core.classpath-able-io.cp :as cp]
            [cryogen-core.classpath-able-io.this :as this]
            [schema.core :as s])
  (:import  [java.nio.file Files]))

(def SourceType this/SourceType)
(def ResourceType this/ResourceType)
(def Prefix this/Prefix)
(def JavaUri this/JavaUri) ; java.net.URI
(def VirtualPath this/VirtualPath)
(def JavaPath this/JavaPath) ; java.nio.Path
(def Resource this/Resource)

(defn get-file-extension-from-resource
  [resource]
  (str "." (last (st/split (:virtual-path resource) #"\."))))

(defn get-filename-from-resource
  [resource]
   (last (st/split (:virtual-path resource) #"\/")))

(defn filter-for-ignore-patterns
  [ignore-patterns source-list]
  (filter #(not (re-matches ignore-patterns %)) source-list))

(defn filter-resources-for-ignore-patterns
  [ignore-patterns resources]
  (filter #(not (re-matches (re-pattern ignore-patterns) (:virtual-path %)))
          resources))

(defn resource-from-cp-or-fs ;:- Resource 
  [fs-prefix ;:- Prefix
   base-path ;:- VirtualPath
   resource-path ;:- VirtualPath
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [path-from-fs (when from-fs
                       (fs/path-if-exists fs-prefix base-path resource-path))
        path-from-cp (when from-cp
                       (cp/path-if-exists))]
    (cond
      (some? path-from-fs)
      (fs/create-resource resource-path path-from-fs :filesystem)
      (some? path-from-cp)
      (cp/create-resource resource-path path-from-cp)
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

; TODO: rename? Allow base-path to be ""?
; base-path must not be ""
(defn get-resources ;:- [Resource]
  [fs-prefix ;:- Prefix
   base-path ;:- VirtualPath
   paths ;:- [VirtualPath]
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [virtual-path-map (fn [resource] {(:virtual-path resource) resource})
        fs-resource-map (if from-fs
                          (apply merge (map virtual-path-map (fs/get-resources fs-prefix base-path paths)))
                          {})
        cp-resource-map (if from-cp
                          (apply merge (map virtual-path-map (cp/get-resources base-path paths)))
                          {})
        resulting-map (merge cp-resource-map fs-resource-map)]
    (if (empty? resulting-map)
      []
      (vals resulting-map))))

(defn get-resource-paths-recursive ;:- [VirtualPath]
  [fs-prefix ;:- Prefix
   base-path ;:- VirtualPath
   paths ;:- [VirtualPath]
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (map #(:virtual-path %)
       (get-resources
        fs-prefix base-path paths
        :from-cp from-cp
        :from-fs from-fs)))

; TODO: Add files to keep
(s/defn delete-resources!
  [virtual-path :- s/Str]
  (let [resource-paths
        (reverse
         (sort
          (get-resource-paths-recursive
           (str (fs/user-dir) "/")
           virtual-path
           [""]
           :from-cp false)))]
    (do
      (doseq [resource-path resource-paths]
        (Files/delete (fs/absolut-path virtual-path resource-path))))))

(defn copy-resources!
  [fs-prefix ;:- Prefix
   base-path ;:- VirtualPath
   source-paths ;:- [VirtualPath]
   target-path  ;:- VirtualPath
   ignore-patterns ;:- s/Str
   ]
  (let [resources (sort this/compare-resource (get-resources fs-prefix base-path source-paths))
        resources (filter-resources-for-ignore-patterns ignore-patterns resources)]
    (if (empty? resources)
      (throw (IllegalArgumentException. (str "resource " base-path ", "
                                             source-paths " not found")))
      (doseq [resource resources]
        (let [target-path (fs/absolut-path target-path (:virtual-path resource))
              source-path (:java-path resource)]
          (when (this/is-dir? resource)
            (Files/createDirectories target-path fs/no-attributes))
          (when (this/is-file? resource)
            (Files/copy source-path target-path fs/overwrite-preserve-attributes)))))))

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
