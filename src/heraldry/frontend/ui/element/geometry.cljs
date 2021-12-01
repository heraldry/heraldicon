(ns heraldry.frontend.ui.element.geometry
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

;; TODO: probably can be improved with better subscriptions
(rf/reg-sub :geometry-submenu-link-name
  (fn [[_ context] _]
    [(rf/subscribe [:heraldry.state/options context])
     (rf/subscribe [:heraldry.state/sanitized-data context])])

  (fn [[options geometry] [_ _path]]
    (let [changes (concat
                   (when (some #(options/changed? % geometry options)
                               [:size :width :height :thickness])
                     [strings/resized])
                   (when (some #(options/changed? % geometry options)
                               [:eccentricity])
                     [strings/adjusted])
                   (when (some #(options/changed? % geometry options)
                               [:stretch])
                     [strings/stretched])
                   (when (:mirrored? geometry)
                     [strings/mirrored-lc])
                   (when (:reversed? geometry)
                     [(string "reversed")]))]
      (if (seq changes)
        (-> (util/combine ", " changes)
            util/upper-case-first)
        "Default"))))

(defn geometry-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:geometry-submenu-link-name context])]
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
