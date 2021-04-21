(ns heraldry.frontend.form.attribution
  (:require [heraldry.frontend.form.element :as element]
            [heraldry.license :as license]
            [re-frame.core :as rf]))

(defn form [db-path & {:keys [charge-presets?]}]
  (let [current-data @(rf/subscribe [:get db-path])]
    [element/component db-path :attribution "Attribution / License" nil
     (when charge-presets?
       [element/select [:ui :dummy :attribution-preset]
        "Presets"
        [["-- auto fill for --" :none]
         ["WappenWiki" :wappenwiki]
         ["Wikimedia" :wikimedia]]
        :on-change (fn [value]
                     (case value
                       :wappenwiki (rf/dispatch [:set
                                                 db-path
                                                 (-> current-data
                                                     (assoc :nature :derivative)
                                                     (assoc :license :cc-attribution-non-commercial-share-alike)
                                                     (assoc :license-version :v4)
                                                     (assoc :source-license :cc-attribution-non-commercial-share-alike)
                                                     (assoc :source-license-version :v3)
                                                     (assoc :source-creator-name "WappenWiki")
                                                     (assoc :source-creator-link "http://wappenwiki.org"))])
                       :wikimedia  (rf/dispatch [:set
                                                 db-path
                                                 (-> current-data
                                                     (assoc :nature :derivative)
                                                     (assoc :license :cc-attribution-share-alike)
                                                     (assoc :license-version :v4)
                                                     (assoc :source-license :cc-attribution-share-alike)
                                                     (assoc :source-license-version :v3))])
                       nil))])

     [element/select (conj db-path :license) "License"
      (assoc-in
       license/license-choices
       [0 0]
       "None (no sharing)")]
     (when (-> current-data
               :license
               license/cc-license?)
       [element/select (conj db-path :license-version) "License version"
        license/cc-license-version-choices
        :default :v4])
     [element/radio-select (conj db-path :nature) license/nature-choices
      :default :own-work]
     (when (-> current-data :nature (= :derivative))
       [:<>
        [element/select (conj db-path :source-license) "Source license"
         license/license-choices]
        (when (-> current-data
                  :source-license
                  license/cc-license?)
          [element/select (conj db-path :source-license-version) "License version"
           license/cc-license-version-choices
           :default :v4])
        [element/text-field (conj db-path :source-name) "Source name" :style {:width "19em"}]
        [element/text-field (conj db-path :source-link) "Source link" :style {:width "19em"}]
        [element/text-field (conj db-path :source-creator-name) "Creator name" :style {:width "19em"}]
        [element/text-field (conj db-path :source-creator-link) "Creator link" :style {:width "19em"}]])
     [:div {:style {:margin-bottom "1em"}} " "]]))
