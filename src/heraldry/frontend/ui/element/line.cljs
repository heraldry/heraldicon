(ns heraldry.frontend.ui.element.line
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.frontend.language :refer [tr]]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.options :as options]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :line-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[line options] [_ _path]]
    (let [sanitized-line (options/sanitize line options)
          changes [(-> sanitized-line :type line/line-map)
                   (when (some #(options/changed? % sanitized-line options)
                               [:eccentricity :spacing :offset :base-line])
                     "adjusted")
                   (when (some #(options/changed? % sanitized-line options)
                               [:width :height])
                     "resized")
                   (when (:mirrored? sanitized-line)
                     "mirrored")
                   (when (:flipped? sanitized-line)
                     "flipped")
                   (when (and (-> sanitized-line :fimbriation :mode)
                              (-> sanitized-line :fimbriation :mode (not= :none)))
                     "fimbriated")]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn line-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:line-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu path label link-name {:style {:width "24em"}
                                               :class "submenu-line"}
         (for [option [:type
                       :eccentricity
                       :height
                       :width
                       :spacing
                       :offset
                       :base-line
                       :mirrored?
                       :flipped?
                       :fimbriation]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :line [path]
  [line-submenu path])
