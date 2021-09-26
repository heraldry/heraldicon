(ns heraldry.frontend.ui.element.geometry
  (:require [heraldry.frontend.language :refer [tr]]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.options :as options]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :geometry-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[geometry options] [_ _path]]
    (let [sanitized-geometry (options/sanitize geometry options)
          changes (concat
                   (when (some #(options/changed? % sanitized-geometry options)
                               [:size :width :height :thickness])
                     ["resized"])
                   (when (some #(options/changed? % sanitized-geometry options)
                               [:eccentricity :stretch])
                     ["adjusted"])
                   (when (:mirrored? sanitized-geometry)
                     ["mirrored"])
                   (when (:reversed? sanitized-geometry)
                     ["reversed"]))]
      (if (seq changes)
        (-> (util/combine ", " changes)
            util/upper-case-first)
        "Default"))))

(defn geometry-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:geometry-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu path label link-name {:style {:width "19em"}
                                               :class "submenu-geometry"}
         (for [option [:width
                       :height
                       :thickness
                       :size-mode
                       :size
                       :eccentricity
                       :stretch
                       :mirrored?
                       :reversed?]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :geometry [path]
  [geometry-submenu path])
