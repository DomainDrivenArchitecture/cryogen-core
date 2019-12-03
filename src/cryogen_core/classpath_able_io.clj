(ns cryogen-core.classpath-able-io
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [me.raynes.fs :as fs]))

(defn copy-resources
  [source-path target-path]
  (cond
        (not (.exists (io/file source-path)))
        (throw (IllegalArgumentException. (str "resource " source-path " not found")))
        (.isDirectory (io/file source-path))
        (copy-dir source-path target-path ignored-files)
        :else
        (fs/copy source-path target-path)))

(defn copy-resources-from-theme
  [theme target]
  (let [source-path (str "templates/themes/" theme "/js")
        target-path (str target "/js")]
    copy-resources source-path target-path))
