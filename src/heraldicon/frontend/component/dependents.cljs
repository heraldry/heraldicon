(ns heraldicon.frontend.component.dependents
  (:require
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(defn- and-in-private-key [dependent-type]
  (case dependent-type
    :heraldicon.entity.type/arms :string.dependents/and-in-private-arms
    :heraldicon.entity.type/collection :string.dependents/and-in-private-collections
    nil))

(defn- entry [{:keys [name username by-current-user?] :as entity}]
  ^{:key (:id entity)}
  [:li
   [:a {:href (attribution/full-url-for-entity-data entity)}
    name]
   " "
   [tr :string.miscellaneous/by]
   " "
   (if by-current-user?
     [tr :string.dependents/you]
     [:a {:href (attribution/full-url-for-username username)}
      username])])

(defn used-by [form-db-path]
  (let [{:keys [visible other-private-count dependent-type]}
        @(rf/subscribe [:get (conj form-db-path :dependents)])]
    (when (or (seq visible)
              (pos? (or other-private-count 0)))
      [:div.dependents
       [:h3 [tr :string.dependents/used-in]]
       (when (seq visible)
         (into [:ul]
               (map entry)
               visible))
       (when (pos? (or other-private-count 0))
         (when-let [key (and-in-private-key dependent-type)]
           [:p.dependents-private-count
            (string/format-tr (tr key) other-private-count)]))])))
