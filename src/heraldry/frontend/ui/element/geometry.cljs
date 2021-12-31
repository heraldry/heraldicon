(ns heraldry.frontend.ui.element.geometry
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]))

;; TODO: probably can be improved with better subscriptions
(defn submenu-link-name [options geometry]
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
      (-> (util/combine ", " changes)
          util/upper-case-first)
      "Default")))

(defn geometry-submenu [context]
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
         (ui-interface/form-elements
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

(defmethod ui-interface/form-element :geometry [context]
  [geometry-submenu context])
