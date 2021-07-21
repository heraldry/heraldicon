(ns heraldry.frontend.attribution
  (:require [heraldry.attribution :as attribution]
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
           url (attribution/full-url path attribution-type context)
           license-url (attribution/license-url license license-version)
           license-display-name (attribution/license-display-name license license-version)
           source-license-url (attribution/license-url source-license source-license-version)
           source-license-display-name (attribution/license-display-name source-license source-license-version)]
       [:<>
        [:a {:href url
             :target "_blank"} title]
        " by "
        [:a {:href (attribution/full-url-for-username username)
             :target "_blank"} username]
        " "
        (case (or license :none)
          :none "is private"
          :public-domain "is in the public domain"
          [:<> "is licensed under "
           [:a {:href license-url :target "_blank"} license-display-name]])
        (when (= nature :derivative)
          [:div.sub-credit
           "source: "
           (if (-> source-name count pos?)
             [:a {:href source-link
                  :target "_blank"} " " source-name]
             "unnamed")
           (when (-> source-creator-name count pos?)
             [:<>
              " by "
              [:a {:href source-creator-link
                   :target "_blank"} source-creator-name]])
           " "
           (case (or source-license :none)
             :none "is private"
             :public-domain "is in the public domain"
             [:<> "is licensed under "
              [:a {:href source-license-url :target "_blank"} source-license-display-name]])])])
     "unsaved data")])

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
