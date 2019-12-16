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
  [fs-prefix resource-path
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (let [from-fs-file (if from-fs
                       (file-from-fs fs-prefix resource-path)
                       nil)
        from-cp-file (if from-cp 
                       (file-from-cp resource-path)
                       nil)]
    (if (some? from-fs-file)
      from-fs-file
      from-cp-file)))

(defn get-resource-paths-recursive ;:- [s/Str]
  [fs-prefix ;:- s/Str
   base-path ;:- s/Str
   paths ;:- [s/Str]
   & {:keys [from-cp from-fs]
      :or   {from-cp true
             from-fs true}}]
  (loop [paths  paths
         result []]
    (if (not (empty? paths))
      (do
        (let [path-to-work-with (first paths)
              file-to-work-with (io/file (file-from-cp-or-filesystem
                                          fs-prefix
                                          (str base-path "/" path-to-work-with)
                                          :from-cp from-cp 
                                          :from-fs from-fs))
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
        (reverse (get-resource-paths-recursive 
                  "" path [""] :from-cp false))]
    (doseq [path resource-paths]
      (io/delete-file path))))

(defn copy-resources!
  [fs-prefix ;:- s/Str 
   base-path ;:- s/Str
   source-paths ;:- [s/Str]
   target-path  ;:- s/Str
   ignore-patterns ;:- s/Str
   ]
  (let [resource-paths
        (get-resource-paths-recursive fs-prefix base-path source-paths)]
     (if (empty? resource-paths)
      (throw (IllegalArgumentException. (str "resource " resource-paths ", " 
                                             source-paths " not found")))
    (doseq [resource-path resource-paths]
      (let [target-file (io/file target-path resource-path)
            source-file (io/file (file-from-cp-or-filesystem 
                                  fs-prefix 
                                  (str base-path "/" resource-path)))]
        (io/make-parents target-file)
        (when (.isFile source-file)
          (io/copy source-file target-file)))))))

(defn copy-resources-from-theme!  
  [fs-prefix theme target-path ignore-patterns]
  (let [theme-path (str "templates/themes/" theme)]
    (copy-resources! fs-prefix theme-path ["css" "js"] 
                     target-path ignore-patterns)
    (copy-resources! fs-prefix (str theme-path "/html") ["404.html"]
                     target-path ignore-patterns)))
