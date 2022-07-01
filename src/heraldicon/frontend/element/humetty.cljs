(ns heraldicon.frontend.element.humetty
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

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
      (string/upper-case-first (string/combine ", " changes))
      :string.submenu-summary/no)))

(defmethod element/element :ui.element/humetty [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:ui/keys [label tooltip]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]
          [tooltip/info tooltip]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-humetty"}
         (element/elements
          context
          [:humetty?
           :corner
           :distance])]]])))
