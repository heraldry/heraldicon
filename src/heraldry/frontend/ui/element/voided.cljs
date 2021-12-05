(ns heraldry.frontend.ui.element.voided
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

;; TODO: probably can be improved with better subscriptions
(defn submenu-link-name [options voided]
  (let [changes (concat
                 (when (:voided? voided)
                   [(string "Voided")])
                 (when (and (:voided? voided)
                            (some #(options/changed? % voided options)
                                  [:thickness]))
                   [(string "resized")]))]
    (if (seq changes)
      (-> (util/combine ", " changes)
          util/upper-case-first)
      (string "No"))))

(defn voided-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          tooltip (:tooltip ui)
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
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
                                                       :class "submenu-voided"}
         (ui-interface/form-elements
          context
          [:voided?
           :corner
           :thickness])]]])))

(defmethod ui-interface/form-element :voided [context]
  [voided-submenu context])
