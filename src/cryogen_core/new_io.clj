;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.new-io
  (:require [clojure.string :as st]
            [clojure.java.io :as io]
            [cryogen-core.classpath-able-io :as cp-io]
            [me.raynes.fs :as fs]))

(def delete-resources! cp-io/delete-resources!)

(def public "resources/public")

(defn copy-resources-from-theme!
  [fs-prefix theme target-path ignore-patterns]
  (let [theme-path (str "templates/themes/" theme)]
    (cp-io/copy-resources! fs-prefix theme-path ["css" "js" "img"]
                           target-path ignore-patterns)
    (cp-io/copy-resources! fs-prefix (str theme-path "/html") ["404.html"]
                           target-path ignore-patterns)))

(defn copy-html-from-theme!
  [fs-prefix theme target-path ignore-patterns]
  (let [theme-path (str "templates/themes/" theme)]
    (cp-io/copy-resources! fs-prefix theme-path ["html"]
                           target-path ignore-patterns)))

(defn copy-resources-from-templates!
  [fs-prefix resources target-path ignore-patterns]
  (let [resource-path "templates"]
    (cp-io/copy-resources! fs-prefix resource-path resources
                           target-path ignore-patterns)))

(defn get-distinct-markup-dirs
  [fs-prefix posts pages ignore-patterns]
  (let [base-path "templates/md"
        resources (cp-io/get-resources
                   fs-prefix base-path [pages posts])
        filtered-resources (->> (filter #(= (:resource-type %) :dir) resources)
                                (cp-io/distinct-resources-by-path))]
    filtered-resources))

(defn create-dirs-from-markup-folders!
  "Copy resources from markup folders. This does not copy the markup entries."
  [fs-prefix posts pages target-path ignore-patterns]
  (let [resources (get-distinct-markup-dirs fs-prefix posts pages
                                            ignore-patterns)]
    (doseq [resource resources]
      (io/make-parents (io/file (str target-path "/" (:virtual-path resource))))
      (.mkdir (io/file (str target-path "/" (:virtual-path resource)))))))

(defn path
  "Creates path from given parts, ignore empty elements"
  [& path-parts]
  (->> path-parts
       (remove st/blank?)
       (st/join "/")
       (#(st/replace % #"/+" "/"))))

(defn get-file-extension-from-resource
  [resource]
  (str "." (last (st/split (:virtual-path resource) #"\."))))

(defn find-assets
  "Find all assets in the given root directory (f) and the given file
extension (ext) ignoring any files that match the given (ignored-files).
First make sure that the root directory exists, if yes: process as normal;
if no, return empty vector."
  [base-path paths fs-prefix ext ignored-files]
  (let [assets (cp-io/get-resources fs-prefix base-path paths)
        filter-file (fn [xs] (filter #(= (:resource-type %) :file) xs))
        filter-ext (fn [xs] (filter #(= (get-file-extension-from-resource %) ext) xs))
        cast-file (fn [java-path] (io/as-file (.toString java-path)))
        get-java-path (fn [map-entry] (cast-file (:java-path map-entry)))]
    (->> assets
         filter-file
         filter-ext
         (map get-java-path))))

;
;
; taken from io.clj

(defn re-filter [bool-fn re & other-res]
  (let [res (conj other-res re)]
    (reify java.io.FilenameFilter
      (accept [this _ name]
        (bool-fn (some #(re-find % name) res))))))

(def match-re-filter (partial re-filter some?))
(def reject-re-filter (partial re-filter nil?))

(defn copy-dir [src target ignored-files]
  (fs/mkdirs target)
  (let [^java.io.FilenameFilter filename-filter (apply reject-re-filter ignored-files)
        files (.listFiles (io/file src) filename-filter)]
    (doseq [^java.io.File f files]
      (let [out (io/file target (.getName f))]
        (if (.isDirectory f)
          (copy-dir f out ignored-files)
          (io/copy f out))))))

(defn create-folder [folder]
  (let [loc (io/file (path public folder))]
    (when-not (.exists loc)
      (.mkdirs loc))))

(defn create-file [file data]
  (spit (path public file) data))

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
          target (path public blog-prefix (fs/base-name resource))]
      (cond
        (not (.exists (io/file src)))
        (throw (IllegalArgumentException. (str "resource " src " not found")))
        (.isDirectory (io/file src))
        (copy-dir src target ignored-files)
        :else
        (fs/copy src target)))))

(defn ignore [ignored-files]
  (fn [^java.io.File file]
    (let [name    (.getName file)
          matches (map #(re-find % name) ignored-files)]
      (not (some seq matches)))))

(defn get-resource [resource]
  (-> resource io/resource io/file))