(ns heraldicon.frontend.element.position
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

;; TODO: probably can be improved with better subscriptions
(defn- submenu-link-name [options position]
  (let [changes [(-> position
                     :point
                     position/orientation-point-map)
                 (when (some #(options/changed? % position options)
                             [:offset-x :offset-y :angle])
                   "adjusted")
                 (when (options/changed? :alignment position options)
                   "aligned")]]
    (string/upper-case-first (string/combine ", " changes))))

(defmethod element/element :ui.element/position [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:ui/keys [label]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-position"}
         (element/elements
          context
          [:point
           :alignment
           :angle
           :offset-x
           :offset-y
           :type])]]])))
