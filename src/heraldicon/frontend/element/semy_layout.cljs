(ns heraldicon.frontend.element.semy-layout
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

;; TODO: probably can be improved with better subscriptions
(defn- submenu-link-name [options layout]
  (let [main-name (when (or (:num-fields-x options)
                            (:num-fields-y options))
                    (string/str-tr (string/combine "x"
                                                   [(:num-fields-x layout)
                                                    (:num-fields-y layout)])
                                   " "
                                   :string.miscellaneous/fields))
        changes (filter identity
                        [main-name
                         (when (options/changed? :num-base-fields layout options)
                           (string/str-tr (:num-base-fields layout) " " :string.submenu-summary/base-fields))
                         (when (some #(options/changed? % layout options)
                                     [:offset-x :offset-y])
                           :string.submenu-summary/shifted)
                         (when (some #(options/changed? % layout options)
                                     [:stretch-x :stretch-y])
                           :string.submenu-summary/stretched)
                         (when (options/changed? :rotation layout options)
                           :string.submenu-summary/rotated)])
        changes (if (seq changes)
                  changes
                  [:string.submenu-summary/default])]
    (string/upper-case-first (string/combine ", " changes))))

(defmethod element/element :ui.element/semy-layout [context]
  (when-let [options (interface/get-options context)]
    (let [{:ui/keys [label]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-semy-layout"}
         (element/elements
          context
          [:num-fields-x
           :num-fields-y
           :offset-x
           :offset-y
           :stretch-x
           :stretch-y
           :rotation])]]])))
