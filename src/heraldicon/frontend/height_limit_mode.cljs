(ns heraldicon.frontend.height-limit-mode
  (:require
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn class []
  (when @(rf/subscribe [::session/height-limit-mode?])
    "height-limited"))

(defn selector []
  (let [height-limit-mode? @(rf/subscribe [::session/height-limit-mode?])]
    [:a.fas {:class (if height-limit-mode?
                      "fa-arrows-alt"
                      "fa-expand")
             :title (tr (if height-limit-mode?
                          :string.tooltip/expand
                          :string.tooltip/fit-in-window))
             :href "#"
             :on-click (js-event/handled
                        #(rf/dispatch [::session/toggle-height-limit-mode]))}]))
