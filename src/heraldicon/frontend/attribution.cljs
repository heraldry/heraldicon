(ns heraldicon.frontend.attribution
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]))

(defn- general [context attribution-type]
  [:div.credit
   (if (interface/get-raw-data (c/++ context :id))
     (let [{:keys [nature
                   license
                   license-version
                   source-name
                   source-creator-name
                   source-creator-link
                   source-link
                   source-license
                   source-license-version]} (interface/get-sanitized-data (c/++ context :attribution))
           title (interface/get-raw-data (c/++ context :name))
           username (interface/get-raw-data (c/++ context :username))
           url (case attribution-type
                 :arms (attribution/full-url-for-arms context)
                 :charge (attribution/full-url-for-charge context)
                 :collection (attribution/full-url-for-collection context)
                 :ribbon (attribution/full-url-for-ribbon context))
           license-url (attribution/license-url license license-version)
           license-display-name (attribution/license-display-name license license-version)
           source-license-url (attribution/license-url source-license source-license-version)
           source-license-display-name (attribution/license-display-name source-license source-license-version)]
       [:<>
        [:a {:href url
             :target "_blank"} title]
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
              [:a {:href source-license-url :target "_blank"} source-license-display-name]])])])
     [tr :string.miscellaneous/unsaved-data])])

(defn for-charge [context]
  [general context :charge])

(defn for-arms [context]
  [general context :arms])

(defn for-collection [context]
  [general context :collection])

(defn for-ribbon [context]
  [general context :ribbon])
