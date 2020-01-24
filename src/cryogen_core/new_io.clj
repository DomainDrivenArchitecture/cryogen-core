;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.new-io
  (:require 
   [clojure.java.io :as io]
   [cryogen-core.classpath-able-io :as cp-io]))

(def delete-resource-recursive! cp-io/delete-resource-recursive!)

(def public "resources/public")

(defn copy-resources-from-templates!
  [fs-prefix resources target-path ignore-patterns]
  (let [resource-path "templates"]
    (cp-io/copy-resources! fs-prefix resource-path resources
                     target-path ignore-patterns)))

(defn copy-resources-from-theme!
  [fs-prefix theme target-path ignore-patterns]
  (let [theme-path (str "templates/themes/" theme)]
    (cp-io/copy-resources! fs-prefix theme-path ["css" "js"]
                     target-path ignore-patterns)
    (cp-io/copy-resources! fs-prefix (str theme-path "/html") ["404.html"]
                     target-path ignore-patterns)))

(defn copy-html-from-theme!
  [fs-prefix theme target-path ignore-patterns]
  (let [theme-path (str "templates/themes/" theme)]
    (cp-io/copy-resources! fs-prefix theme-path ["html"]
                           target-path ignore-patterns)))

(defn get-distinct-markup-dirs
  [fs-prefix posts pages ignore-patterns]
  (let [base-path "templates/md"
        resources (cp-io/get-resources-recursive
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
