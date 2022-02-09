(ns heraldry.frontend.language
  (:require
   [clojure.string :as s]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.strings :refer [known-languages]]
   [heraldry.util :as util]
   [hodgepodge.core :refer [get-item local-storage set-item]]
   [re-frame.core :as rf]))

(def language-path
  [:ui :language])

(def language-menu-open?-path
  [:ui :menu :language-menu :open?])

(def local-storage-language-name
  "hd-language")

(defn browser-preferred-language []
  (when-let [language (or (first js/navigator.languages)
                          js/navigator.language
                          js/navigator.userLanguage)]
    (let [known-language-keys (-> known-languages
                                  keys
                                  set)]
      (or (get known-language-keys (keyword language))
          (get known-language-keys (-> language (s/split #"-") first keyword))))))

(defn store-language-setting [language]
  (set-item local-storage local-storage-language-name language))

(rf/reg-sub ::selected-language
  (fn [db _]
    (or (get-in db language-path)
        :en)))

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
    (browser-preferred-language)
    (let [loaded-language (get-item local-storage local-storage-language-name ":en")
          loaded-language (cond-> loaded-language
                            (s/starts-with? loaded-language ":") (-> (subs 1) keyword))]
      (if loaded-language
        (set-language db loaded-language)
        (set-language db (browser-preferred-language))))))

(defn tr [data]
  (util/tr-raw data @(rf/subscribe [::selected-language])))

(defn selected-language-option []
  (let [selected-language-code @(rf/subscribe [::selected-language])
        title (get known-languages selected-language-code)]
    [tr title]))

(defn language-option [language-code & {:keys [on-click]}]
  (let [title (get known-languages language-code)]
    [:a {:href "#"
         :on-click (fn [event]
                     (doto event
                       .preventDefault
                       .stopPropagation)
                     (when on-click
                       (on-click)))}
     [tr title]]))

(defn selector []
  [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
   {:on-mouse-leave #(rf/dispatch [:heraldry.frontend.header/clear-menu-open?
                                   language-menu-open?-path])}
   [:<>
    [:a.nav-menu-link {:href "#"
                       :on-click #(state/dispatch-on-event-and-prevent-default
                                   % [:heraldry.frontend.header/toggle-menu-open?
                                      language-menu-open?-path])}
     [selected-language-option]
     " "]
    [:ul.nav-menu.nav-menu-children
     {:style {:display (if @(rf/subscribe [:heraldry.frontend.header/menu-open?
                                           language-menu-open?-path])
                         "block"
                         "none")}}
     (doall
      (for [language-code (keys known-languages)]
        ^{:key language-code}
        [:li.nav-menu-item
         [:a.nav-menu-link
          [language-option language-code
           :on-click #(state/dispatch-on-event-and-prevent-default
                       % [::set-language language-code])]]]))]]])
