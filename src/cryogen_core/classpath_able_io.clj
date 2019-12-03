(ns cryogen-core.classpath-able-io
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(defn file-from-cp-or-filesystem
  [fs-prefix resource-path]
  (let [file-from-cp (io/file (io/resource "templates/themes/bootstrap4-test/js"))
        file-from-fs (io/file "./test-resources/templates/themes/bootstrap4-test/js")]
    file-from-cp))

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

