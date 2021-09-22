(ns heraldry.frontend.language
  (:require [clojure.string :as s]
            [heraldry.frontend.macros :as macros]
            [hodgepodge.core :refer [get-item local-storage set-item]]
            [re-frame.core :as rf]))

(def language-path
  [:ui :language])

(def local-storage-language-name
  "hd-language")

(def known-languages
  #{:en :de})

(defn store-language-setting [language]
  (set-item local-storage local-storage-language-name language))

(rf/reg-sub ::selected-language
  (fn [db _]
    (or (get-in db language-path)
        :en)))

(rf/reg-sub ::selector-opacity
  (fn [_ _]
    (rf/subscribe [::selected-language]))

  (fn [selected-language [_ language]]
    (if (= selected-language language)
      1
      0.25)))

(defn set-language [db language]
  (if (known-languages language)
    (do
      (store-language-setting language)
      (assoc-in db language-path language))
    db))

(macros/reg-event-db ::set-language
  (fn [db [_ language]]
    (set-language db language)))

(macros/reg-event-db ::load-language-setting
  (fn [db _]
    (let [loaded-language (get-item local-storage local-storage-language-name)
          loaded-language (cond-> loaded-language
                            (s/starts-with? loaded-language ":") (-> (subs 1) keyword))]
      (set-language db loaded-language))))

(defn selector []
  [:div {:style {:white-space "nowrap"
                 :position "relative"
                 :top "50%"
                 :transform "translate(0,-50%)"}}
   [:img {:src "/img/flag-united-kingdom.svg"
          :on-click #(rf/dispatch [::set-language :en])
          :style {:width "2em"
                  :height "1em"
                  :cursor "pointer"
                  :opacity @(rf/subscribe [::selector-opacity :en])}}]
   " "
   [:img {:src "/img/flag-germany.svg"
          :on-click #(rf/dispatch [::set-language :de])
          :style {:width "2em"
                  :height "1em"
                  :cursor "pointer"
                  :opacity @(rf/subscribe [::selector-opacity :de])}}]])

(defn tr-raw [data language]
  (if (map? data)
    (get data
         language
         (get data :en))
    data))

(defn tr [data]
  (tr-raw data @(rf/subscribe [::selected-language])))
