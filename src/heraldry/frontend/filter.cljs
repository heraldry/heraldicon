(ns heraldry.frontend.filter
  (:require [clojure.set :as set]
            [clojure.string :as s]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.ui.element.tags :as tags]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

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

(defn filter-items [user-data item-list filter-keys filter-string filter-tags filter-access filter-own]
  (let [words (-> filter-string
                  (s/split #" +")
                  (->> (map s/lower-case)))
        filter-tags-set (-> filter-tags
                            keys
                            set)]
    (filterv (fn [item]
               (and (or (not filter-own)
                        (= (:username item)
                           (:username user-data)))
                    (case filter-access
                      :public (:is-public item)
                      :private (not (:is-public item))
                      true)
                    (every? (fn [word]
                              (some (fn [attribute]
                                      (-> item
                                          (get attribute)
                                          (util/matches-word word)))
                                    filter-keys))
                            words)
                    (set/subset? filter-tags-set
                                 (-> item
                                     :tags
                                     keys
                                     set))))
             item-list)))

(defn component [id user-data all-items filter-keys display-fn refresh-fn & {:keys [hide-ownership-filter?
                                                                                    hide-access-filter?
                                                                                    on-filter-string-change]}]
  (let [filter-path [:ui :filter id]
        filter-string-path (conj filter-path :filter-string)
        filter-tags-path (conj filter-path :filter-tags)
        filter-access-path (conj filter-path :filter-access)
        filter-own-path (conj filter-path :filter-own)
        filter-string @(rf/subscribe [:get filter-string-path])
        filter-tags @(rf/subscribe [:get filter-tags-path])
        filter-access @(rf/subscribe [:get filter-access-path])
        filter-own @(rf/subscribe [:get filter-own-path])
        filtered-items (filter-items user-data
                                     all-items
                                     filter-keys
                                     filter-string
                                     filter-tags
                                     filter-access
                                     filter-own)
        tags-to-display (->> filtered-items
                             (map (comp keys :tags))
                             (apply concat)
                             set
                             (set/union (-> filter-tags keys set)))
        filtered? (or (-> filter-string count pos?)
                      (-> filter-tags count pos?))]
    [:<>
     [element/search-field filter-string-path
      :on-change (fn [value]
                   (rf/dispatch-sync [:set filter-string-path value])
                   (when on-filter-string-change
                     (on-filter-string-change)))]
     (when refresh-fn
       [:a {:style {:margin-left "0.5em"}
            :on-click #(do
                         (refresh-fn)
                         (.stopPropagation %))} [:i.fas.fa-sync-alt]])
     (when-not hide-ownership-filter?
       [element/checkbox filter-own-path "Mine only"])
     (when-not hide-access-filter?
       [element/radio-select filter-access-path [["All" :all]
                                                 ["Public" :public]
                                                 ["Private" :private]]
        :default :all])
     [:div
      [tags/tags-view tags-to-display
       :on-click #(rf/dispatch [:toggle-tag filter-tags-path %])
       :selected filter-tags]]
     (if (empty? filtered-items)
       [:div "None"]
       [display-fn
        :items filtered-items
        :filtered? filtered?])]))
