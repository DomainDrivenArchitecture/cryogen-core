(ns cryogen-core.rss
  (:require [clj-rss.core :as rss]
            [cryogen-core.new-io :as new-io]
            [text-decoration.core :refer :all]
            [cryogen-core.io :as cryogen-io])
  (:import java.util.Date))

(defn posts-to-items [^String site-url posts]
  (map
   (fn [{:keys [uri title content date enclosure author description]}]
     (let [link (str (if (.endsWith site-url "/") (apply str (butlast site-url)) site-url) uri)
           enclosure (if (nil? enclosure) "" enclosure)]
       {:guid        link
        :link        link
        :title       title
        :description (or description content)
        :author      author
        :enclosure   enclosure
        :pubDate     date}))
   posts))

(defn make-channel [config posts]
  (apply
   (partial rss/channel-xml
            false
            {:title         (:site-title config)
             :link          (:site-url config)
             :description   (:description config)
             :lastBuildDate (Date.)})
   (posts-to-items (:site-url config) posts)))

(defn make-filtered-channels [{:keys [rss-filters blog-prefix] :as config} posts-by-tag]
  (doseq [filter rss-filters]
    (let [uri (new-io/path "/" blog-prefix (str (name filter) ".xml"))]
      (println "\t-->" (cyan uri))
      (cryogen-io/create-file uri (make-channel config (get posts-by-tag filter))))))
