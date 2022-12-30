(ns heraldicon.frontend.attribution
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.interface :as interface]
   [heraldicon.render.theme :as theme]
   [re-frame.core :as rf]))

(defn- credits [{:keys [url title username
                        creator-name
                        creator-link
                        nature
                        license license-version
                        source-license source-license-version
                        source-name source-link source-creator-name source-creator-link
                        source-modification]}]
  (let [license-url (attribution/license-url license license-version)
        license-display-name (attribution/license-display-name license license-version)
        source-license-url (attribution/license-url source-license source-license-version)
        source-license-display-name (attribution/license-display-name source-license source-license-version)
        source-modification (s/trim (or source-modification ""))]
    [:div.credit
     [:<>
      (if url
        [:a {:href url
             :target "_blank"} [tr title]]
        [:span [tr title]])
      (if username
        [:<>
         " "
         [tr :string.miscellaneous/by]
         " "
         [:a {:href (attribution/full-url-for-username username)
              :target "_blank"} username]]
        (when (-> creator-name count pos?)
          [:<>
           " "
           [tr :string.miscellaneous/by]
           " "
           (if creator-link
             [:a {:href creator-link
                  :target "_blank"} creator-name]
             [:span creator-name])]))
      " "
      (case (or license :none)
        :none [tr :string.attribution/is-private]
        :public-domain [tr :string.attribution/is-in-the-public-domain]
        [:<> [tr :string.attribution/is-licensed-under] " "
         [:a {:href license-url :target "_blank"} license-display-name]])
      (when (= nature :derivative)
        [:div.sub-credit
         [tr :string.attribution/source]
         ": "
         (if (-> source-name count pos?)
           [:a {:href source-link
                :target "_blank"} " " source-name]
           [tr :string.miscellaneous/unnamed])
         (when (-> source-creator-name count pos?)
           [:<>
            " "
            [tr :string.miscellaneous/by]
            " "
            [:a {:href source-creator-link
                 :target "_blank"} source-creator-name]])
         " "
         (case (or source-license :none)
           :none [tr :string.attribution/is-private]
           :public-domain [tr :string.attribution/is-in-the-public-domain]
           [:<> [tr :string.attribution/is-licensed-under] " "
            [:a {:href source-license-url :target "_blank"} source-license-display-name]])
         (when (-> source-modification count pos?)
           [:<>
            " " [tr :string.attribution/modifications] ": " source-modification])])]]))

(defn for-entity [context]
  (let [id (interface/get-raw-data (c/++ context :id))]
    [:div.credit
     (if id
       (let [attribution (interface/get-sanitized-data (c/++ context :attribution))
             title (interface/get-raw-data (c/++ context :name))
             username (interface/get-raw-data (c/++ context :username))
             url (attribution/full-url-for-entity context)]
         [credits (assoc attribution
                         :title title
                         :username username
                         :url url)])
       [tr :string.miscellaneous/unsaved-data])]))

(defn for-escutcheons [escutcheon inescutcheons]
  (let [all-escutcheons (sort (conj inescutcheons escutcheon))]
    [:<>
     [:h3 [tr :string.render-options/escutcheon]]
     (for [escutcheon all-escutcheons]
       ^{:key escutcheon}
       [credits (assoc (escutcheon/attribution escutcheon)
                       :title (escutcheon/escutcheon-map escutcheon))])]))

(defn for-theme [theme]
  [:<>
   [:h3 [tr :string.render-options/theme]]
   [credits (assoc (theme/attribution theme)
                   :title (theme/theme-map theme))]])

(defn for-entities [entities]
  (let [entity-paths (keep (fn [{:keys [id version]}]
                             (when id
                               (some-> @(rf/subscribe [::entity-for-rendering/data id version])
                                       :path
                                       (conj :entity)))) entities)
        entity-type (when-let [path (first entity-paths)]
                      (interface/get-raw-data {:path (conj path :type)}))
        title (case entity-type
                :heraldicon.entity.type/arms :string.entity/arms
                :heraldicon.entity.type/charge :string.entity/charges
                :heraldicon.entity.type/ribbon :string.entity/ribbons
                :heraldicon.entity.type/collection :string.entity/collections
                nil)]
    (when (seq entity-paths)
      [:<>
       [:h3 [tr title]]
       (into [:ul]
             (keep (fn [path]
                     ^{:key path}
                     [:li [for-entity {:path path}]]))
             entity-paths)])))

(defn attribution [context & sections]
  (into [:div.attribution
         [:h3 [tr :string.attribution/license]]
         [for-entity context]]
        sections))
