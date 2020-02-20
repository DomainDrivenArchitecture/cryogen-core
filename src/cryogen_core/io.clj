(ns cryogen-core.io
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [cryogen-core.new-io :as new-io]
            [me.raynes.fs :as fs]))

(def public "resources/public")

(defn re-filter [bool-fn re & other-res]
  (let [res (conj other-res re)]
    (reify java.io.FilenameFilter
      (accept [this _ name]
        (bool-fn (some #(re-find % name) res))))))

(def match-re-filter (partial re-filter some?))
(def reject-re-filter (partial re-filter nil?))

(defn get-resource [resource]
  (-> resource io/resource io/file))

(defn ignore [ignored-files]
  (fn [^java.io.File file]
    (let [name    (.getName file)
          matches (map #(re-find % name) ignored-files)]
      (not (some seq matches)))))

(defn create-folder [folder]
  (let [loc (io/file (new-io/path public folder))]
    (when-not (.exists loc)
      (.mkdirs loc))))

(defn create-file [file data]
  (spit (new-io/path public file) data))

(defn create-file-recursive [file data]
  (create-folder (.getParent (io/file file)))
  (create-file file data))

(defn copy-dir [src target ignored-files]
  (fs/mkdirs target)
  (let [^java.io.FilenameFilter filename-filter (apply reject-re-filter ignored-files)
        files (.listFiles (io/file src) filename-filter)]
    (doseq [^java.io.File f files]
      (let [out (io/file target (.getName f))]
        (if (.isDirectory f)
          (copy-dir f out ignored-files)
          (io/copy f out))))))

(defn copy-resources [{:keys [blog-prefix resources ignored-files]}]
  (doseq [resource resources]
    (let [src (str "resources/templates/" resource)
          target (new-io/path public blog-prefix (fs/base-name resource))]
      (cond
        (not (.exists (io/file src)))
        (throw (IllegalArgumentException. (str "resource " src " not found")))
        (.isDirectory (io/file src))
        (copy-dir src target ignored-files)
        :else
        (fs/copy src target)))))
