(ns heraldicon.frontend.ui.element.position
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.translation.string :as string]
   [heraldicon.util :as util]))

;; TODO: probably can be improved with better subscriptions
(defn submenu-link-name [options position]
  (let [changes [(-> position
                     :point
                     position/orientation-point-map)
                 (when (some #(options/changed? % position options)
                             [:offset-x :offset-y :angle])
                   "adjusted")
                 (when (options/changed? :alignment position options)
                   "aligned")]]
    (-> (string/combine ", " changes)
        util/upper-case-first)))

(defn position-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-position"}
         (ui.interface/form-elements
          context
          [:point
           :alignment
           :angle
           :offset-x
           :offset-y
           :type])]]])))

(defmethod ui.interface/form-element :position [context]
  [position-submenu context])
