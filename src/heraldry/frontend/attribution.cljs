(ns heraldry.frontend.attribution
  (:require
   [heraldry.attribution :as attribution]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(defn general [context attribution-type]
  [:div.credit
   (if (interface/get-raw-data (update context :path conj :id))
     (let [{:keys [nature
                   license
                   license-version
                   source-name
                   source-creator-name
                   source-creator-link
                   source-link
                   source-license
                   source-license-version]} (interface/get-sanitized-data (update context :path conj :attribution))
           title (interface/get-raw-data (update context :path conj :name))
           username (interface/get-raw-data (update context :path conj :username))
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
        [tr strings/by]
        [:a {:href (attribution/full-url-for-username username)
             :target "_blank"} username]
        " "
        (case (or license :none)
          :none [tr {:en "is private"
                     :de "ist privat"}]
          :public-domain [tr {:en "is in the public domain"
                              :de "ist gemeinfrei"}]
          [:<> [tr {:en "is licensed under "
                    :de "ist lizensiert unter "}]
           [:a {:href license-url :target "_blank"} license-display-name]])
        (when (= nature :derivative)
          [:div.sub-credit
           [tr {:en "source: "
                :de "Quelle: "}]
           (if (-> source-name count pos?)
             [:a {:href source-link
                  :target "_blank"} " " source-name]
             [tr {:en "unnamed"
                  :de "unbenamt"}])
           (when (-> source-creator-name count pos?)
             [:<>
              [tr strings/by]
              [:a {:href source-creator-link
                   :target "_blank"} source-creator-name]])
           " "
           (case (or source-license :none)
             :none [tr {:en "is private"
                        :de "ist privat"}]
             :public-domain [tr {:en "is in the public domain"
                                 :de "ist gemeinfrei"}]
             [:<> [tr {:en "is licensed under "
                       :de "ist lizensiert unter "}]
              [:a {:href source-license-url :target "_blank"} source-license-display-name]])])])
     [tr {:en "unsaved data"
          :de "ungespeicherte Daten"}])])

(defn for-charge [context]
  [general context :charge])

(defn for-arms [context]
  [general context :arms])

(defn for-collection [context]
  [general context :collection])

(defn for-ribbon [context]
  [general context :ribbon])
