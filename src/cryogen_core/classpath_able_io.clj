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

(defn wipe-folder
  [folder]
  (io/delete-file "target/tmp"))

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

(defn copy-dir
  [source-dir target-dir ignore-patterns]
  (loop [l-source-list (.list source-dir)
         l-source-dir  source-dir
         l-target-dir  target-dir]
    (println (str "source-dir:  " source-dir "   target-dir: " target-dir "    f: ") (first l-source-list) "   second: " (second l-source-list))
    (let [f           (first l-source-list)
          second? (not (nil? (second l-source-list)))
          target-file (io/file target-dir f)
          source-file (io/file source-dir f)]
      ; TODO: .isFile is called on wrong path to the actual file, does not consider the path leading to the subdirectory of the file 
      (println "type of f: " (type f))
      (if (.isFile source-file)
        (do
          (copy-file f target-file)
          ;; continue copying files
          (when second?
            (recur (drop 1 l-source-list) source-dir target-dir)))
        ;; recur down to contained directory
        (do (println (type (.list source-file)))
            (when second? (recur (concat (.list source-file) (drop 1 l-source-list)) source-file target-file)))))))

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

