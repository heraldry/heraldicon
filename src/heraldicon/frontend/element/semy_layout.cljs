(ns heraldicon.frontend.element.semy-layout
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

;; TODO: probably can be improved with better subscriptions
(defn- submenu-link-name [options layout raw-layout]
  (let [x-option (:num-fields-x options)
        y-option (:num-fields-y options)
        x-auto? (and (nil? (get raw-layout :num-fields-x))
                     (:default-display-value x-option))
        y-auto? (and (nil? (get raw-layout :num-fields-y))
                     (:default-display-value y-option))
        main-name (when (or x-option y-option)
                    (string/str-tr (string/combine "x"
                                                   [(if x-auto? "[auto]" (:num-fields-x layout))
                                                    (if y-auto? "[auto]" (:num-fields-y layout))])
                                   " "
                                   :string.miscellaneous/charges))
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
          link-name (submenu-link-name options
                                       (interface/get-sanitized-data context)
                                       (interface/get-raw-data context))]
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
