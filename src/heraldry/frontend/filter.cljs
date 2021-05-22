(ns heraldry.frontend.filter
  (:require [clojure.set :as set]
            [clojure.string :as s]
            [heraldry.frontend.util :as util]))

(defn items [item-list filter-string filter-keys filter-tags]
  (if (and (or (not filter-string)
               (-> filter-string s/trim count zero?))
           (-> filter-tags count zero?))
    item-list
    (let [words (-> filter-string
                    (s/split #" +")
                    (->> (map s/lower-case)))
          filter-tags-set (-> filter-tags
                              keys
                              set)]
      (filterv (fn [arms]
                 (and (every? (fn [word]
                                (some (fn [attribute]
                                        (-> arms
                                            (get attribute)
                                            (util/matches-word word)))
                                      filter-keys))
                              words)
                      (set/subset? filter-tags-set
                                   (-> arms
                                       :tags
                                       keys
                                       set))))
               item-list))))
