(ns heraldry.frontend.credits
  (:require [heraldry.attribution :as attribution]
            [heraldry.util :as util]))

(defn general [title url username data]
  #_(let [{:keys [license
                  license-version
                  source-license
                  source-license-version]} data
          license-url (attribution/url license license-version)
          license-display-name (attribution/display-name license license-version)
          source-license-url (attribution/url source-license source-license-version)
          source-license-display-name (attribution/display-name source-license source-license-version)]
      [:div.credit
       [:a {:href url
            :target "_blank"} title]
       " by "
       [:a {:href (util/full-url-for-username username)
            :target "_blank"} username]
       " "
       (case (or license :none)
         :none "is private"
         :public-domain "is in the public domain"
         [:<> "is licensed under "
          [:a {:href license-url :target "_blank"} license-display-name]])
       (when (-> data :nature (= :derivative))
         [:div.sub-credit
          "source: "
          [:a {:href (-> data :source-link)
               :target "_blank"} " " (-> data :source-name)]
          " by "
          [:a {:href (-> data :source-creator-link)
               :target "_blank"} (-> data :source-creator-name)]
          " "
          (case (or source-license :none)
            :none "is private"
            :public-domain "is in the public domain"
            [:<> "is licensed under "
             [:a {:href source-license-url :target "_blank"} source-license-display-name]])])]))

(defn for-charge [charge]
  #_(when (:id charge)
      (let [attribution (-> charge :attribution)
            username (:username charge)
            title (str " " (-> charge :type name) ": " (:name charge))
            url (util/full-url-for-charge charge)]
        [general title url username attribution])))

(defn for-arms [arms]
  #_(when (:id arms)
      (let [attribution (-> arms :attribution)
            username (:username arms)
            title (:name arms)
            url (util/full-url-for-arms arms)]
        [general title url username attribution])))

(defn for-collection [collection]
  #_(when (:id collection)
      (let [attribution (-> collection :attribution)
            username (:username collection)
            title (:name collection)
            url (util/full-url-for-collection collection)]
        [general title url username attribution])))
