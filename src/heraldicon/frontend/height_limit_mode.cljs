(ns heraldicon.frontend.height-limit-mode
  (:require
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn class []
  (when @(rf/subscribe [::session/height-limit-mode?])
    "height-limited"))

(defn selector []
  [:a.fas {:class (if @(rf/subscribe [::session/height-limit-mode?])
                    "fa-arrows-alt"
                    "fa-expand")
           :href "#"
           :on-click (js-event/handled
                      #(rf/dispatch [::session/toggle-height-limit-mode]))}])
