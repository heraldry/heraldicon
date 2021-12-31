(ns heraldry.frontend.ui.element.humetty
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

;; TODO: probably can be improved with better subscriptions
(defn submenu-link-name [options humetty]
  (let [changes (concat
                 (when (:humetty? humetty)
                   [:string.option/humetty])
                 (when (and (:humetty? humetty)
                            (some #(options/changed? % humetty options)
                                  [:distance]))
                   [:string.submenu-summary/resized]))]
    (if (seq changes)
      (-> (util/combine ", " changes)
          util/upper-case-first)
      :string.submenu-summary/no)))

(defn humetty-submenu [context]
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
                                                       :class "submenu-humetty"}
         (ui-interface/form-elements
          context
          [:humetty?
           :corner
           :distance])]]])))

(defmethod ui-interface/form-element :humetty [context]
  [humetty-submenu context])
