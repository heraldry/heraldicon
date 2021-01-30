(ns heraldry.frontend.credits
  (:require [heraldry.util :as util]))

(defn general [title url username data]
  (let [license (-> data :license)
        source-license (-> data :source-license)]
    [:div.credit
     [:a {:href url
          :target "_blank"} title]
     " by "
     [:a {:href (util/full-url-for-username username)
          :target "_blank"} username]
     " "
     (cond
       (= license :cc-attribution) [:<> "is licensed under "
                                    [:a {:href "https://creativecommons.org/licenses/by/4.0"
                                         :target "_blank"} "CC BY"]]
       (= license :cc-attribution-share-alike) [:<> "is licensed under "
                                                [:a {:href "https://creativecommons.org/licenses/by-sa/4.0"
                                                     :target "_blank"} "CC BY-SA"]]
       (= license :public-domain) [:<> "is in the public domain"]
       :else "is private")
     (when (-> data :nature (= :derivative))
       [:div.sub-credit
        "source: "
        [:a {:href (-> data :source-link)
             :target "_blank"} " " (-> data :source-name)]
        " by "
        [:a {:href (-> data :source-creator-link)
             :target "_blank"} (-> data :source-creator-name)]
        " "
        (cond
          (= source-license :cc-attribution) [:<> "is licensed under "
                                              [:a {:href "https://creativecommons.org/licenses/by/4.0"
                                                   :target "_blank"} "CC BY"]]
          (= source-license :cc-attribution-share-alike) [:<> "is licensed under "
                                                          [:a {:href "https://creativecommons.org/licenses/by-sa/4.0"
                                                               :target "_blank"} "CC BY-SA"]]
          (= source-license :public-domain) [:<> "is in the "
                                             [:a {:href "https://creativecommons.org/publicdomain/mark/1.0/"
                                                  :target "_blank"} "public domain"]]
          :else "is private")])]))

(defn for-charge [charge]
  (when (:id charge)
    (let [attribution (-> charge :attribution)
          username (:username charge)
          title (str " " (-> charge :type name) ": " (:name charge))
          url (util/full-url-for-charge charge)]
      [general title url username attribution])))

(defn for-arms [arms]
  (when (:id arms)
    (let [attribution (-> arms :attribution)
          username (:username arms)
          title (:name arms)
          url (util/full-url-for-arms arms)]
      [general title url username attribution])))
