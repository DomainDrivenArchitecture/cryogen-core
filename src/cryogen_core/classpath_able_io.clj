;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(defn filter-for-ignore-patterns
  [ignore-patterns source-list]
  (filter #(not (re-matches ignore-patterns %)) source-list))

;; TODO: make fct wipe-folders

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

(defn copy-file
  [source-file ;first element of (.list source-dir)
   target-file]
  (do (io/make-parents target-file)
      (io/copy source-file target-file)))

(defn- build-f-for-subdirs
  "builds the list for subdirs and returns it"
  [f]; TODO: f is not what i would be expecting here
  (let [xs (.list f)]
    (println (str "** f:    " f "    xs:  "  (apply str xs)))
    (map #(str f "/" %) xs)))

(defn copy-dir
  [source-dir target-dir ignore-patterns]
  (loop [source-list (.list source-dir)
         file-path-prefix [""]]
    (let [f (first source-list)
          second? (not (nil? (second source-list)))
          target-file (io/file target-dir (str (first file-path-prefix) f))
          source-file (io/file source-dir (str (first file-path-prefix) f))]
      (if (.isFile source-file)
        (do
          (copy-file source-file target-file)
          (when second?
            (recur (drop 1 source-list) (drop 1 file-path-prefix))))
        (when (> (count (.list source-file)) 0)
          (recur (concat (.list source-file) (drop 1 source-list))
                 (concat (repeat (count (.list source-file)) (str (first file-path-prefix) f "/"))
                         (drop 1 file-path-prefix))))))))

(defn copy-resources
  [fs-prefix source-path target-path ignore-patterns]
  (let [source-file (file-from-cp-or-filesystem fs-prefix source-path)
        target-file (io/file target-path source-path)
        is-source-dir? (.isDirectory source-file)]
    (cond
      (nil? source-file)
      (throw (IllegalArgumentException. (str "resource " source-path " not found")))
      is-source-dir?
      (copy-dir source-file target-file ignore-patterns)
      :else (copy-file source-file target-file)
      ;; TODO: Call copy-file fct. - take care on parameter.
      )))


(defn copy-resources-from-theme
  [fs-prefix theme target-path]
  (let [theme-path (str "templates/themes/" theme)]
    (copy-resources fs-prefix (str theme-path "/css") target-path "")
    (copy-resources fs-prefix (str theme-path "/js") target-path "")
    (copy-resources fs-prefix (str theme-path "/html/404.html") target-path "")))

