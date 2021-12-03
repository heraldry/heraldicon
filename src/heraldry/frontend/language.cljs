(ns heraldry.frontend.language
  (:require
   [clojure.string :as s]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.static :as static]
   [heraldry.util :as util]
   [hodgepodge.core :refer [get-item local-storage set-item]]
   [re-frame.core :as rf]))

(def language-path
  [:ui :language])

(def language-menu-open?-path
  [:ui :menu :language-menu :open?])

(def local-storage-language-name
  "hd-language")

(def known-languages
  {:en (static/static-url "/img/flag-united-kingdom.svg")
   :de (static/static-url "/img/flag-germany.svg")})

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
    (let [loaded-language (get-item local-storage local-storage-language-name ":en")
          loaded-language (cond-> loaded-language
                            (s/starts-with? loaded-language ":") (-> (subs 1) keyword))]
      (set-language db loaded-language))))

(defn language-flag [language-code & {:keys [on-click]}]
  [:img {:src (get known-languages language-code)
         :on-click on-click
         :style {:width "2em"
                 :filter "drop-shadow(0 0 5px #888)";
                 :vertical-align "middle"}}])

(defn selector []
  [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
   {:style {:min-width "3em"}
    :on-mouse-leave #(rf/dispatch [:heraldry.frontend.header/clear-menu-open?
                                   language-menu-open?-path])}
   [:<>
    [:a.nav-menu-link {:href "#"
                       :on-click #(state/dispatch-on-event
                                   % [:heraldry.frontend.header/toggle-menu-open?
                                      language-menu-open?-path])}
     [language-flag @(rf/subscribe [::selected-language])]
     " "]
    [:ul.nav-menu.nav-menu-children
     {:style {:display (if @(rf/subscribe [:heraldry.frontend.header/menu-open?
                                           language-menu-open?-path])
                         "block"
                         "none")}}
     (doall
      (for [language-code (keys known-languages)]
        [:li.nav-menu-item
         [:a.nav-menu-link
          [language-flag language-code
           :on-click #(rf/dispatch [::set-language language-code])]]]))]]])

(defn tr [data]
  (when data
    (util/tr-raw data @(rf/subscribe [::selected-language]))))
