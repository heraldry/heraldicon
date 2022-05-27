(ns heraldicon.frontend.ui.element.geometry
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

;; TODO: probably can be improved with better subscriptions
(defn- submenu-link-name [options geometry]
  (let [changes (concat
                 (when (some #(options/changed? % geometry options)
                             [:size :width :height :thickness])
                   [:string.submenu-summary/resized])
                 (when (some #(options/changed? % geometry options)
                             [:eccentricity])
                   [:string.submenu-summary/adjusted])
                 (when (some #(options/changed? % geometry options)
                             [:stretch])
                   [:string.submenu-summary/stretched])
                 (when (:mirrored? geometry)
                   [:string.submenu-summary/mirrored])
                 (when (:reversed? geometry)
                   [:string.submenu-summary/reversed]))]
    (if (seq changes)
      (string/upper-case-first (string/combine ", " changes))
      "Default")))

(defmethod ui.interface/form-element :geometry [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name (submenu-link-name options (interface/get-sanitized-data context))]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-geometry"}
         (ui.interface/form-elements
          context
          [:width
           :height
           :thickness
           :size-mode
           :size
           :eccentricity
           :stretch
           :mirrored?
           :reversed?])]]])))
