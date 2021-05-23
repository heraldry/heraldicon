(ns heraldry.frontend.form.arms-select
  (:require [clojure.string :as s]
            [heraldry.frontend.filter :as filter]
            [heraldry.frontend.form.tag :as tag]
            [heraldry.frontend.user :as user]))

(defn component [arms-list link-fn refresh-fn & {:keys [hide-ownership-filter?]}]
  (let [user-data (user/data)]
    [filter/component
     :arms-list
     user-data
     arms-list
     [:name :username :tags]
     (fn [& {:keys [items]}]
       [:ul.arms-list
        (doall
         (for [arms (sort-by (comp s/lower-case :name) items)]
           ^{:key (:id arms)}
           [:li.arms
            (if (-> arms :is-public)
              [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
              [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]])
            " "
            [link-fn arms]
            " "
            [tag/tags-view (-> arms :tags keys)]]))])
     refresh-fn
     :hide-ownership-filter? hide-ownership-filter?]))
