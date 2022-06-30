(ns heraldicon.frontend.layout
  (:require
   [heraldicon.frontend.element.submenu :as submenu]
   [re-frame.core :as rf]))

(defn three-columns [left middle right]
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] minmax(26em, 33%) [second] minmax(10em, 25%) [end]"
                 :grid-template-rows "[top] 100% [bottom]"
                 :grid-template-areas "'left middle right'"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(rf/dispatch [::submenu/close-all])}
   [:div.no-scrollbar {:style {:grid-area "left"
                               :position "relative"}}
    left]
   [:div.no-scrollbar {:style {:grid-area "middle"
                               :padding-top "10px"
                               :position "relative"}}
    middle]
   [:div.no-scrollbar {:style {:grid-area "right"
                               :padding-top "5px"
                               :position "relative"}}
    right]])
