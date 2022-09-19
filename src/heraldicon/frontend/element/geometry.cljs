(ns heraldicon.frontend.element.geometry
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.ordinary-group-options :as ordinary-group-options]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
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

(defmethod element/element :ui.element/geometry [context]
  (when-let [options (interface/get-options context)]
    (let [{:ui/keys [label]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))
          field-context (-> context c/-- interface/parent)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-geometry"}
         (element/elements
          context
          [:width
           :height
           :thickness
           :size-mode
           :size
           :eccentricity
           :stretch
           :mirrored?
           :reversed?])

         [ordinary-group-options/elements field-context]]]])))
