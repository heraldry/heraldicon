(ns heraldry.frontend.ui.element.geometry
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
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
                     [strings/resized])
                   (when (some #(options/changed? % sanitized-geometry options)
                               [:eccentricity])
                     [strings/adjusted])
                   (when (some #(options/changed? % sanitized-geometry options)
                               [:stretch])
                     [strings/stretched])
                   (when (:mirrored? sanitized-geometry)
                     [strings/mirrored-lc])
                   (when (:reversed? sanitized-geometry)
                     [{:en "reversed"
                       :de "umgedreht"}]))]
      (if (seq changes)
        (-> (util/combine ", " changes)
            util/upper-case-first)
        "Default"))))

(defn geometry-submenu [{:keys [path] :as context}]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:geometry-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu path label link-name {:style {:width "22em"}
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
           ^{:key option} [interface/form-element (update context :path conj option)])]]])))

(defmethod interface/form-element :geometry [context]
  [geometry-submenu context])
