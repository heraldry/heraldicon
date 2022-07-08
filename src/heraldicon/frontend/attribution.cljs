(ns heraldicon.frontend.attribution
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.interface :as interface]))

(defn- credits [{:keys [url title username
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
      " "
      [tr :string.miscellaneous/by]
      " "
      [:a {:href (attribution/full-url-for-username username)
           :target "_blank"} username]
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

(defn- general [context attribution-type]
  [:div.credit
   (if (interface/get-raw-data (c/++ context :id))
     (let [attribution (interface/get-sanitized-data (c/++ context :attribution))
           title (interface/get-raw-data (c/++ context :name))
           username (interface/get-raw-data (c/++ context :username))
           url (case attribution-type
                 :arms (attribution/full-url-for-arms context)
                 :charge (attribution/full-url-for-charge context)
                 :collection (attribution/full-url-for-collection context)
                 :ribbon (attribution/full-url-for-ribbon context))]
       [credits (assoc attribution
                       :title title
                       :username username
                       :url url)])
     [tr :string.miscellaneous/unsaved-data])])

(defn for-charge [context]
  [general context :charge])

(defn for-arms [context]
  [general context :arms])

(defn for-collection [context]
  [general context :collection])

(defn for-ribbon [context]
  [general context :ribbon])

(defn for-escutcheon [context]
  (let [escutcheon (interface/render-option :escutcheon context)
        escutcheon-attribution (escutcheon/attribution escutcheon)]
    [credits (assoc escutcheon-attribution
                    :title (escutcheon/escutcheon-map escutcheon))]))
