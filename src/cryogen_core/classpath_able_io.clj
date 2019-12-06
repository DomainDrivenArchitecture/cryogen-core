(ns cryogen-core.classpath-able-io
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

; TODO: loading from cpasspath results in nil even if file exists
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


(defn copy-dir 
  [source-dir target-file ignore-patterns]
  (let [source-list (.list source-dir)]
    (io/make-parents target-file)
    (doseq [f source-list]
      (io/copy f target-file))))

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
  [fs-prefix theme target]
  (let [source-path (str "templates/themes/" theme "/js")
        target-path (str target "/js")]
    (copy-resources fs-prefix source-path target-path "")))

