(ns heraldry.frontend.credits
  (:require [heraldry.frontend.util :as util]
            [reitit.frontend.easy :as reife]))

(defn general [title url username data]
  (let [license        (-> data :license)
        source-license (-> data :source-license)]
    [:div.credit
     [:a {:href   url
          :target "_blank"} title]
     " by "
     [:a {:href "#"} username]
     " "
     (cond
       (= license :none)                       "is private"
       (= license :cc-attribution)             [:<> "is licensed under "
                                                [:a {:href   "https://creativecommons.org/licenses/by/4.0"
                                                     :target "_blank"} "CC BY"]]
       (= license :cc-attribution-share-alike) [:<> "is licensed under "
                                                [:a {:href   "https://creativecommons.org/licenses/by-sa/4.0"
                                                     :target "_blank"} "CC BY-SA"]]
       (= license :public-domain)              [:<> "is in the public domain"])
     (when (-> data :nature (= :derivative))
       [:div.sub-credit
        "source: "
        [:a {:href   (-> data :source-link)
             :target "_blank"} " " (-> data :source-name)]
        " by "
        [:a {:href   (-> data :source-creator-link)
             :target "_blank"} (-> data :source-creator-name)]
        " "
        (cond
          (= source-license :none)                       "is private"
          (= source-license :cc-attribution)             [:<> "is licensed under "
                                                          [:a {:href   "https://creativecommons.org/licenses/by/4.0"
                                                               :target "_blank"} "CC BY"]]
          (= source-license :cc-attribution-share-alike) [:<> "is licensed under "
                                                          [:a {:href   "https://creativecommons.org/licenses/by-sa/4.0"
                                                               :target "_blank"} "CC BY-SA"]]
          (= source-license :public-domain)              [:<> "is in the "
                                                          [:a {:href   "https://creativecommons.org/publicdomain/mark/1.0/"
                                                               :target "_blank"} "public domain"]])])]))

(defn for-charge [charge]
  (when-let [charge-id (:id charge)]
    (let [attribution (-> charge :attribution)
          username    (:username charge)
          title       (str " " (-> charge :type name) ": " (:name charge))
          ;; TODO: external URL
          url         (reife/href :view-charge-by-id {:id (util/id-for-url charge-id)})]
      [general title url username attribution])))
