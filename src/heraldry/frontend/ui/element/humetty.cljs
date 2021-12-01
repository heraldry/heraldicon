(ns heraldry.frontend.ui.element.humetty
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

;; TODO: probably can be improved with better subscriptions
(rf/reg-sub :humetty-submenu-link-name
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/options context])
     (rf/subscribe [:heraldry.state/sanitized-data context])])

  (fn [[options humetty] [_ _path]]
    (let [changes (concat
                   (when (:humetty? humetty)
                     [(string "Humetty / Couped")])
                   (when (and (:humetty? humetty)
                              (some #(options/changed? % humetty options)
                                    [:distance]))
                     [(string "resized")]))]
      (if (seq changes)
        (-> (util/combine ", " changes)
            util/upper-case-first)
        (string "No")))))

(defn humetty-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          tooltip (:tooltip ui)
          link-name @(rf/subscribe [:humetty-submenu-link-name context])]
      [:div.ui-setting
       (when label
         [:label [tr label]
          (when tooltip
            [:div.tooltip.info {:style {:display "inline-block"
                                        :margin-left "0.2em"}}
             [:i.fas.fa-question-circle]
             [:div.bottom
              [:h3 {:style {:text-align "center"}} [tr tooltip]]
              [:i]]])])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-humetty"}
         (ui-interface/form-elements
          context
          [:humetty?
           :corner
           :distance])]]])))

(defmethod ui-interface/form-element :humetty [context]
  [humetty-submenu context])
