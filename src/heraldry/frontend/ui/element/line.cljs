(ns heraldry.frontend.ui.element.line
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :line-title
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
                   (when (-> sanitized-line :fimbriation :mode (not= :none))
                     "fimbriated")]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn line-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          title @(rf/subscribe [:line-title path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label title {:width "30em"}
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
