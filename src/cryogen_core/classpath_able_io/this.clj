;   Copyright (c) meissa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cryogen-core.classpath-able-io.this
  (:require [clojure.string :as st]
            [schema.core :as s]))

(def SourceType (s/enum :java-classpath-filesystem :java-classpath-jar :filesystem))
(def ResourceType (s/enum :file :dir :unknown))
(def Prefix s/Str)
(def JavaUri s/Any) ; java.net.URI
(def VirtualPath s/Str)
(def JavaPath s/Any) ; java.nio.Path
(def Resource
  {:virtual-path  VirtualPath
   :source-type   SourceType
   :resource-type ResourceType
   :java-uri      JavaUri
   :java-path     JavaPath})

(s/defn is-file? :- s/Bool
  [resource :- Resource]
  (= :file (:resource-type resource)))

(s/defn is-dir? :- s/Bool
  [resource :- Resource]
  (= :dir (:resource-type resource)))

(s/defn virtual-path-from-elements :- VirtualPath
  [& path-elements ;:- VirtualPath
   ]
  (st/join "/"
           (filter #(not (empty? %))
                   path-elements)))

(s/defn  compare-resource 
  [first :- Resource
   second :- Resource]
  (compare (:virtual-path first) (:virtual-path second)))
