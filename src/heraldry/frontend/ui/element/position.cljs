(ns heraldry.frontend.ui.element.position
  (:require
   [heraldry.coat-of-arms.position :as position]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

;; TODO: probably can be improved with better subscriptions
(defn submenu-link-name [options position]
  (let [changes [(-> position
                     :point
                     position/anchor-point-map)
                 (when (some #(options/changed? % position options)
                             [:offset-x :offset-y :angle])
                   "adjusted")
                 (when (options/changed? :alignment position options)
                   "aligned")]]
    (-> (util/combine ", " changes)
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
         (ui-interface/form-elements
          context
          [:point
           :alignment
           :angle
           :offset-x
           :offset-y
           :type])]]])))

(defmethod ui-interface/form-element :position [context]
  [position-submenu context])
