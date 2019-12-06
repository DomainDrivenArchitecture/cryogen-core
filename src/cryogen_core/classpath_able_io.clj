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

; TODO: fix recursion as we put function calls on callstack here
(defn copy-dir-2
  [source-dir target-dir ignore-patterns]
  (loop [source-list (.list source-dir)]
    (when (not (nil? (first source-list)))
      (let [f (first source-list)
            target-file (io/file target-dir f)
            source-file (io/file source-dir f)]
        (println (str "frsit f: " f))
        (if (.isFile source-file)
          (do
            (println (str "source file:    " source-file))
            (io/make-parents target-file)
            (io/copy f target-file)
            (recur (drop 1 source-list)))
          (do 
            (println source-file)
            (recur (concat (drop 1 source-list) (.list source-file)))))
        ))))

(defn copy-dir
  [source-dir target-dir ignore-patterns]
  (let [source-list (.list source-dir)]
    (doseq [f source-list]
      (let [target-file (io/file target-dir f)
            source-file (io/file source-dir f)]
        (if (.isFile source-file)
          (do
            (println source-file)
            (io/make-parents target-file)
            (io/copy f target-file))
          (copy-dir source-file target-file ignore-patterns))))))

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
      :else
      nil 
      ;(fs/copy src target)
      )))

(defn copy-resources-from-theme
  [fs-prefix theme target-path]
  (let [source-path (str "templates/themes/" theme "/js")]
    (copy-resources fs-prefix source-path target-path "")))

