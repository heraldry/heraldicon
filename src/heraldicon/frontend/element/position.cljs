(ns heraldicon.frontend.element.position
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.ordinary-group-options :as ordinary-group-options]
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
                   :string.submenu-summary/adjusted)
                 (when (options/changed? :alignment position options)
                   :string.submenu-summary/aligned)]]
    (string/upper-case-first (string/combine ", " changes))))

(defmethod element/element :ui.element/position [context]
  (when-let [options (interface/get-options context)]
    (let [{:ui/keys [label]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))
          ordinary-context (c/-- context)
          ordinary-group-position? (some #{:fess-group
                                           :pale-group
                                           :chevron-group} (:path ordinary-context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "24em"}
                                                       :class "submenu-position"}
         (element/elements
          context
          [:point
           :alignment
           :angle
           :spacing-top
           :spacing-left
           :offset-x
           :offset-y
           :type])

         (when-not ordinary-group-position?
           [ordinary-group-options/elements ordinary-context])]]])))
