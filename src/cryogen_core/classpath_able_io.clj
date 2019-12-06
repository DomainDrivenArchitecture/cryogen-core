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
  [source-path target-path ignored-files]
  )

(defn copy-resources
  [source-path target-path]
  (let [ignored-files []
        source-file (io/file source-path)]
      (cond
        (not (.exists (io/file source-path)))
        (throw (IllegalArgumentException. (str "resource " source-path " not found")))
        (.isDirectory (io/file source-path))
        (copy-dir source-path target-path ignored-files)
        )))

(defn copy-resources-from-theme
  [theme target]
  (let [source-path (str "templates/themes/" theme "/js")
        target-path (str target "/js")]
    copy-resources source-path target-path))

