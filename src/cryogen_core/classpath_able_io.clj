(ns cryogen-core.classpath-able-io
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [me.raynes.fs :as fs]))

(defn copy-dir 
  [source-path target-path ignored-files]
  (fs/mkdirs target-path)
  (let [^java.io.FilenameFilter filename-filter (apply reject-re-filter ignored-files)
        files (.listFiles (io/file source-path) filename-filter)]
    (doseq [^java.io.File f files]
      (let [out (io/file target-path (.getName f))]
        (if (.isDirectory f)
          (copy-dir f out ignored-files)
          (io/copy f out))))))

(defn copy-resources
  [source-path target-path]
  (let [ignored-files []]
      (cond
        (not (.exists (io/file source-path)))
        (throw (IllegalArgumentException. (str "resource " source-path " not found")))
        (.isDirectory (io/file source-path))
        (copy-dir source-path target-path ignored-files)
        :else
        (fs/copy source-path target-path))))

(defn copy-resources-from-theme
  [theme target]
  (let [source-path (str "templates/themes/" theme "/js")
        target-path (str target "/js")]
    copy-resources source-path target-path))

