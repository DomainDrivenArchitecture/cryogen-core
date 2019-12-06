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
  [source-dir target-path ignore-patterns]
  (let [source-list (.list source-dir)]
    (doseq [f source-list]
      (io/copy f (io/file target-path))))
  )

(defn copy-resources
  [fs-prefix source-path target-path ignore-patterns]
  (let [file (file-from-cp-or-filesystem fs-prefix source-path)
        is-dir? (.isDirectory file)]
    (cond
      (nil? file)
      (throw (IllegalArgumentException. (str "resource " source-path " not found")))
      is-dir?
      (copy-dir file target-path ignore-patterns)
      :else
      nil
      ;(fs/copy src target)
      )))

(defn copy-resources-from-theme
  [theme target]
  (let [source-path (str "templates/themes/" theme "/js")
        target-path (str target "/js")]
    copy-resources source-path target-path))

