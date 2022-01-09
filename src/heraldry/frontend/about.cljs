(ns heraldry.frontend.about
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.static :as static]
   [re-frame.core :as rf]))

(defn view []
  (rf/dispatch [:set-title :string.menu/about])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:h2 [tr :string.text.about/welcome-to-heraldicon]]
   [:h3 [tr :string.text.about/what-is-this-about?]]
   [:img {:style {:width "30%"
                  :float "right"
                  :margin-left "20px"
                  :margin-bottom "20px"}
          :src (static/static-url "/img/heraldicon-logo.png")
          :alt "logo"}]
   [:p
    [:a {:href "https://en.wikipedia.org/wiki/Heraldry"
         :target "_blank"} [tr :string.entity/heraldry]]
    " "
    [tr :string.text.about/heraldry-intro]]
   [:p
    [tr :string.text.about/arms-creation-1-effort]]
   [:p
    [tr :string.text.about/arms-creation-2-with-heraldicon]]
   [:p
    [tr :string.text.about/arms-creation-3-heraldicon-no-replacement-for-art]]
   [:h3 [tr :string.text.about/goals]]
   [:ul
    [:li [tr :string.text.about/goal-1-open-charge-library]]
    [:li [tr :string.text.about/goal-2-open-arms-library]]
    [:li [tr :string.text.about/goal-3-support-heraldic-concepts]]]
   [:h3 [tr :string.text.about/status]]
   [:p
    [tr :string.text.about/status-site-still-beta]]
   [:h3 [tr :string.text.about/licensing]]
   [:p
    [tr :string.text.about/licensing-1-and-attribution-for-public-objects]]
   [:p
    [tr :string.text.about/licensing-2-how-to-specify-and-responsibility]]
   [:p
    [:em
     [tr :string.text.about/licensing-3-contact-if-you-see-your-work-unattributed]]]
   [:p
    [tr :string.text.about/licensing-4-technical-info-on-attribution]]])
