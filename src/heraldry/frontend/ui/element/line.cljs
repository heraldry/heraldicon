(ns heraldry.frontend.ui.element.line
  (:require
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
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
                     strings/adjusted)
                   (when (some #(options/changed? % sanitized-line options)
                               [:width :height])
                     strings/resized)
                   (when (:mirrored? sanitized-line)
                     strings/mirrored-lc)
                   (when (:flipped? sanitized-line)
                     strings/flipped-lc)
                   (when (and (-> sanitized-line :fimbriation :mode)
                              (-> sanitized-line :fimbriation :mode (not= :none)))
                     strings/fimbriated)]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn line-submenu [{:keys [path] :as context}]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:line-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label link-name {:style {:width "24em"}
                                                  :class "submenu-line"}
         (ui-interface/form-elements
          context
          [:type
           :eccentricity
           :height
           :width
           :spacing
           :offset
           :base-line
           :mirrored?
           :flipped?
           :fimbriation])]]])))

(defmethod ui-interface/form-element :line [context]
  [line-submenu context])
