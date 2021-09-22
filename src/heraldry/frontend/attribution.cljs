(ns heraldry.frontend.attribution
  (:require [heraldry.attribution :as attribution]
            [heraldry.frontend.language :refer [tr]]
            [heraldry.frontend.strings :as strings]
            [heraldry.interface :as interface]))

(defn general [path attribution-type context]
  [:div.credit
   (if (interface/get-raw-data (conj path :id) context)
     (let [{:keys [nature
                   license
                   license-version
                   source-name
                   source-creator-name
                   source-creator-link
                   source-link
                   source-license
                   source-license-version]} (interface/get-sanitized-data (conj path :attribution) context)
           title (interface/get-raw-data (conj path :name) context)
           username (interface/get-raw-data (conj path :username) context)
           url (case attribution-type
                 :arms (attribution/full-url-for-arms path context)
                 :charge (attribution/full-url-for-charge path context)
                 :collection (attribution/full-url-for-collection path context)
                 :ribbon (attribution/full-url-for-ribbon path context))
           license-url (attribution/license-url license license-version)
           license-display-name (attribution/license-display-name license license-version)
           source-license-url (attribution/license-url source-license source-license-version)
           source-license-display-name (attribution/license-display-name source-license source-license-version)]
       [:<>
        [:a {:href url
             :target "_blank"} title]
        strings/by
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
              strings/by
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

(defn for-charge [path context]
  [general path :charge context])

(defn for-arms [path context]
  [general path :arms context])

(defn for-collection [collection]
  #_(when (:id collection)
      (let [attribution (-> collection :attribution)
            username (:username collection)
            title (:name collection)
            url (util/full-url-for-collection collection)]
        [general title url username attribution])))

(defn for-ribbon [path context]
  [general path :ribbon context])
