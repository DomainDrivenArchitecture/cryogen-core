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

(defn file-from-cp
  [resource-path]
  (let [file-from-cp (io/file (io/resource resource-path))]
    (try
      (when (.exists file-from-cp)
        file-from-cp)
      (catch Exception e
        nil))))

(defn file-from-fs
  [fs-prefix resource-path]
  (let [file-from-fs (io/file (str fs-prefix resource-path))]
    (try
      (when (.exists file-from-fs)
        file-from-fs)
      (catch Exception e
        nil))))

(defn file-from-cp-or-filesystem
  [fs-prefix resource-path]
  (let [from-fs (file-from-fs fs-prefix resource-path)]
    (if (some? from-fs)
      from-fs
      (file-from-cp resource-path))))

(s/defn get-resource-paths-recursive :- [s/Str]
  [fs-prefix :- s/Str
   base-path :- s/Str
   paths :- [s/Str]]
  (loop [paths  paths
         result []]
    (if (not (empty? paths))
      (do
        (let [path-to-work-with (first paths)
              file-to-work-with (io/file (file-from-cp-or-filesystem
                                          fs-prefix
                                          (str base-path "/" path-to-work-with)))
              result            (into result [path-to-work-with])]
          (cond 
            (nil? file-to-work-with) []
            (.isFile file-to-work-with) (recur (drop 1 paths) result)
            :else
            (recur (into (drop 1 paths)
                         (map #(str path-to-work-with "/" %) 
                              (.list file-to-work-with)))
                   result)
            )))
      result)))

(s/defn delete-resource-recursive!
  [path :- s/Str]
  (let [resource-paths
        (reverse (get-resource-paths-recursive "" path [""]))]
    (println resource-paths)
    (doseq [path resource-paths]
      (io/delete-file path))))

(defn copy-file!
  [source-file
   target-file]
  (do (io/make-parents target-file)
      (io/copy source-file target-file)))

(defn do-copy!
  [source-dir target-dir ignore-patterns]
  (loop [source-list      (.list source-dir)
         file-path-prefix [""]]
    (let [f           (first source-list)
          second?     (not (nil? (second source-list)))
          target-file (io/file target-dir (str (first file-path-prefix) f))
          source-file (io/file source-dir (str (first file-path-prefix) f))]
      (if (.isFile source-file)
        (do
          (copy-file! source-file target-file)
          (when second?
            (recur (drop 1 source-list) (drop 1 file-path-prefix))))
        (when (> (count (.list source-file)) 0)
          (recur (concat (.list source-file) (drop 1 source-list))
                 (concat (repeat (count (.list source-file)) (str (first file-path-prefix) f "/"))
                         (drop 1 file-path-prefix))))))))

(defn copy-resources!
  [fs-prefix source-path target-path ignore-patterns]
  (let [source-file    (file-from-cp-or-filesystem fs-prefix source-path)
        target-file    (io/file target-path source-path)
        is-source-dir? (.isDirectory source-file)]
    (if (nil? source-file)
      (throw (IllegalArgumentException. (str "resource " source-path " not found")))
      (do-copy! source-file target-file ignore-patterns))))

(defn copy-resources-from-theme!  
  [fs-prefix theme target-path ignore-patterns]
  (let [theme-path (str "templates/themes/" theme)]
    (copy-resources! fs-prefix (str theme-path "/css") target-path ignore-patterns)
    (copy-resources! fs-prefix (str theme-path "/js") target-path ignore-patterns)
    (copy-resources! fs-prefix (str theme-path "/html/") target-path ignore-patterns)))
