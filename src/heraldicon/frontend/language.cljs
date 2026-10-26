(ns heraldicon.frontend.language
  (:require
   [clojure.string :as str]
   [heraldicon.config :as config]
   [heraldicon.frontend.header :as-alias header]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.localization.locale :as locale]
   [heraldicon.localization.string :as string]
   [hodgepodge.core :refer [get-item local-storage set-item]]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private language-path
  [:ui :language])

(def ^:private requested-language-path
  [:ui :requested-language])

(def ^:private language-menu-open?-path
  [:ui :menu :language-menu :open?])

(def ^:private local-storage-language-name
  "hd-language")

(defn- store-language-setting [language]
  (set-item local-storage local-storage-language-name language))

(rf/reg-sub ::selected-language
  (fn [db _]
    (or (get-in db language-path)
        :en)))

(defn- set-language [db language]
  (store-language-setting language)
  (-> db
      (assoc-in language-path language)
      (assoc-in requested-language-path nil)))

(defn- dictionary-url [language]
  (str "/locales/" (name language) ".json?version=" (config/get :commit)))

(rf/reg-fx ::load-dictionary
  (fn [language]
    (-> (js/fetch (dictionary-url language))
        (.then (fn [^js response]
                 (if (.-ok response)
                   (.json response)
                   (throw (ex-info "dictionary request failed" {:status (.-status response)})))))
        (.then (fn [data]
                 (locale/register! language data)
                 (rf/dispatch [::activate language])))
        (.catch (fn [error]
                  (log/error error "load dictionary error" {:language language}))))))

(macros/reg-event-db ::activate
  (fn [db [_ language]]
    ;; a later selection wins over a dictionary that arrives late
    (if (= (get-in db requested-language-path) language)
      (set-language db language)
      db)))

(defn- select-language [db language]
  (cond
    (not (locale/all language)) {}
    (locale/loaded? language) {:db (set-language db language)}
    :else {:db (assoc-in db requested-language-path language)
           ::load-dictionary language}))

(macros/reg-event-fx ::set
  (fn [{:keys [db]} [_ language]]
    (select-language db language)))

(macros/reg-event-fx ::load-setting
  (fn [{:keys [db]} _]
    (let [loaded-language (get-item local-storage local-storage-language-name ":en")
          loaded-language (cond-> loaded-language
                            (str/starts-with? loaded-language ":") (->
                                                                     (subs 1)
                                                                     keyword))]
      (select-language db loaded-language))))

(defn tr [data]
  (string/tr-raw data @(rf/subscribe [::selected-language])))

(defn- selected-language-option []
  (let [selected-language-code @(rf/subscribe [::selected-language])
        title (get locale/all selected-language-code)]
    [tr title]))

(defn- language-option [language-code & {:keys [on-click]}]
  (let [title (get locale/all language-code)]
    [:a.nav-menu-link {:href "#"
                       :on-click (js-event/handled on-click)}
     [tr title]]))

(defn selector []
  [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
   {:on-mouse-leave #(rf/dispatch [::header/clear-menu-open?
                                   language-menu-open?-path])}
   [:<>
    [:a.nav-menu-link {:href "#"
                       :on-click (js-event/handled
                                  #(rf/dispatch [::header/toggle-menu-open?
                                                 language-menu-open?-path]))}
     [selected-language-option]]
    (into [:ul.nav-menu.nav-menu-children
           {:style {:display (if @(rf/subscribe [::header/menu-open?
                                                 language-menu-open?-path])
                               "block"
                               "none")}}]
          (map (fn [language-code]
                 ^{:key language-code}
                 [:li.nav-menu-item
                  [language-option language-code
                   :on-click #(rf/dispatch [::set language-code])]]))
          (keys locale/all))]])
