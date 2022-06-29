(ns heraldicon.frontend.language
  (:require
   [clojure.string :as s]
   [heraldicon.frontend.header :as-alias header]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.state :as state]
   [heraldicon.localization.locale :as locale]
   [heraldicon.localization.string :as string]
   [hodgepodge.core :refer [get-item local-storage set-item]]
   [re-frame.core :as rf]))

(def ^:private language-path
  [:ui :language])

(def ^:private language-menu-open?-path
  [:ui :menu :language-menu :open?])

(def ^:private local-storage-language-name
  "hd-language")

(defn- browser-preferred-language []
  (when-let [language (or (first js/navigator.languages)
                          js/navigator.language
                          js/navigator.userLanguage)]
    (let [known-language-keys (-> locale/all
                                  keys
                                  set)]
      (or (get known-language-keys (keyword language))
          (get known-language-keys (-> language (s/split #"-") first keyword))))))

(defn- store-language-setting [language]
  (set-item local-storage local-storage-language-name language))

(rf/reg-sub ::selected-language
  (fn [db _]
    (or (get-in db language-path)
        :en)))

(defn- set-language [db language]
  (if (locale/all language)
    (do
      (store-language-setting language)
      (assoc-in db language-path language))
    db))

(macros/reg-event-db ::set
  (fn [db [_ language]]
    (set-language db language)))

(macros/reg-event-db ::load-language-setting
  (fn [db _]
    (browser-preferred-language)
    (let [loaded-language (get-item local-storage local-storage-language-name ":en")
          loaded-language (cond-> loaded-language
                            (s/starts-with? loaded-language ":") (->
                                                                   (subs 1)
                                                                   keyword))]
      (if loaded-language
        (set-language db loaded-language)
        (set-language db (browser-preferred-language))))))

(defn tr [data]
  (string/tr-raw data @(rf/subscribe [::selected-language])))

(defn- selected-language-option []
  (let [selected-language-code @(rf/subscribe [::selected-language])
        title (get locale/all selected-language-code)]
    [tr title]))

(defn- language-option [language-code & {:keys [on-click]}]
  (let [title (get locale/all language-code)]
    [:a.nav-menu-link {:href "#"
                       :on-click (fn [event]
                                   (doto event
                                     .preventDefault
                                     .stopPropagation)
                                   (when on-click
                                     (on-click)))}
     [tr title]]))

(defn selector []
  [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
   {:on-mouse-leave #(rf/dispatch [::header/clear-menu-open?
                                   language-menu-open?-path])}
   [:<>
    [:a.nav-menu-link {:href "#"
                       :on-click #(state/dispatch-on-event-and-prevent-default
                                   % [::header/toggle-menu-open?
                                      language-menu-open?-path])}
     [selected-language-option]
     " "]
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
