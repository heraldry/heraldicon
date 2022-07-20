(ns heraldicon.frontend.dark-mode
  (:require
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn class []
  (when @(rf/subscribe [::session/dark-mode?])
    "dark"))

(defn selector []
  [:li
   [:a.fas.fa-adjust {:href "#"
                      :on-click (js-event/handled
                                 #(rf/dispatch [::session/toggle-dark-mode]))}]])
