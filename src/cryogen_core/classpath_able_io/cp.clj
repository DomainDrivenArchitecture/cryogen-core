;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.cp
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [schema.core :as s]
            [cryogen-core.classpath-able-io.this :as this]
            [cryogen-core.classpath-able-io.fs :as fs]
            [cryogen-core.classpath-able-io.jar :as jar])
  (:import [java.nio.file Paths Files]))

(s/defn path-if-exists :- this/JavaPath
  [& path-elements ;:- this/VirtualPath
   ]
  (try
    (let [resource-uri
          (.toURI (io/resource
                   (apply this/virtual-path-from-elements path-elements)))]
      (if (jar/is-from-classpath-jar? resource-uri)
        (apply jar/path-if-exists path-elements)
        (when (Files/exists (Paths/get resource-uri) fs/no-link-option)
          (Paths/get resource-uri))))
    (catch Exception e
      nil)))

(s/defn fs-prefix
  [java-path
   base-path
   path]
  (let [java-path-str (.toString java-path)]
    (subs java-path-str 0 (- (count java-path-str) (+ (count base-path) (count path) 2)))))

(s/defn get-resources ;:- [this/Resource]
  "base-path is sensible for getting the right jar from classpath. So base-path 
   should be specific enough for the jar desired. Paths must not be empty."
  [base-path :- this/VirtualPath
   paths :- [this/VirtualPath]]
  (flatten
   (map
    (fn [p]
      (if-let [path-to-work-with (path-if-exists base-path p)]
        (if (jar/is-from-classpath-jar? (.toUri path-to-work-with))
          (jar/get-resources base-path [p])
          (fs/get-resources
           (fs-prefix path-to-work-with base-path p)
           base-path
           [p]
           :java-classpath-filesystem))
        []))
    paths)))